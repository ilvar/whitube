package ru.ilvar.whitube

import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL


class VideosAdapter(
        private val cacheDir: File,
        private val thumbHeight: Int,
        private val videos: List<Video>,
        private val mainActivity: MainActivity) : RecyclerView.Adapter<VideosAdapter.PhotoHolder>() {
    init {
        Thread({
            for(v in videos) {
                val file = File(cacheDir, "video_" + v.videoId)
                if (!file.exists() or !file.isFile) {
                    try {
                        loadCoverFromInternet(v.videoId)
                    } catch (e: java.io.FileNotFoundException) {}
                }
            }
        }).start()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): VideosAdapter.PhotoHolder {
        val view = LayoutInflater.from(parent?.getContext()).inflate(R.layout.gallery_thumb, parent, false)
        return PhotoHolder(view)
    }

    override fun onBindViewHolder(holder: VideosAdapter.PhotoHolder, position: Int) {
        holder.video = videos[position]
        holder.img?.scaleType = ImageView.ScaleType.FIT_CENTER;
        holder.img?.maxHeight = thumbHeight
        holder.img?.minimumHeight = thumbHeight
        try {
            holder.img?.setImageDrawable(createCover(videos[position].videoId));
            holder.mainActivity = mainActivity
        } catch (e: java.io.FileNotFoundException) {}
    }

    override fun getItemCount() = videos.size

    private fun loadCoverFromInternet(videoId: String): InputStream {
        val file = File(cacheDir, "video_" + videoId)
        val thumb_u = URL("https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg")

        val content = thumb_u.readBytes()
        val outputStream = FileOutputStream(file)
        outputStream.write(content)
        outputStream.close()

        Log.d("Whitube", "Loaded thumb from Internet... " + videoId)
        return content.inputStream()
    }

    private fun loadCover(videoId: String): InputStream {
        val file = File(cacheDir, "video_" + videoId)

        if (file.exists() and file.isFile) {
            Log.d("Whitube", "Loading thumb from file... " + videoId)
            return FileInputStream(file)
        } else {
            return loadCoverFromInternet(videoId)
        }
    }

    private fun createCover(videoId: String): Drawable {
        return Drawable.createFromStream(loadCover(videoId), "src")
    }

    class PhotoHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        var video: Video? = null
        var img: ImageView? = null
        var mainActivity: MainActivity? = null

        init {
            img = v.findViewById<ImageView>(R.id.img)

            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            mainActivity?.setVideo(video!!.videoId)
        }
    }
}