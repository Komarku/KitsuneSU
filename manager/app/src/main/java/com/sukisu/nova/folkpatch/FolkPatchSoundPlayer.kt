package com.sukisu.nova.folkpatch

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * Plays FolkPatch bundled startup / voice sound effects from assets/sound/.
 */
object FolkPatchSoundPlayer {
    private const val TAG = "KitsuneSoundPlayer"
    private var activePlayer: MediaPlayer? = null

    fun playStartupIfEnabled(context: Context) {
        val state = FolkPatchPrefs.state.value
        if (!state.startupSoundEnabled) return
        playAssetSound(context, state.startupSoundAsset)
    }

    fun playAssetSound(context: Context, assetName: String) {
        runCatching {
            activePlayer?.release()
            activePlayer = null
            val afd = context.assets.openFd("sound/$assetName")
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener {
                it.release()
                if (activePlayer === it) activePlayer = null
            }
            mp.setOnErrorListener { player, _, _ ->
                player.release()
                if (activePlayer === player) activePlayer = null
                true
            }
            mp.prepare()
            mp.start()
            activePlayer = mp
        }.onFailure {
            Log.w(TAG, "Failed to play sound asset: $assetName", it)
        }
    }
}

