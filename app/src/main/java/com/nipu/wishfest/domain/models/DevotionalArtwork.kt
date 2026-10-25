package com.nipu.wishfest.domain.models

/**
 * Bundled transparent artwork works offline and keeps saved cards portable.
 *
 * [tintable] illustrations are white silhouettes — users can recolor them in the editor.
 * Full-color artworks keep their original palette unless a tint is explicitly applied.
 */
data class DevotionalArtwork(
    val name: String,
    val assetPath: String,
    val tintable: Boolean = false,
    val category: String = "Hindu",
    val defaultTint: String = "#E5A93C"
)

object DevotionalArtworks {
    val items = listOf(
        // Full-color classic art
        DevotionalArtwork("Lakshmi", "devotional/lakshmi.png", tintable = false, category = "Hindu"),
        DevotionalArtwork("Ganesh", "devotional/ganesh.png", tintable = false, category = "Hindu"),
        DevotionalArtwork("Kali", "devotional/kali.png", tintable = false, category = "Hindu"),

        // Tintable illustration silhouettes
        DevotionalArtwork("Durga", "devotional/durga_face.png", tintable = true, category = "Hindu", defaultTint = "#C41E3A"),
        DevotionalArtwork("Kali Face", "devotional/kali_face.png", tintable = true, category = "Hindu", defaultTint = "#1B1B2F"),
        DevotionalArtwork("Krishna", "devotional/krishna_face.png", tintable = true, category = "Hindu", defaultTint = "#1E88E5"),
        DevotionalArtwork("Om", "devotional/om_symbol.png", tintable = true, category = "Hindu", defaultTint = "#E5A93C"),

        DevotionalArtwork("Jesus", "devotional/jesus_face.png", tintable = true, category = "Christian", defaultTint = "#5D4037"),
        DevotionalArtwork("Mary", "devotional/mary_face.png", tintable = true, category = "Christian", defaultTint = "#1565C0"),
        DevotionalArtwork("Cross", "devotional/cross_symbol.png", tintable = true, category = "Christian", defaultTint = "#8B1E3D"),

        DevotionalArtwork("Buddha", "devotional/buddha_face.png", tintable = true, category = "Buddhist", defaultTint = "#F5B942"),
        DevotionalArtwork("Crescent", "devotional/crescent_star.png", tintable = true, category = "Islamic", defaultTint = "#0F766E")
    )

    fun nameFor(assetPath: String): String =
        items.firstOrNull { it.assetPath == assetPath }?.name ?: "Image"

    fun find(assetPath: String): DevotionalArtwork? =
        items.firstOrNull { it.assetPath == assetPath }

    fun isTintable(assetPath: String): Boolean =
        find(assetPath)?.tintable == true

    fun defaultTintFor(assetPath: String): String =
        find(assetPath)?.defaultTint ?: "#E5A93C"

    val categories: List<String> = listOf("All") + items.map { it.category }.distinct()
}
