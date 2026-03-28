package com.example.vrdesert;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.util.Random;

/**
 * Procedurally generates all sound effects — no audio files needed.
 * - Ambient arctic wind (looping filtered noise)
 * - Ice crack on calving event
 * - Water splash on impact
 */
public class SoundEngine {

    private static final int SAMPLE_RATE = 22050;
    private volatile boolean running = false;
    private Thread ambientThread;
    private final Random rand = new Random();

    // Pre-generated one-shot buffers
    private short[] crackBuffer;
    private short[] splashBuffer;

    public SoundEngine() {
        generateCrackSound();
        generateSplashSound();
    }

    /** Starts the ambient wind loop on a background thread. */
    public void start() {
        if (running) return;
        running = true;
        ambientThread = new Thread(this::windLoop, "AmbientWind");
        ambientThread.start();
    }

    /** Stops all audio. */
    public void stop() {
        running = false;
        if (ambientThread != null) {
            ambientThread.interrupt();
            ambientThread = null;
        }
    }

    /** Plays ice cracking sound (non-blocking). */
    public void playCrack() {
        new Thread(() -> playOneShot(crackBuffer, 1.0f), "CrackSFX").start();
    }

    /** Plays water splash sound (non-blocking). */
    public void playSplash() {
        new Thread(() -> playOneShot(splashBuffer, 0.8f), "SplashSFX").start();
    }

    // --- Wind ambient loop ---
    private void windLoop() {
        int bufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufSize < 4096) bufSize = 4096;

        AudioTrack track = new AudioTrack(
                AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufSize, AudioTrack.MODE_STREAM);

        track.play();
        short[] buf = new short[2048];
        float filtered = 0f;
        float filtered2 = 0f;
        float t = 0f;

        while (running) {
            for (int i = 0; i < buf.length; i++) {
                float noise = rand.nextFloat() * 2f - 1f;
                // Double low-pass for soft wind
                filtered  = filtered  * 0.995f + noise * 0.005f;
                filtered2 = filtered2 * 0.990f + filtered * 0.010f;
                // Slow volume modulation (wind gusts)
                t += 1f / SAMPLE_RATE;
                float gust = 0.6f + 0.4f * (float) Math.sin(t * 0.3) * (float) Math.sin(t * 0.17);
                buf[i] = (short)(filtered2 * 5000f * gust);
            }
            track.write(buf, 0, buf.length);
        }

        track.stop();
        track.release();
    }

    // --- Generate ice crack PCM data ---
    private void generateCrackSound() {
        // 0.4 second sharp crack
        int samples = (int)(SAMPLE_RATE * 0.4f);
        crackBuffer = new short[samples];
        float f = 0f;
        for (int i = 0; i < samples; i++) {
            float t = (float) i / samples;
            // Fast decay envelope
            float env = (float) Math.pow(Math.max(0, 1.0 - t * 2.5), 3);
            // Mix of noise + sharp click + low rumble
            float noise = rand.nextFloat() * 2f - 1f;
            float click = (float) Math.sin(i * 0.15) * (float) Math.sin(i * 0.08);
            float rumble = (float) Math.sin(i * 0.01) * 0.5f;
            f = f * 0.7f + (noise * 0.6f + click * 0.3f + rumble) * 0.3f;
            crackBuffer[i] = (short)(f * env * 28000f);
        }
    }

    // --- Generate splash PCM data ---
    private void generateSplashSound() {
        // 0.6 second splash
        int samples = (int)(SAMPLE_RATE * 0.6f);
        splashBuffer = new short[samples];
        float f1 = 0f, f2 = 0f;
        for (int i = 0; i < samples; i++) {
            float t = (float) i / samples;
            // Slower decay, holds longer
            float env = (float) Math.pow(Math.max(0, 1.0 - t), 2);
            // Filtered noise (water-like) — bandpass
            float noise = rand.nextFloat() * 2f - 1f;
            f1 = f1 * 0.85f + noise * 0.15f;
            f2 = f2 * 0.92f + f1 * 0.08f;
            float water = f1 - f2; // bandpass
            // Add bubble pops
            float bubbles = 0f;
            if (rand.nextFloat() < 0.02f * (1f - t)) {
                bubbles = (float) Math.sin(i * (0.05 + rand.nextFloat() * 0.2)) * 0.4f;
            }
            splashBuffer[i] = (short)((water + bubbles) * env * 22000f);
        }
    }

    // --- Play a pre-generated buffer as one-shot ---
    private void playOneShot(short[] data, float volume) {
        try {
            int bufSize = data.length * 2;
            AudioTrack track = new AudioTrack(
                    AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bufSize, AudioTrack.MODE_STATIC);
            track.write(data, 0, data.length);
            track.setStereoVolume(volume, volume);
            track.play();
            // Wait for playback to finish, then release
            Thread.sleep((long)(data.length * 1000L / SAMPLE_RATE) + 100);
            track.stop();
            track.release();
        } catch (Exception e) {
            // Silently fail — audio is non-critical
        }
    }
}
