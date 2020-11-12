package com.yatin.newsapp

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.yatin.newsapp.api.ApiClient.apiClient
import com.yatin.newsapp.api.ApiInterface
import com.yatin.newsapp.models.Article
import com.yatin.newsapp.models.News
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainActivity : AppCompatActivity(), OnRefreshListener {
    private var recyclerView: RecyclerView? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var articles: List<Article>? = ArrayList()
    private var adapter: Adapter? = null
    private var topHeadline: TextView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var errorLayout: RelativeLayout? = null
    private var errorImage: ImageView? = null
    private var errorTitle: TextView? = null
    private var errorMessage: TextView? = null
    private var btnRetry: Button? = null
    private val modePref = "ModePref"
    private var sharedPreferences: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout!!.setOnRefreshListener(this)
        swipeRefreshLayout!!.setColorSchemeResources(R.color.colorAccent)
        topHeadline = findViewById(R.id.topheadelines)
        recyclerView = findViewById(R.id.recyclerView)
        layoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        recyclerView!!.isNestedScrollingEnabled = false
        sharedPreferences = getSharedPreferences(modePref, MODE_PRIVATE)
        val mode = sharedPreferences!!.getString("Mode", "LightMode")
        if (mode == "LightMode") {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Toast.makeText(this@MainActivity, "LightMode Enabled!!", Toast.LENGTH_LONG).show()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Toast.makeText(this@MainActivity, "DarkMode Enabled!!", Toast.LENGTH_LONG).show()
        }
        onLoadingSwipeRefresh("")
        errorLayout = findViewById(R.id.errorLayout)
        errorImage = findViewById(R.id.errorImage)
        errorTitle = findViewById(R.id.errorTitle)
        errorMessage = findViewById(R.id.errorMessage)
        btnRetry = findViewById(R.id.btnRetry)
    }

    private fun loadJson(keyword: String) {
        errorLayout!!.visibility = View.GONE
        swipeRefreshLayout!!.isRefreshing = true
        val apiInterface = apiClient!!.create(ApiInterface::class.java)
        val country = Utils.country
        val language = Utils.language
        val call: Call<News?>?
        call = if (keyword.isNotEmpty()) {
            apiInterface.getNewsSearch(keyword, language, "publishedAt", API_KEY)
        } else {
            apiInterface.getNews(country, API_KEY)
        }
        call!!.enqueue(object : Callback<News?> {
            override fun onResponse(call: Call<News?>, response: Response<News?>) {
                if (response.isSuccessful && response.body()!!.article != null) {
                    if (articles!!.isNotEmpty()) {
                        (articles as ArrayList).clear()
                    }
                    articles = response.body()!!.article
                    adapter = Adapter(articles, this@MainActivity)
                    recyclerView!!.adapter = adapter
                    adapter!!.notifyDataSetChanged()
                    initListener()
                    topHeadline!!.visibility = View.VISIBLE
                    swipeRefreshLayout!!.isRefreshing = false
                } else {
                    topHeadline!!.visibility = View.INVISIBLE
                    swipeRefreshLayout!!.isRefreshing = false
                    val errorCode: String = when (response.code()) {
                        404 -> "404 not found"
                        500 -> "500 server broken"
                        else -> "unknown error"
                    }
                    showErrorMessage(
                            R.drawable.no_result,
                            "No Result",
                            """
                                Please Try Again!
                                $errorCode
                                """.trimIndent())
                }
            }

            override fun onFailure(call: Call<News?>, t: Throwable) {
                topHeadline!!.visibility = View.INVISIBLE
                swipeRefreshLayout!!.isRefreshing = false
                showErrorMessage(
                        R.drawable.oops,
                        "Oops..",
                        """
                            Network failure, Please Try Again
                            $t
                            """.trimIndent())
            }
        })
    }

    private fun initListener() {
        adapter!!.setOnItemClickListener(object : Adapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val imageView = view.findViewById<ImageView>(R.id.img)
                 val intent = Intent(this@MainActivity, NewsDetailActivity::class.java)
                 val article = articles!![position]
                 intent.putExtra("url", article.url)
                 intent.putExtra("title", article.title)
                 intent.putExtra("img", article.urlToImage)
                 intent.putExtra("date", article.publishedAt)
                 intent.putExtra("source", article.source!!.name)
                 intent.putExtra("author", article.author)
                 val pair = Pair.create(imageView as View, ViewCompat.getTransitionName(imageView))
                 val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                         this@MainActivity,
                         pair
                 )
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                     startActivity(intent, optionsCompat.toBundle())
                 } else {
                     startActivity(intent)
                 }
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        val searchMenuItem = menu.findItem(R.id.action_search)
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.queryHint = "Search Latest News..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.length > 2) {
                    onLoadingSwipeRefresh(query)
                } else {
                    Toast.makeText(this@MainActivity, "Type more than two letters!", Toast.LENGTH_SHORT).show()
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        searchMenuItem.icon.setVisible(false, false)
        return true
    }

    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_about) {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("https://yatin.htmlsave.net")
            startActivity(i)
            return true
        }
        if (id == R.id.action_darkmode) {
            Toast.makeText(this@MainActivity, "DarkMode Enabled!!", Toast.LENGTH_LONG).show()
            val editor = sharedPreferences!!.edit()
            editor.putString("Mode", "DarkMode")
            editor.apply()
            editor.commit()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            onLoadingSwipeRefresh("")
            return true
        }
        if (id == R.id.action_lightmode) {
            Toast.makeText(this@MainActivity, "LightMode Enabled!!", Toast.LENGTH_LONG).show()
            val editor = sharedPreferences!!.edit()
            editor.putString("Mode", "LightMode")
            editor.apply()
            editor.commit()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            onLoadingSwipeRefresh("")
            return true
        }
        if (id == R.id.action_layout) {
            val editor = sharedPreferences!!.edit()
            editor.putString("Layout", "Layout1")
            editor.apply()
            editor.commit()
            onLoadingSwipeRefresh("")
            return true
        }
        if (id == R.id.action_newlayout) {
            val editor = sharedPreferences!!.edit()
            editor.putString("Layout", "Layout2")
            editor.apply()
            editor.commit()
            onLoadingSwipeRefresh("")
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() {
        loadJson("")
    }

    private fun onLoadingSwipeRefresh(keyword: String) {
        swipeRefreshLayout!!.post { loadJson(keyword) }
    }

    private fun showErrorMessage(imageView: Int, title: String, message: String) {
        if (errorLayout!!.visibility == View.GONE) {
            errorLayout!!.visibility = View.VISIBLE
        }
        errorImage!!.setImageResource(imageView)
        errorTitle!!.text = title
        errorMessage!!.text = message
        btnRetry!!.setOnClickListener { onLoadingSwipeRefresh("") }
    }

    companion object {
        const val API_KEY = "aee363b4e3d6430cb988bdcbc4918870" //"your secret api key";
    }
}