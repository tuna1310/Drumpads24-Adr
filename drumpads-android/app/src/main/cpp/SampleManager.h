#pragma once

#include <vector>
#include <cmath>
#include <cstdint>

namespace drumpads {

class SampleManager {
public:
    static constexpr int NUM_PADS = 24;
    static constexpr int SAMPLE_RATE = 44100;

    SampleManager() {
        samples_.resize(NUM_PADS);
        generateFallbackSamples();
    }

    void loadSample(int padIndex, const float* data, int numFrames) {
        if (padIndex < 0 || padIndex >= NUM_PADS || !data || numFrames <= 0) return;
        samples_[padIndex].assign(data, data + numFrames);
    }

    const float* getSampleData(int padIndex) const noexcept {
        if (padIndex < 0 || padIndex >= NUM_PADS || samples_[padIndex].empty()) return nullptr;
        return samples_[padIndex].data();
    }

    int getSampleLength(int padIndex) const noexcept {
        if (padIndex < 0 || padIndex >= NUM_PADS) return 0;
        return static_cast<int>(samples_[padIndex].size());
    }

private:
    void generateFallbackSamples() {
        for (int p = 0; p < NUM_PADS; ++p) {
            int length = static_cast<int>(SAMPLE_RATE * 0.35); // 350ms
            samples_[p].resize(length);

            if (p < 6) {
                // DRUMS: Pitch sweeps and noise bursts
                double startFreq = 160.0 - p * 15.0;
                double endFreq = 45.0;
                double phase = 0.0;
                for (int i = 0; i < length; ++i) {
                    double t = static_cast<double>(i) / SAMPLE_RATE;
                    double freq = startFreq * std::exp(-t * 20.0) + endFreq;
                    phase += 2.0 * M_PI * freq / SAMPLE_RATE;
                    float body = static_cast<float>(std::sin(phase) * std::exp(-t * 12.0));
                    float noise = (p >= 2 && p <= 5) ? (static_cast<float>((rand() % 2000 - 1000) / 1000.0) * std::exp(-t * (p == 4 ? 40.0 : 18.0))) : 0.0f;
                    samples_[p][i] = std::tanh(body * 0.6f + noise * 0.4f);
                }
            } else if (p < 12) {
                // BASS: Low harmonic frequencies
                double freq = 55.0 * std::pow(2.0, (p - 6) * 2.0 / 12.0); // A1 to D2
                for (int i = 0; i < length; ++i) {
                    double t = static_cast<double>(i) / SAMPLE_RATE;
                    float s = static_cast<float>(std::sin(2.0 * M_PI * freq * t) + 0.3 * std::sin(4.0 * M_PI * freq * t));
                    samples_[p][i] = s * static_cast<float>(std::exp(-t * 4.0));
                }
            } else if (p < 18) {
                // MELODY: Pentatonic / Major scale tones
                double freqs[] = { 261.63, 293.66, 329.63, 392.00, 440.00, 523.25 };
                double freq = freqs[p - 12];
                for (int i = 0; i < length; ++i) {
                    double t = static_cast<double>(i) / SAMPLE_RATE;
                    float s = static_cast<float>(std::sin(2.0 * M_PI * freq * t) + 0.2 * std::sin(6.0 * M_PI * freq * t));
                    samples_[p][i] = s * static_cast<float>(std::exp(-t * 5.0));
                }
            } else {
                // FX: Lasers, sweep, crashes
                for (int i = 0; i < length; ++i) {
                    double t = static_cast<double>(i) / SAMPLE_RATE;
                    double freq = 1200.0 * std::exp(-t * 15.0) + 100.0;
                    float s = static_cast<float>(std::sin(2.0 * M_PI * freq * t));
                    float noise = static_cast<float>((rand() % 2000 - 1000) / 1000.0) * std::exp(-t * 6.0);
                    samples_[p][i] = std::tanh(s * 0.5f + noise * 0.5f);
                }
            }
        }
    }

    std::vector<std::vector<float>> samples_;
};

} // namespace drumpads
