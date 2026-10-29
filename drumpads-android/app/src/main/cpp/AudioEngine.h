#pragma once

#include <memory>
#include <atomic>
#include <chrono>
#include "LockFreeQueue.h"
#include "VoicePool.h"
#include "SampleManager.h"

#if __has_include(<oboe/Oboe.h>)
#include <oboe/Oboe.h>
#define HAVE_OBOE 1
#else
#define HAVE_OBOE 0
#endif

namespace drumpads {

struct NoteOnEvent {
    int padIndex = -1;
    uint64_t timestamp = 0;
};

class AudioEngine
#if HAVE_OBOE
    : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback
#endif
{
public:
    AudioEngine();
    ~AudioEngine();

    bool start();
    void stop();

    bool triggerPad(int padIndex) noexcept;
    void loadSample(int padIndex, const float* data, int numFrames);

    void setStressTest(bool enable);
    bool isStressTestActive() const noexcept;
    int32_t getXRunCount() const noexcept;

#if HAVE_OBOE
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream *audioStream,
        void *audioData,
        int32_t numFrames) override;

    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) override;
#endif

    // For testing and manual rendering
    void renderAudioFrames(float* output, int32_t numFrames, int numChannels = 1) noexcept;

    VoicePool& getVoicePool() noexcept { return voicePool_; }
    SampleManager& getSampleManager() noexcept { return sampleManager_; }

private:
#if HAVE_OBOE
    std::shared_ptr<oboe::AudioStream> stream_;
#endif
    VoicePool voicePool_;
    SampleManager sampleManager_;
    LockFreeQueue<NoteOnEvent, 256> commandQueue_;

    std::atomic<bool> isRunning_{false};
    std::atomic<bool> stressTestEnabled_{false};
    std::atomic<int32_t> xrunCount_{0};

    uint64_t stressTestStartNs_{0};
    uint64_t stressLastTriggerNs_{0};
    uint64_t eventSequence_{0};

    static uint64_t getCurrentTimeNs() noexcept {
        return static_cast<uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(
                std::chrono::steady_clock::now().time_since_epoch()
            ).count()
        );
    }
};

} // namespace drumpads
