#pragma once

#include <cstdint>
#include <cstddef>
#include <algorithm>
#include <cmath>

namespace drumpads {

struct Voice {
    int padId = -1;
    const float* sampleData = nullptr;
    int totalFrames = 0;
    int currentFrame = 0;
    bool active = false;
    uint64_t triggerTimestamp = 0;
};

class VoicePool {
public:
    static constexpr int MAX_VOICES = 32;

    VoicePool() {
        for (int i = 0; i < MAX_VOICES; ++i) {
            voices_[i] = Voice{};
        }
    }

    // Must be real-time safe (O(32), no allocation, no locks)
    int allocateVoice(int padId, const float* sampleData, int totalFrames, uint64_t timestamp) noexcept {
        if (!sampleData || totalFrames <= 0) {
            return -1;
        }

        // 1. Search for an inactive voice
        int candidateIndex = -1;
        for (int i = 0; i < MAX_VOICES; ++i) {
            if (!voices_[i].active) {
                candidateIndex = i;
                break;
            }
        }

        // 2. If all 32 voices are active, apply voice stealing: pick the oldest voice
        if (candidateIndex == -1) {
            uint64_t oldestTimestamp = UINT64_MAX;
            for (int i = 0; i < MAX_VOICES; ++i) {
                if (voices_[i].triggerTimestamp < oldestTimestamp) {
                    oldestTimestamp = voices_[i].triggerTimestamp;
                    candidateIndex = i;
                }
            }
        }

        // 3. Initialize voice
        if (candidateIndex >= 0 && candidateIndex < MAX_VOICES) {
            voices_[candidateIndex].padId = padId;
            voices_[candidateIndex].sampleData = sampleData;
            voices_[candidateIndex].totalFrames = totalFrames;
            voices_[candidateIndex].currentFrame = 0;
            voices_[candidateIndex].active = true;
            voices_[candidateIndex].triggerTimestamp = timestamp;
            return candidateIndex;
        }

        return -1;
    }

    // Must be real-time safe: mixes active voices into outputBuffer
    void renderMixed(float* outputBuffer, int numFrames, int numChannels = 1) noexcept {
        // Clear buffer
        const int totalSamples = numFrames * numChannels;
        for (int i = 0; i < totalSamples; ++i) {
            outputBuffer[i] = 0.0f;
        }

        // Render each active voice
        for (int v = 0; v < MAX_VOICES; ++v) {
            if (!voices_[v].active) continue;

            Voice& voice = voices_[v];
            const float* src = voice.sampleData;
            const int remaining = voice.totalFrames - voice.currentFrame;
            const int framesToRender = std::min(numFrames, remaining);

            if (numChannels == 1) {
                for (int i = 0; i < framesToRender; ++i) {
                    outputBuffer[i] += src[voice.currentFrame + i];
                }
            } else if (numChannels == 2) {
                for (int i = 0; i < framesToRender; ++i) {
                    const float s = src[voice.currentFrame + i];
                    outputBuffer[i * 2] += s;
                    outputBuffer[i * 2 + 1] += s;
                }
            }

            voice.currentFrame += framesToRender;
            if (voice.currentFrame >= voice.totalFrames) {
                voice.active = false;
            }
        }

        // Soft clip master output to prevent digital clipping distortion
        for (int i = 0; i < totalSamples; ++i) {
            float s = outputBuffer[i];
            if (s > 1.0f) {
                s = 1.0f - std::exp(-s);
            } else if (s < -1.0f) {
                s = -1.0f + std::exp(s);
            }
            outputBuffer[i] = s;
        }
    }

    int getActiveVoiceCount() const noexcept {
        int count = 0;
        for (int i = 0; i < MAX_VOICES; ++i) {
            if (voices_[i].active) count++;
        }
        return count;
    }

    const Voice& getVoice(int index) const noexcept {
        return voices_[index];
    }

    void reset() noexcept {
        for (int i = 0; i < MAX_VOICES; ++i) {
            voices_[i].active = false;
            voices_[i].currentFrame = 0;
            voices_[i].padId = -1;
        }
    }

private:
    Voice voices_[MAX_VOICES];
};

} // namespace drumpads
