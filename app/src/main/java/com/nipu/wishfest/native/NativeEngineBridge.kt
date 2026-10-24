package com.nipu.wishfest.native

object NativeEngineBridge {

    init {
        try {
            System.loadLibrary("wishfest_native")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    external fun nCreateDocument(id: String, templateId: String, width: Int, height: Int, bgColor: String): Long
    external fun nDestroyDocument(handle: Long)
    external fun nSerializeDocument(handle: Long): String
    external fun nDeserializeDocument(json: String): Long

    external fun nAddTextLayer(
        handle: Long, id: String, x: Float, y: Float, width: Float, height: Float, rotation: Float,
        text: String, fontId: String, fontSize: Float, color: String, align: String
    )

    external fun nAddImageLayer(
        handle: Long, id: String, x: Float, y: Float, width: Float, height: Float, rotation: Float,
        assetPath: String
    )

    external fun nRemoveLayer(handle: Long, id: String)
    external fun nUpdateTransform(handle: Long, id: String, x: Float, y: Float, width: Float, height: Float, rotation: Float)
    external fun nUpdateCanvasBackground(handle: Long, color: String, photoPath: String)

    external fun nUndo(handle: Long)
    external fun nRedo(handle: Long)
    external fun nCanUndo(handle: Long): Boolean
    external fun nCanRedo(handle: Long): Boolean
}
