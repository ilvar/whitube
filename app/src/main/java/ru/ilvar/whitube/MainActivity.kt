package ru.ilvar.whitube

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView.ScaleType
import android.widget.LinearLayout
import com.pierfrancescosoffritti.youtubeplayer.player.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayerView
import org.json.JSONArray
import org.json.JSONException
import java.net.URL
import java.util.*
import android.util.DisplayMetrics
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : Activity() {
    var jsonList: JSONArray = JSONArray()
    var ytPlayer: YouTubePlayer? = null
    var vidIdx = 0
    var vidCount = -1
    var gallery: LinearLayout? = null
    val covers = mutableListOf<ImageButton>()
    val videoIds = mutableListOf<String>()

    fun nextVideo() {
        vidIdx += 1
        if (vidIdx >= vidCount) {
            vidIdx = 0
        }
        val vidObj = jsonList.getJSONObject(vidIdx)

        val videoId = vidObj.getString("yid")
        setVideo(videoId)

        for (i in 0..(vidIdx+25)) {
            if (i < jsonList.length()) {
                createCover(videoIds[i], i)
            }
        }
    }

    fun setVideo(videoId: String) {
        ytPlayer?.loadVideo(videoId, 0f)
    }

    override fun onPause() {
        super.onPause()

        ytPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()

        ytPlayer?.play()
    }

    fun createCover(videoId: String, i: Int): ImageButton {
        try {
            return covers[i]
        } catch (e: java.lang.IndexOutOfBoundsException) {
            System.out.print("Need to load the cover " + videoId)
        }

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val imageView = ImageButton(this)
        imageView.setId(i)
        imageView.setPadding(2, 2, 2, 2)
        imageView.setScaleType(ScaleType.FIT_CENTER)
        imageView.setAdjustViewBounds(true)
        imageView.maxHeight = metrics.heightPixels / 3

        val file = File(cacheDir, "video_" + videoId)

        if (file.exists() and file.isFile) {
            try {
                val inputStream = FileInputStream(file)
                val thumb_d = Drawable.createFromStream(inputStream, "src")
                imageView.setImageDrawable(thumb_d)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                val thumb_u = URL("https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg")

                val content = thumb_u.readBytes()
                try {
                    val outputStream = FileOutputStream(file)
                    outputStream.write(content)
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val thumb_d = Drawable.createFromStream(content.inputStream(), "src")
                imageView.setImageDrawable(thumb_d)
            } catch (e: Exception) {
                val bmp = BitmapFactory.decodeResource(getResources(), R.drawable.abc_btn_colored_material)
                imageView.setImageBitmap(bmp);

                e.printStackTrace()
            }
        }

        imageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay)
                clickOverlay.visibility = View.INVISIBLE
                setVideo(videoId)
            }
        })

        covers.add(i, imageView)
        gallery?.addView(imageView)

        return imageView
    }

    fun loadVideos() {
        val json = URL("https://pacific-cove-93657.herokuapp.com/videos/").readText()

        try {
            jsonList = JSONArray(json)

            gallery = findViewById<LinearLayout>(R.id.gallery)
            vidCount = jsonList.length()
            for (i in 0..(vidCount - 1)) {

                val vidObj = jsonList.getJSONObject(i)
                val videoId = vidObj.getString("yid")

                videoIds.add(i, videoId)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        Collections.shuffle(videoIds)
        nextVideo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = this
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_main)

        val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)

        youTubePlayerView.initialize({ initializedYouTubePlayer ->
            ytPlayer = initializedYouTubePlayer
            initializedYouTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                override fun onReady() {
                    loadVideos()
                }

                override fun onStateChange(state: Int) {
                    if (state == PlayerConstants.PlayerState.ENDED) {
                        nextVideo()
                    }
                    super.onStateChange(state)
                }
            })
        }, true)

        youTubePlayerView.enterFullScreen()

        val decorView = getWindow().getDecorView()
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.setSystemUiVisibility(uiOptions)

        val uiController = youTubePlayerView.playerUIController
        uiController.showVideoTitle(false)
        uiController.showMenuButton(false)
        uiController.showFullscreenButton(false)
        uiController.showUI(false)

        val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay);
        val clickButton = findViewById<Button>(R.id.nextButton);
        clickButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (clickOverlay.visibility == View.VISIBLE) {
                    clickOverlay.visibility = View.INVISIBLE
                    ytPlayer?.play()
                } else {
                    clickOverlay.visibility = View.VISIBLE
                    ytPlayer?.pause()
                }

            }
        });
    }
}
