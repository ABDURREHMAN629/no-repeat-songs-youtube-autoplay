package com.norepeat.songs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.R as AppCompatR
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.R as MaterialR
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    private lateinit var artistsInput: TextInputEditText
    private lateinit var genresInput: TextInputEditText
    private lateinit var sizeGroup: MaterialButtonToggleGroup
    private lateinit var makeButton: MaterialButton
    private lateinit var progress: LinearProgressIndicator
    private lateinit var statusText: TextView
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultTitle: TextView
    private lateinit var resultInfo: TextView
    private lateinit var memoryCount: TextView
    private lateinit var importButton: MaterialButton
    private lateinit var forgetButton: MaterialButton

    private val pickHistoryFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importHistory(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        artistsInput = findViewById(R.id.artistsInput)
        genresInput = findViewById(R.id.genresInput)
        sizeGroup = findViewById(R.id.sizeGroup)
        makeButton = findViewById(R.id.makeButton)
        progress = findViewById(R.id.progress)
        statusText = findViewById(R.id.statusText)
        resultCard = findViewById(R.id.resultCard)
        resultTitle = findViewById(R.id.resultTitle)
        resultInfo = findViewById(R.id.resultInfo)
        memoryCount = findViewById(R.id.memoryCount)
        importButton = findViewById(R.id.importButton)
        forgetButton = findViewById(R.id.forgetButton)

        if (savedInstanceState == null) {
            artistsInput.setText(prefs.getString(KEY_ARTISTS, ""))
            genresInput.setText(prefs.getString(KEY_GENRES, getString(R.string.default_genres)))
            sizeGroup.check(if (prefs.getInt(KEY_SIZE, 50) == 25) R.id.size25 else R.id.size50)
        }

        makeButton.setOnClickListener { makePlaylist() }
        findViewById<MaterialButton>(R.id.playButton).setOnClickListener { openPlaylist() }
        importButton.setOnClickListener { pickHistoryFile.launch(arrayOf("*/*")) }
        findViewById<MaterialButton>(R.id.howToButton).setOnClickListener { showHowTo() }
        forgetButton.setOnClickListener { confirmForget() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveInputs()
    }

    private fun render(state: UiState) {
        makeButton.isEnabled = !state.working
        importButton.isEnabled = !state.working
        forgetButton.isEnabled = !state.working
        progress.visibility = if (state.working) View.VISIBLE else View.GONE

        statusText.visibility = if (state.status.isEmpty()) View.GONE else View.VISIBLE
        statusText.text = state.status
        statusText.setTextColor(
            MaterialColors.getColor(statusText, if (state.isError) AppCompatR.attr.colorError else MaterialR.attr.colorOnSurface),
        )

        memoryCount.text = getString(R.string.memory_count, "%,d".format(state.rememberedCount))

        val playlist = state.playlist
        resultCard.visibility = if (playlist == null) View.GONE else View.VISIBLE
        if (playlist != null) {
            resultTitle.text = playlist.title
            resultInfo.text = getString(R.string.result_songs, playlist.songCount)
        }
    }

    private fun makePlaylist() {
        saveInputs()
        val artists = lines(artistsInput)
        val genres = lines(genresInput)
        if (artists.isEmpty() && genres.isEmpty()) {
            viewModel.showError(getString(R.string.need_input))
            return
        }
        val size = if (sizeGroup.checkedButtonId == R.id.size25) 25 else 50
        viewModel.makePlaylist(artists, genres, size)
    }

    /**
     * Opens the playlist straight in the YouTube app (official or another YouTube app),
     * skipping the "open with Chrome?" question.
     */
    private fun openPlaylist() {
        val link = viewModel.state.value.playlist?.url ?: return
        val youtubeApps = linkHandlers(link) - linkHandlers("https://example.com")
        val app = if (YOUTUBE in youtubeApps) YOUTUBE else youtubeApps.firstOrNull()

        val intent = Intent(Intent.ACTION_VIEW, link.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        if (app != null) intent.setPackage(app)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.showError(getString(R.string.no_app))
        }
    }

    private fun linkHandlers(url: String): Set<String> {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    private fun showHowTo() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.how_to_title)
            .setMessage(R.string.how_to_body)
            .setPositiveButton(R.string.open_takeout) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, "https://takeout.google.com/".toUri()))
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun confirmForget() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.forget_title)
            .setMessage(R.string.forget_body)
            .setPositiveButton(R.string.forget_confirm) { _, _ -> viewModel.forgetAll() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveInputs() {
        prefs.edit {
            putString(KEY_ARTISTS, artistsInput.text?.toString().orEmpty())
            putString(KEY_GENRES, genresInput.text?.toString().orEmpty())
            putInt(KEY_SIZE, if (sizeGroup.checkedButtonId == R.id.size25) 25 else 50)
        }
    }

    /** Entries can be separated by new lines or commas. */
    private fun lines(input: TextInputEditText): List<String> =
        input.text?.toString().orEmpty().split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }

    private companion object {
        const val YOUTUBE = "com.google.android.youtube"
        const val KEY_ARTISTS = "artists"
        const val KEY_GENRES = "genres"
        const val KEY_SIZE = "size"
    }
}
