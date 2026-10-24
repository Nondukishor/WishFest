#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include "CardDocument.h"
#include "Commands.h"
#include "Serializer.h"

#define LOG_TAG "WishFestJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct NativeEngineHandle {
    std::shared_ptr<wishfest::CardDocument> doc;
    wishfest::CommandManager commandManager;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nCreateDocument(
    JNIEnv* env, jobject /* this */, jstring jId, jstring jTemplateId, jint width, jint height, jstring jBgColor) {

    const char* idStr = env->GetStringUTFChars(jId, nullptr);
    const char* tIdStr = env->GetStringUTFChars(jTemplateId, nullptr);
    const char* bgStr = env->GetStringUTFChars(jBgColor, nullptr);

    wishfest::CanvasConfig canvas;
    canvas.width = width;
    canvas.height = height;
    canvas.backgroundColor = bgStr ? bgStr : "#FFFFFF";

    auto doc = std::make_shared<wishfest::CardDocument>(
        idStr ? idStr : "doc-1",
        tIdStr ? tIdStr : "template-1",
        canvas
    );

    env->ReleaseStringUTFChars(jId, idStr);
    env->ReleaseStringUTFChars(jTemplateId, tIdStr);
    env->ReleaseStringUTFChars(jBgColor, bgStr);

    auto* handle = new NativeEngineHandle();
    handle->doc = doc;
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nDestroyDocument(
    JNIEnv* env, jobject /* this */, jlong handlePtr) {
    if (handlePtr != 0) {
        auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
        delete handle;
    }
}

JNIEXPORT jstring JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nSerializeDocument(
    JNIEnv* env, jobject /* this */, jlong handlePtr) {
    if (handlePtr == 0) return env->NewStringUTF("{}");
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (!handle->doc) return env->NewStringUTF("{}");

    std::string json = wishfest::Serializer::serialize(*(handle->doc));
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jlong JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nDeserializeDocument(
    JNIEnv* env, jobject /* this */, jstring jJson) {
    const char* jsonStr = env->GetStringUTFChars(jJson, nullptr);
    std::string json(jsonStr ? jsonStr : "");
    env->ReleaseStringUTFChars(jJson, jsonStr);

    auto doc = wishfest::Serializer::deserialize(json);
    auto* handle = new NativeEngineHandle();
    handle->doc = doc;
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nAddTextLayer(
    JNIEnv* env, jobject /* this */, jlong handlePtr, jstring jId,
    jfloat x, jfloat y, jfloat width, jfloat height, jfloat rotation,
    jstring jText, jstring jFontId, jfloat fontSize, jstring jColor, jstring jAlign) {

    if (handlePtr == 0) return;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (!handle->doc) return;

    const char* idStr = env->GetStringUTFChars(jId, nullptr);
    const char* textStr = env->GetStringUTFChars(jText, nullptr);
    const char* fontStr = env->GetStringUTFChars(jFontId, nullptr);
    const char* colorStr = env->GetStringUTFChars(jColor, nullptr);
    const char* alignStr = env->GetStringUTFChars(jAlign, nullptr);

    wishfest::Transform t{x, y, width, height, rotation};
    auto layer = std::make_shared<wishfest::TextLayer>(
        idStr ? idStr : "", t,
        textStr ? textStr : "",
        fontStr ? fontStr : "default",
        fontSize,
        colorStr ? colorStr : "#000000",
        alignStr ? alignStr : "center"
    );

    env->ReleaseStringUTFChars(jId, idStr);
    env->ReleaseStringUTFChars(jText, textStr);
    env->ReleaseStringUTFChars(jFontId, fontStr);
    env->ReleaseStringUTFChars(jColor, colorStr);
    env->ReleaseStringUTFChars(jAlign, alignStr);

    auto cmd = std::make_unique<wishfest::AddLayerCommand>(layer);
    handle->commandManager.executeCommand(*(handle->doc), std::move(cmd));
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nAddImageLayer(
    JNIEnv* env, jobject /* this */, jlong handlePtr, jstring jId,
    jfloat x, jfloat y, jfloat width, jfloat height, jfloat rotation, jstring jAssetPath) {

    if (handlePtr == 0) return;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (!handle->doc) return;

    const char* idStr = env->GetStringUTFChars(jId, nullptr);
    const char* pathStr = env->GetStringUTFChars(jAssetPath, nullptr);

    wishfest::Transform t{x, y, width, height, rotation};
    auto layer = std::make_shared<wishfest::ImageLayer>(
        idStr ? idStr : "", t, pathStr ? pathStr : ""
    );

    env->ReleaseStringUTFChars(jId, idStr);
    env->ReleaseStringUTFChars(jAssetPath, pathStr);

    auto cmd = std::make_unique<wishfest::AddLayerCommand>(layer);
    handle->commandManager.executeCommand(*(handle->doc), std::move(cmd));
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nRemoveLayer(
    JNIEnv* env, jobject /* this */, jlong handlePtr, jstring jId) {
    if (handlePtr == 0) return;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (!handle->doc) return;

    const char* idStr = env->GetStringUTFChars(jId, nullptr);
    std::string layerId(idStr ? idStr : "");
    env->ReleaseStringUTFChars(jId, idStr);

    auto layer = handle->doc->findLayer(layerId);
    if (layer) {
        auto cmd = std::make_unique<wishfest::RemoveLayerCommand>(layer);
        handle->commandManager.executeCommand(*(handle->doc), std::move(cmd));
    }
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nUpdateTransform(
    JNIEnv* env, jobject /* this */, jlong handlePtr, jstring jId,
    jfloat x, jfloat y, jfloat width, jfloat height, jfloat rotation) {

    if (handlePtr == 0) return;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (!handle->doc) return;

    const char* idStr = env->GetStringUTFChars(jId, nullptr);
    std::string layerId(idStr ? idStr : "");
    env->ReleaseStringUTFChars(jId, idStr);

    auto layer = handle->doc->findLayer(layerId);
    if (layer) {
        wishfest::Transform oldT = layer->getTransform();
        wishfest::Transform newT{x, y, width, height, rotation};
        auto cmd = std::make_unique<wishfest::UpdateTransformCommand>(layerId, oldT, newT);
        handle->commandManager.executeCommand(*(handle->doc), std::move(cmd));
    }
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nUpdateCanvasBackground(
    JNIEnv* env, jobject /* this */, jlong handlePtr, jstring jColor, jstring jPhotoPath) {

    if (handlePtr == 0) return;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (!handle->doc) return;

    const char* colorStr = env->GetStringUTFChars(jColor, nullptr);
    const char* photoStr = env->GetStringUTFChars(jPhotoPath, nullptr);

    auto canvas = handle->doc->getCanvas();
    if (colorStr) canvas.backgroundColor = colorStr;
    if (photoStr) canvas.backgroundPhotoPath = photoStr;
    handle->doc->setCanvas(canvas);

    env->ReleaseStringUTFChars(jColor, colorStr);
    env->ReleaseStringUTFChars(jPhotoPath, photoStr);
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nUndo(
    JNIEnv* env, jobject /* this */, jlong handlePtr) {
    if (handlePtr == 0) return;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (handle->doc) {
        handle->commandManager.undo(*(handle->doc));
    }
}

JNIEXPORT void JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nRedo(
    JNIEnv* env, jobject /* this */, jlong handlePtr) {
    if (handlePtr == 0) return;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    if (handle->doc) {
        handle->commandManager.redo(*(handle->doc));
    }
}

JNIEXPORT jboolean JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nCanUndo(
    JNIEnv* env, jobject /* this */, jlong handlePtr) {
    if (handlePtr == 0) return JNI_FALSE;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    return handle->commandManager.canUndo() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_nipu_wishfest_native_NativeEngineBridge_nCanRedo(
    JNIEnv* env, jobject /* this */, jlong handlePtr) {
    if (handlePtr == 0) return JNI_FALSE;
    auto* handle = reinterpret_cast<NativeEngineHandle*>(handlePtr);
    return handle->commandManager.canRedo() ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
