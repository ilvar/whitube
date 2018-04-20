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
import java.io.*


class MainActivity : Activity() {
    var jsonList: JSONArray = JSONArray()
    var ytPlayer: YouTubePlayer? = null
    var vidIdx = -1
    var vidCount = -1
    var gallery: LinearLayout? = null
    var splash: FrameLayout? = null
    val covers = mutableListOf<ImageButton>()
    val videoIds = mutableListOf<String>()

    fun nextVideo() {
        vidIdx += 1
        if (vidIdx >= vidCount) {
            vidIdx = 0
        }
        setVideo(vidIdx)

        Thread({
            for (i in 0..videoIds.size-1) {
                loadCover(videoIds[i])
            }
        }).start()
    }

    fun setVideo(i: Int) {
        val videoId = videoIds[i]
        vidIdx = i
        ytPlayer?.loadVideo(videoId, 0f)
        Thread.sleep(100)

        for (j in vidIdx..(vidIdx+5)) {
            if (j < videoIds.size) {
                createCover(videoIds[j], j)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        ytPlayer?.pause()
    }

    fun loadCover(videoId: String): InputStream {
        val file = File(cacheDir, "video_" + videoId)

        if (file.exists() and file.isFile) {
            return FileInputStream(file)
        } else {
            val thumb_u = URL("https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg")

            val content = thumb_u.readBytes()
            val outputStream = FileOutputStream(file)
            outputStream.write(content)
            outputStream.close()

            return content.inputStream()
        }
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

        val thumb_d = Drawable.createFromStream(loadCover(videoId), "src")
        imageView.setImageDrawable(thumb_d)

        imageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay)
                clickOverlay.visibility = View.INVISIBLE
                setVideo(i)
            }
        })

        covers.add(i, imageView)
        gallery?.addView(imageView)

        System.out.println("Loaded cover for " + videoId)

        return imageView
    }

    fun parseVideosJSON(json: String) {
        try {
            jsonList = JSONArray(json)

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
    }

    fun loadCached() {
        val file = File(cacheDir, "list.json")
        System.out.println("Loading videos list from cache...")

        if (file.exists() and file.isFile) {
            try {
                val inputStream = FileInputStream(file)
                val json = inputStream.reader().readText()

                parseVideosJSON(json)
                System.out.println("Parsed JSON")

                nextVideo()
                System.out.println("Started video")

                Thread({
                    loadVideos(false)
                    System.out.println("Loaded from internet")
                }).start()
            } catch (e: Exception) {
                e.printStackTrace()
                loadVideos(true)
                System.out.println("Error, loaded from internet")
            }
        } else {
            loadVideos(true)
            System.out.println("No data, loaded from internet")
        }
    }

    fun loadVideos(start: Boolean) {
        val json = URL("https://pacific-cove-93657.herokuapp.com/videos/").readText()

        try {
            val file = File(cacheDir, "list.json")
            val outputStream = FileOutputStream(file)
            outputStream.write(json.toByteArray())
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (start) {
            parseVideosJSON(json)

            Collections.shuffle(videoIds, Random(System.currentTimeMillis()))
            nextVideo()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_main)

        val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)

        gallery = findViewById<LinearLayout>(R.id.gallery)
        splash = findViewById<FrameLayout>(R.id.splash)

        youTubePlayerView.initialize({ initializedYouTubePlayer ->
            ytPlayer = initializedYouTubePlayer
            initializedYouTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                override fun onReady() {
                    loadCached()
                }

                override fun onStateChange(state: Int) {
                    if (state == PlayerConstants.PlayerState.ENDED) {
                        nextVideo()
                    }
                    if (state == PlayerConstants.PlayerState.PLAYING) {
                        splash?.visibility = View.INVISIBLE
                        splash = null
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
