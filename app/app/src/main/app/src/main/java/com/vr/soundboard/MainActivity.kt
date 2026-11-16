package com.vr.soundboard

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private var player: MediaPlayer? = null
    private lateinit var layout: LinearLayout
    private lateinit var status: TextView

    private val sounds = mutableListOf<Uri>()

    private val prefs by lazy {
        getSharedPreferences("sounds", MODE_PRIVATE)
    }

    private val PICK_AUDIO = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)

        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 35, 25, 35)
            setBackgroundColor(Color.rgb(18, 18, 30))
        }

        scroll.addView(layout)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "🔊 VR SOUNDBOARD"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        layout.addView(title)

        status = TextView(this).apply {
            text = "Ready to make some noise!"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(10, 20, 10, 20)
        }

        layout.addView(status)

        addButton("➕ ADD SOUNDS", Color.rgb(30, 130, 220)) {
            openAudioPicker()
        }

        addButton("⏹ STOP ALL SOUNDS", Color.rgb(200, 45, 55)) {
            stopSound()
        }

        loadSounds()
    }

    private fun addButton(
        label: String,
        color: Int,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 17f
            setTextColor(Color.WHITE)
            setBackgroundColor(color)
        }

        layout.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75
            ).apply {
                setMargins(0, 12, 0, 12)
            }
        )

        button.setOnClickListener { action() }
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        startActivityForResult(intent, PICK_AUDIO)
    }

    @Deprecated("Deprecated callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_AUDIO &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val uris = mutableListOf<Uri>()

            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    uris.add(clip.getItemAt(i).uri)
                }
            } ?: data.data?.let {
                uris.add(it)
            }

            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                if (!sounds.contains(uri)) {
                    sounds.add(uri)
                }
            }

            saveSounds()
            refreshButtons()
        }
    }

    private fun saveSounds() {
        prefs.edit().putString(
            "list",
            sounds.joinToString("|") { it.toString() }
        ).apply()
    }

    private fun loadSounds() {
        val saved = prefs.getString("list", "") ?: ""

        if (saved.isNotEmpty()) {
            saved.split("|").forEach {
                sounds.add(Uri.parse(it))
            }
        }

        refreshButtons()
    }

    private fun refreshButtons() {
        while (layout.childCount > 4) {
            layout.removeViewAt(4)
        }

        sounds.forEachIndexed { index, uri ->

            val name = uri.lastPathSegment
                ?.substringAfterLast("/")
                ?.takeLast(30)
                ?: "Sound ${index + 1}"

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val play = Button(this).apply {
                text = "▶ $name"
                textSize = 14f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(90, 55, 170))
            }

            val delete = Button(this).apply {
                text = "✖"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(190, 40, 50))
            }

            row.addView(
                play,
                LinearLayout.LayoutParams(0, 75, 1f)
            )

            row.addView(
                delete,
                LinearLayout.LayoutParams(75, 75)
            )

            layout.addView(row)

            play.setOnClickListener {
                playSound(uri)
            }

            delete.setOnClickListener {
                stopSound()
                sounds.remove(uri)
                saveSounds()
                refreshButtons()
            }
        }
    }

    private fun playSound(uri: Uri) {
        stopSound()

        try {
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, uri)

                setOnPreparedListener {
                    it.start()
                    status.text = "🔊 Playing sound!"
                }

                setOnCompletionListener {
                    it.release()
                    player = null
                    status.text = "Ready to play!"
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            status.text = "Unable to play sound."
            stopSound()
        }
    }

    private fun stopSound() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}

        player = null
        status.text = "⏹ Sound stopped."
    }

    override fun onDestroy() {
        stopSound()
        super.onDestroy()
    }
}
