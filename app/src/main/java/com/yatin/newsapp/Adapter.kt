package com.yatin.newsapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.yatin.newsapp.Adapter.MyViewHolder
import com.yatin.newsapp.models.Article

class Adapter(private val articles: List<Article>?, private val context: Context) : RecyclerView.Adapter<MyViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null
    private var sharedPreferences: SharedPreferences? = null
    private val modePref = "ModePref"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        sharedPreferences = context.getSharedPreferences(modePref, Context.MODE_PRIVATE)
        val layout = sharedPreferences!!.getString("Layout", "Layout1")
        return if (layout == "Layout1") {
            val view = LayoutInflater.from(context).inflate(R.layout.item, parent, false)
            MyViewHolder(view, onItemClickListener)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item2, parent, false)
            MyViewHolder(view, onItemClickListener)
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun onBindViewHolder(holders: MyViewHolder, position: Int) {
        val model = articles!![position]
        val requestOptions = RequestOptions()
        requestOptions.placeholder(Utils.randomDrawableColor)
        requestOptions.error(Utils.randomDrawableColor)
        requestOptions.diskCacheStrategy(DiskCacheStrategy.ALL)
        requestOptions.centerCrop()
        if(!model.urlToImage.isNullOrEmpty()) {
            Glide.with(context)
                    .load(model.urlToImage)
                    .apply(requestOptions)
                    .listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                            holders.progressBar.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            holders.progressBar.visibility = View.GONE
                            return false
                        }
                    })
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holders.imageView)
        }
        holders.title.text = model.title
        holders.desc.text = model.description
        holders.source.text = model.source!!.name
        holders.time.text = " \u2022 " + Utils.dateToTimeFormat(model.publishedAt)
        holders.publishedAt.text = Utils.dateFormat(model.publishedAt)
        holders.author.text = model.author
    }

    override fun getItemCount(): Int {
        return articles!!.size
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    inner class MyViewHolder(itemView: View, onItemClickListener: OnItemClickListener?) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var title: TextView
        var desc: TextView
        var author: TextView
        var publishedAt: TextView
        var source: TextView
        var time: TextView
        var imageView: ImageView
        var progressBar: ProgressBar
        private var onItemClickListener: OnItemClickListener?
        override fun onClick(v: View) {
            onItemClickListener!!.onItemClick(v, adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
            title = itemView.findViewById(R.id.title)
            desc = itemView.findViewById(R.id.desc)
            author = itemView.findViewById(R.id.author)
            publishedAt = itemView.findViewById(R.id.publishedAt)
            source = itemView.findViewById(R.id.source)
            time = itemView.findViewById(R.id.time)
            imageView = itemView.findViewById(R.id.img)
            progressBar = itemView.findViewById(R.id.prograss_load_photo)
            this.onItemClickListener = onItemClickListener
        }
    }
}