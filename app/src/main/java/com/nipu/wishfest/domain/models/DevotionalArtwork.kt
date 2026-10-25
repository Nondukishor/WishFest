package com.nipu.wishfest.domain.models

/** Bundled transparent artwork works offline and keeps saved cards portable. */
data class DevotionalArtwork(val name: String, val assetPath: String)

object DevotionalArtworks {
    val items = listOf(
        DevotionalArtwork("Lakshmi", "devotional/lakshmi.png"),
        DevotionalArtwork("Ganesh", "devotional/ganesh.png"),
        DevotionalArtwork("Kali", "devotional/kali.png")
    )

    fun nameFor(assetPath: String): String = items.firstOrNull { it.assetPath == assetPath }?.name ?: "Image"
}
