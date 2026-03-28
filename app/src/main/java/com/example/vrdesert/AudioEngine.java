package com.example.vrdesert;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;

import java.util.Random;

public class AudioEngine {

    private ToneGenerator toneGenerator;
    private AudioTrack windTrack;
    private Thread windThread;
    private boolean isPlaying = false;

    public AudioEngine() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    }

    public void playCollectionSound() {
        // Simple distinct hardware beep requiring no heavy soundfile loading
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
    }

    public void startDesertWind() {
        int sampleRate = 8000; // Low frequency base
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        
        windTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 
                bufferSize, AudioTrack.MODE_STREAM);
                
        isPlaying = true;
        windTrack.play();
        
        windThread = new Thread(() -> {
            short[] buffer = new short[bufferSize];
            Random rand = new Random();
            float lastVal = 0;
            
            while (isPlaying) {
                for (int i = 0; i < bufferSize; i++) {
                    // Generate white noise / high frequency harsh wind
                    float white = (rand.nextFloat() * 2f - 1f);
                    // Lighter filter to keep it biting and harsh
                    float icy = (lastVal + (0.2f * white)) / 1.15f; 
                    lastVal = icy;
                    
                    // Modulate volume rapidly to simulate howling gusts
                    float modulation = (float) Math.sin((System.currentTimeMillis() % 6000) / 6000f * Math.PI * 2) * 0.4f + 0.6f;
                    
                    // Scale volume heavily down so it's subtle background ambiance
                    buffer[i] = (short) (icy * 12000 * modulation); 
                }
                windTrack.write(buffer, 0, buffer.length);
            }
        });
        windThread.start();
    }

    public void stopAudio() {
        isPlaying = false;
        if (windTrack != null) {
            windTrack.stop();
            windTrack.release();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}
