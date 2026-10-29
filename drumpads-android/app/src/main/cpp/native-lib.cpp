#include <jni.h>
#include <memory>
#include <vector>
#include "AudioEngine.h"

static std::unique_ptr<drumpads::AudioEngine> sAudioEngine;

extern "C" {

JNIEXPORT void JNICALL
Java_com_drumpads_app_core_audio_NativeAudioEngineBridge_nativeInit(JNIEnv *env, jobject thiz) {
    if (!sAudioEngine) {
        sAudioEngine = std::make_unique<drumpads::AudioEngine>();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_drumpads_app_core_audio_NativeAudioEngineBridge_nativeStart(JNIEnv *env, jobject thiz) {
    if (sAudioEngine) {
        return sAudioEngine->start() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_drumpads_app_core_audio_NativeAudioEngineBridge_nativeStop(JNIEnv *env, jobject thiz) {
    if (sAudioEngine) {
        sAudioEngine->stop();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_drumpads_app_core_audio_NativeAudioEngineBridge_nativeTriggerPad(JNIEnv *env, jobject thiz, jint pad_index) {
    if (sAudioEngine) {
        return sAudioEngine->triggerPad(pad_index) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_drumpads_app_core_audio_NativeAudioEngineBridge_nativeLoadSample(
    JNIEnv *env, jobject thiz, jint pad_index, jshortArray pcm_data, jint length) {
    if (!sAudioEngine || !pcm_data || length <= 0) return;

    jshort *elements = env->GetShortArrayElements(pcm_data, nullptr);
    if (!elements) return;

    std::vector<float> floatBuffer(length);
    for (int i = 0; i < length; ++i) {
        floatBuffer[i] = static_cast<float>(elements[i]) / 32768.0f;
    }

    sAudioEngine->loadSample(pad_index, floatBuffer.data(), length);
    env->ReleaseShortArrayElements(pcm_data, elements, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_drumpads_app_core_audio_NativeAudioEngineBridge_nativeSetStressTest(
    JNIEnv *env, jobject thiz, jboolean enable) {
    if (sAudioEngine) {
        sAudioEngine->setStressTest(enable == JNI_TRUE);
    }
}

JNIEXPORT jint JNICALL
Java_com_drumpads_app_core_audio_NativeAudioEngineBridge_nativeGetXRunCount(JNIEnv *env, jobject thiz) {
    if (sAudioEngine) {
        return sAudioEngine->getXRunCount();
    }
    return 0;
}

} // extern "C"
