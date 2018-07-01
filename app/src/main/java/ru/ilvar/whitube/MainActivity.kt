package ru.ilvar.whitube

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.StrictMode
import android.view.View
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
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout


class MainActivity : Activity() {
    var jsonList: JSONArray = JSONArray()
    var ytPlayer: YouTubePlayer? = null
    var vidIdx = -1
    var vidCount = -1
    var splash: FrameLayout? = null
    val videoIds = mutableListOf<String>()
    var isPlaying: Boolean? = null

    fun nextVideo() {
        vidIdx += 1
        if (vidIdx >= vidCount) {
            vidIdx = 0
        }
        setVideo(vidIdx)
    }

    fun prevVideo() {
        vidIdx -= 1
        if (vidIdx < 0) {
            vidIdx = vidCount - 1
        }
        setVideo(vidIdx)
    }

    fun setVideo(i: Int) {
        try {
            val videoId = videoIds[i]
            vidIdx = i
            ytPlayer?.loadVideo(videoId, 0f)

            val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay);
            clickOverlay.visibility = View.INVISIBLE
        } catch(e: java.lang.IndexOutOfBoundsException) {}
    }

    fun setVideo(videoId: String) {
        for (i in (0..videoIds.size - 1)) {
            if (videoId == videoIds[i]) {
                setVideo(i)
            }
        }
    }

    fun stopPlaying() {
        if (isPlaying == true) {
            isPlaying = false
            val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.saved_playing_state), false)
                apply()
            }
        }
    }

    fun getPlayingState(): Boolean? {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return null
        val key = getString(R.string.saved_playing_state)
        if (sharedPref.contains(key)) {
            return sharedPref.getBoolean(key, false)
        } else {
            return null
        }
    }

    override fun onPause() {
        super.onPause()

        ytPlayer?.pause()
        val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay);
        clickOverlay.visibility = View.VISIBLE

        stopPlaying()
    }

    override fun onStop() {
        super.onStop()

        ytPlayer?.pause()
        val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay);
        clickOverlay.visibility = View.VISIBLE

        stopPlaying()
    }

    override fun onResume() {
        super.onResume()

        ytPlayer?.pause()
        val clickOverlay = findViewById<FrameLayout>(R.id.galleryOverlay);
        clickOverlay.visibility = View.VISIBLE

        stopPlaying()
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

    private fun loadVideos(start: Boolean) {
        val currentLocale = resources.configuration.locale
        val lang = currentLocale.isO3Language

        Log.e("lang", "Lang: $lang")

        val sharedPref = this.application.getSharedPreferences("ALP", Context.MODE_PRIVATE) ?: return
        val defaultValue: MutableSet<String> = hashSetOf()
        val selectedLists = sharedPref.getStringSet(getString(R.string.selected_channels), defaultValue)

        val listsKey = if (selectedLists.size > 0) { "?lists=" + selectedLists.joinToString(separator=",") } else { "" }
        val videosUrl = "https://pacific-cove-93657.herokuapp.com/videos/$listsKey"

        Log.e("URL", videosUrl)

        val json = URL(videosUrl).readText()

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

            videoIds.shuffle(Random(System.currentTimeMillis()))
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

        val mainActivity = this
        val settingsButton = findViewById<ImageButton>( R.id.settingsButton );
        settingsButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val intent = Intent(mainActivity, ChannelsActivity::class.java).apply {}
                startActivity(intent)
            }
        });


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

        splash = findViewById(R.id.splash)

        youTubePlayerView.initialize({ initializedYouTubePlayer ->
            ytPlayer = initializedYouTubePlayer
            initializedYouTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                override fun onReady() {
                    if (splash?.visibility != View.INVISIBLE) {
                        splash?.visibility = View.INVISIBLE
                    }
                    isPlaying = getPlayingState()
                    Log.d("Whitube", "isPlaying is " + isPlaying.toString())
                    if (isPlaying == null) {
                        nextVideo()
                        isPlaying = true
                    }
                }

                override fun onStateChange(state: Int) {
                    if (state == PlayerConstants.PlayerState.ENDED && isPlaying == true) {
                        nextVideo()
                    }
                    if (state == PlayerConstants.PlayerState.PLAYING) {
                        if (splash?.visibility != View.INVISIBLE) {
                            splash?.visibility = View.INVISIBLE
                        }
                    }
                    super.onStateChange(state)
                }
            })
        }, false)

        youTubePlayerView.enterFullScreen()

        val decorView = getWindow().getDecorView()
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.setSystemUiVisibility(uiOptions)

        val uiController = youTubePlayerView.playerUIController
        uiController.showVideoTitle(false)
        uiController.showMenuButton(false)
        uiController.showFullscreenButton(false)
        uiController.showUI(false)

        val galleryOverlay = findViewById<FrameLayout>(R.id.galleryOverlay);
        val videoOverlay = findViewById<FrameLayout>(R.id.videoOverlay);
        videoOverlay.setOnClickListener {
            galleryOverlay.visibility = View.VISIBLE
            ytPlayer?.pause()
            isPlaying = false
        }

        val playButton = findViewById<ImageButton>(R.id.playButton);
        playButton.setOnClickListener {
            galleryOverlay.visibility = View.INVISIBLE
            ytPlayer?.play()
            isPlaying = true
        }

        val nextButton = findViewById<ImageButton>(R.id.nextButton);
        nextButton.setOnClickListener {
            galleryOverlay.visibility = View.INVISIBLE
            this.nextVideo()
            ytPlayer?.play()
            isPlaying = true
        }

        val prevButton = findViewById<ImageButton>(R.id.prevButton);
        prevButton.setOnClickListener {
            galleryOverlay.visibility = View.INVISIBLE
            this.prevVideo()
            ytPlayer?.play()
            isPlaying = true
        }

    }
}
