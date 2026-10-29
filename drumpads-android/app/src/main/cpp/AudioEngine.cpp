#include "AudioEngine.h"
#include <android/log.h>

#define LOG_TAG "DrumPadsAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace drumpads {

AudioEngine::AudioEngine() {
    LOGI("AudioEngine initialized with 32-voice hard limit pool");
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    if (isRunning_.load()) return true;

#if HAVE_OBOE
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Stereo)
        ->setSampleRate(44100)
        ->setDataCallback(this)
        ->setErrorCallback(this);

    oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open Oboe stream: %s", oboe::convertToText(result));
        return false;
    }

    result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start Oboe stream: %s", oboe::convertToText(result));
        stream_->close();
        stream_.reset();
        return false;
    }

    LOGI("Oboe audio stream started: bufferSize=%d, sampleRate=%d",
         stream_->getBufferSizeInFrames(), stream_->getSampleRate());
#endif

    isRunning_.store(true);
    return true;
}

void AudioEngine::stop() {
    if (!isRunning_.load()) return;
    isRunning_.store(false);

#if HAVE_OBOE
    if (stream_) {
        stream_->stop();
        stream_->close();
        stream_.reset();
    }
#endif
    voicePool_.reset();
    LOGI("AudioEngine stopped");
}

bool AudioEngine::triggerPad(int padIndex) noexcept {
    if (padIndex < 0 || padIndex >= SampleManager::NUM_PADS) return false;
    NoteOnEvent evt;
    evt.padIndex = padIndex;
    evt.timestamp = ++eventSequence_;
    return commandQueue_.push(evt);
}

void AudioEngine::loadSample(int padIndex, const float* data, int numFrames) {
    sampleManager_.loadSample(padIndex, data, numFrames);
}

void AudioEngine::setStressTest(bool enable) {
    stressTestEnabled_.store(enable);
    if (enable) {
        stressTestStartNs_ = getCurrentTimeNs();
        stressLastTriggerNs_ = 0;
        LOGI("Stress Test Mode ENABLED: Running 32 concurrent voices for 60 seconds");
    } else {
        LOGI("Stress Test Mode DISABLED");
    }
}

bool AudioEngine::isStressTestActive() const noexcept {
    return stressTestEnabled_.load();
}

int32_t AudioEngine::getXRunCount() const noexcept {
#if HAVE_OBOE
    if (stream_) {
        auto result = stream_->getXRunCount();
        if (result) {
            return result.value();
        }
    }
#endif
    return xrunCount_.load();
}

#if HAVE_OBOE
oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream *audioStream,
    void *audioData,
    int32_t numFrames) {

    // REAL-TIME SAFE: No malloc, no lock, no I/O, no string formatting!
    float* floatBuffer = static_cast<float*>(audioData);
    renderAudioFrames(floatBuffer, numFrames, audioStream->getChannelCount());

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) {
    LOGE("Audio stream error: %s. Attempting restart...", oboe::convertToText(error));
    if (isRunning_.load()) {
        stop();
        start();
    }
}
#endif

void AudioEngine::renderAudioFrames(float* output, int32_t numFrames, int numChannels) noexcept {
    // 1. Stress test injection (if enabled)
    if (stressTestEnabled_.load(std::memory_order_relaxed)) {
        const uint64_t now = getCurrentTimeNs();
        const uint64_t elapsedSec = (now - stressTestStartNs_) / 1000000000ULL;
        if (elapsedSec < 60) {
            // Trigger all 32 voices periodically every 200ms
            if (now - stressLastTriggerNs_ > 200000000ULL) {
                stressLastTriggerNs_ = now;
                for (int v = 0; v < VoicePool::MAX_VOICES; ++v) {
                    const int pad = v % SampleManager::NUM_PADS;
                    const float* data = sampleManager_.getSampleData(pad);
                    const int len = sampleManager_.getSampleLength(pad);
                    voicePool_.allocateVoice(pad, data, len, ++eventSequence_);
                }
            }
        } else {
            stressTestEnabled_.store(false, std::memory_order_relaxed);
        }
    }

    // 2. Consume NoteOn events from lock-free queue
    NoteOnEvent evt;
    while (commandQueue_.pop(evt)) {
        const float* sampleData = sampleManager_.getSampleData(evt.padIndex);
        const int totalFrames = sampleManager_.getSampleLength(evt.padIndex);
        if (sampleData && totalFrames > 0) {
            voicePool_.allocateVoice(evt.padIndex, sampleData, totalFrames, evt.timestamp);
        }
    }

    // 3. Render and mix voices into audio output buffer (pure calculation, no alloc)
    voicePool_.renderMixed(output, numFrames, numChannels);
}

} // namespace drumpads
