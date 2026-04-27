package com.example.chatterinomobile.data.local

import android.content.Context
import java.io.File

class DiskCacheRoot(context: Context) {

    private val root: File = File(context.cacheDir, "metadata").apply { mkdirs() }

    fun emoteDir(): File = File(root, "emotes").apply { mkdirs() }
    fun badgeDir(): File = File(root, "badges").apply { mkdirs() }
    fun dimensionFile(): File = File(root, "emote_dimensions.json")

    fun wipe() {
        root.deleteRecursively()
        root.mkdirs()
    }
}
