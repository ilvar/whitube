package ru.ilvar.whitube

import android.app.Activity
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.pierfrancescosoffritti.youtubeplayer.player.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayerView
import org.json.JSONArray
import org.json.JSONException
import java.net.URL
import java.util.*
import java.io.*
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.util.Log
import android.widget.LinearLayout


class MainActivity : Activity() {
    var jsonList: JSONArray = JSONArray()
    var ytPlayer: YouTubePlayer? = null
    var vidIdx = -1
    var vidCount = -1
    var splash: FrameLayout? = null
    val videoIds = mutableListOf<String>()

    fun nextVideo() {
        vidIdx += 1
        if (vidIdx >= vidCount) {
            vidIdx = 0
        }
        setVideo(vidIdx)
    }

    fun setVideo(i: Int) {
        val videoId = videoIds[i]
        vidIdx = i
        ytPlayer?.loadVideo(videoId, 0f)
    }

    fun setVideo(videoId: String) {
        for (i in (0..videoIds.size - 1)) {
            if (videoId == videoIds[i]) {
                setVideo(i)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        ytPlayer?.pause()
        val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay);
        clickOverlay.visibility = View.VISIBLE
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

    fun prepareData(): List<Video> {
        return List<Video>(videoIds.size, init = {i -> Video(videoIds[i])})
    }

    fun loadCached(start: Boolean) {
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
                loadVideos(start)
                System.out.println("Error, loaded from internet")
            }
        } else {
            loadVideos(start)
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

        val recyclerView = findViewById<View>(R.id.gallery) as RecyclerView
        recyclerView.setHasFixedSize(true)

        loadCached(false)

        Log.d("Whitube", "Loaded" + videoIds.size.toString() + " videos")

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val thumbHeight = metrics.heightPixels / 3

        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.orientation = LinearLayout.HORIZONTAL
        recyclerView.layoutManager = layoutManager

        val adapter = VideosAdapter(cacheDir, thumbHeight, prepareData(), this)
        recyclerView.adapter = adapter

        val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)

        splash = findViewById<FrameLayout>(R.id.splash)

        youTubePlayerView.initialize({ initializedYouTubePlayer ->
            ytPlayer = initializedYouTubePlayer
            initializedYouTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                override fun onReady() {
                    nextVideo()
                }

                override fun onStateChange(state: Int) {
                    if (state == PlayerConstants.PlayerState.ENDED) {
                        nextVideo()
                    }
                    if (state == PlayerConstants.PlayerState.PLAYING) {
                        splash?.visibility = View.INVISIBLE
                        splash?.removeAllViews()
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
