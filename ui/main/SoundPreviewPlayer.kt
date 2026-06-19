package com.example.timeboxvibe.ui.main

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

object SoundPreviewPlayer {
    private var activeTrack: AudioTrack? = null
    private var tickTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    fun playPreview(context: Context, soundKey: String, volume: Float) {
        stop()
        when (soundKey) {
            "oriental" -> playOrientalPreview(context, volume)
            "synth-bad-apple", "synth-senbonzakura" -> {
                // PC-98 tunes return all channels from getMelody(false); use time-based preview
                val allNotes = SoundMelodies.getMelody(soundKey, volume, false)
                val previewNotes = allNotes.filter { it.startMs < 3500 }
                playTones(previewNotes)
            }
            "synth-chime", "synth-victory" -> {
                val melody = SoundMelodies.getMelody(soundKey, volume, false).take(8)
                val bass = SoundMelodies.getMelody(soundKey, volume, true).take(8)
                playTones(melody + bass)
            }
        }
    }

    /**
     * The PC-98 "Thock"
     * Generates a heavy, mechanical UI click using a low-frequency square/triangle blend
     * with a rapid pitch-drop (FM synthesis) to simulate a physical switch.
     */
    fun playTick(volume: Float) {
        try {
            var track = tickTrack
            if (track == null) {
                val sampleRate = 44100
                val durationMs = 35
                val numSamples = sampleRate * durationMs / 1000
                val buffer = FloatArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    // Pitch drop: Starts at 300Hz, drops rapidly to 100Hz
                    val freq = 300.0 * exp(-50.0 * t)
                    val phase = 2.0 * Math.PI * freq * t

                    // Crunchy Square-like wave
                    var sample = if (sin(phase) > 0) 0.8f else -0.8f

                    val ageMs = i * 1000 / sampleRate
                    // Extremely sharp attack and decay for that mechanical "clack"
                    val envelope = when {
                        ageMs < 2 -> ageMs / 2f
                        else -> {
                            val decayFactor = (ageMs - 2) / (durationMs - 2).toFloat()
                            exp(-8.0 * decayFactor).toFloat()
                        }
                    }
                    sample *= envelope * 0.15f * volume
                    buffer[i] = sample
                }

                val shortBuffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val clamped = maxOf(-1.0f, minOf(1.0f, buffer[i]))
                    shortBuffer[i] = (clamped * 32767).toInt().toShort()
                }

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(shortBuffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(shortBuffer, 0, shortBuffer.size)
                tickTrack = track
            }

            track.stop()
            track.setPlaybackHeadPosition(0)
            track.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            activeTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
                it.release()
            }
        } catch (e: Exception) { e.printStackTrace() }
        activeTrack = null

        // WE DO NOT RELEASE tickTrack HERE ANYMORE!
        // We want the 'thock' to stay loaded in memory so it has zero latency.

        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) { e.printStackTrace() }
        mediaPlayer = null

        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
    }





    /**
     * Compute the amplitude envelope for a given sample age.
     * Uses legacy exponential decay when useADSR is false (backward compat),
     * or a proper Attack-Decay-Sustain-Release envelope when true.
     */
    private fun computeEnvelope(ageMs: Int, spec: ToneSpec): Float {
        if (!spec.useADSR) {
            // Legacy envelope — identical to the original implementation
            return when {
                ageMs < 30 -> ageMs / 30f
                else -> {
                    val decayFactor = (ageMs - 30) / (spec.durationMs - 30).toFloat()
                    exp(-3.0 * decayFactor).toFloat()
                }
            }
        }
        // ADSR envelope
        val sustainEnd = (spec.durationMs - spec.releaseMs).coerceAtLeast(spec.attackMs + spec.decayMs)
        return when {
            ageMs < spec.attackMs -> {
                if (spec.attackMs > 0) ageMs.toFloat() / spec.attackMs else 1.0f
            }
            ageMs < spec.attackMs + spec.decayMs -> {
                val progress = (ageMs - spec.attackMs).toFloat() / spec.decayMs.coerceAtLeast(1)
                1.0f - (1.0f - spec.sustainLevel) * progress
            }
            ageMs < sustainEnd -> spec.sustainLevel
            else -> {
                val progress = (ageMs - sustainEnd).toFloat() / spec.releaseMs.coerceAtLeast(1)
                (spec.sustainLevel * (1.0f - progress)).coerceAtLeast(0f)
            }
        }
    }

    /**
     * Generate a waveform sample for the given phase and type.
     */
    private fun generateWaveform(phase: Double, t: Float, spec: ToneSpec): Float {
        return when (spec.type) {
            "square" -> if (phase < Math.PI) 1.0f else -1.0f
            "pulse25" -> {
                // Pulse Width Modulation: sweeps duty cycle between 13% and 37% at 1.8Hz
                val dutyCycle = 0.25f + 0.12f * sin(2.0 * Math.PI * 1.8 * t).toFloat()
                if (phase < 2.0 * Math.PI * dutyCycle) 1.0f else -1.0f
            }
            "triangle" -> {
                val p = phase / (2.0 * Math.PI)
                when {
                    p < 0.25 -> (4.0 * p).toFloat()
                    p < 0.75 -> (2.0 - 4.0 * p).toFloat()
                    else -> (4.0 * p - 4.0).toFloat()
                }
            }
            "noise" -> (Math.random().toFloat() * 2f - 1f)
            "noise-metallic" -> {
                // Sample-and-hold noise clocked at spec.freq → tonal metallic timbre
                val step = (spec.freq * t).toInt()
                val hash = (step * 1103515245 + 12345)
                (hash and 0x7FFF).toFloat() / 16384f - 1f
            }
            else -> sin(phase).toFloat()
        }
    }

    /** Soft-clip limiter to prevent harsh clipping with 4-channel mixes. */
    private fun softClip(x: Float): Float = x / (1f + abs(x)) * 1.5f

    private fun playTones(specs: List<ToneSpec>) {
        if (specs.isEmpty()) return
        val sampleRate = 44100
        val maxDurationMs = specs.maxOf { it.startMs + it.durationMs }
        val numSamples = (sampleRate * maxDurationMs / 1000)
        val buffer = FloatArray(numSamples)

        for (spec in specs) {
            val startSample = sampleRate * spec.startMs / 1000
            val durationSamples = sampleRate * spec.durationMs / 1000
            val endSample = minOf(numSamples, startSample + durationSamples)

            var phase = 0.0
            for (i in startSample until endSample) {
                val t = (i - startSample).toFloat() / sampleRate
                
                // Add vibrato: periodic pitch modulation for lead and bass channels
                val hasVibrato = spec.type == "pulse25" || spec.type == "square" || spec.type == "triangle"
                val freq = if (hasVibrato && spec.freq > 0f) {
                    val vibratoDepth = 0.007f // depth (~12 cents)
                    val vibratoRate = 6.0f   // rate in Hz
                    spec.freq * (1.0f + vibratoDepth * sin(2.0 * Math.PI * vibratoRate * t).toFloat())
                } else {
                    spec.freq
                }

                val phaseStep = 2.0 * Math.PI * freq / sampleRate
                phase = (phase + phaseStep) % (2.0 * Math.PI)
                var sample = generateWaveform(phase, t, spec)

                val ageMs = (i - startSample) * 1000 / sampleRate
                val envelope = computeEnvelope(ageMs, spec)
                sample *= envelope * spec.volume
                buffer[i] += sample
            }
        }

        val shortBuffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val mixed = buffer[i] * 2.8f
            shortBuffer[i] = (softClip(mixed) * 21845).toInt().coerceIn(-32768, 32767).toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(shortBuffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(shortBuffer, 0, shortBuffer.size)
            track.play()
            activeTrack = track
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun playOrientalPreview(context: Context, volume: Float) {
        try {
            mediaPlayer = MediaPlayer().apply {
                // THE FIX: Route MediaPlayer through the ALARM channel so it doesn't get muted!
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = context.assets.openFd("public/sounds/oriental_alarm.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setVolume(volume, volume)
                prepare()
                start()
                afd.close() // Safer to close AFTER prepare
            }
            val runnable = Runnable { stop() }
            stopRunnable = runnable
            handler.postDelayed(runnable, 3500)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun playGentleReminder(context: Context, soundKey: String, volume: Float) {
        stop()
        when (soundKey) {
            "oriental" -> {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        val afd = context.assets.openFd("public/sounds/oriental_alarm.mp3")
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        setVolume(volume, volume)
                        prepare()
                        start()
                        afd.close()
                    }
                    val runnable = Runnable { stop() }
                    stopRunnable = runnable
                    handler.postDelayed(runnable, 1000)
                } catch (e: Exception) { e.printStackTrace() }
            }
            "synth-bad-apple", "synth-senbonzakura" -> {
                val allNotes = SoundMelodies.getMelody(soundKey, volume, false)
                val previewNotes = allNotes.filter { it.startMs < 1500 }
                playTones(previewNotes)
            }
            "synth-chime", "synth-victory" -> {
                val melody = SoundMelodies.getMelody(soundKey, volume, false).take(8)
                val bass = SoundMelodies.getMelody(soundKey, volume, true).take(8)
                playTones(melody + bass)
            }
        }
    }

    fun playAlarm(context: Context, soundKey: String, volume: Float) {
        stop()
        when (soundKey) {
            "oriental" -> {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        val afd = context.assets.openFd("public/sounds/oriental_alarm.mp3")
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        setVolume(volume, volume)
                        isLooping = true
                        prepare()
                        start()
                        afd.close()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            "synth-bad-apple", "synth-senbonzakura" -> {
                val allNotes = SoundMelodies.getMelody(soundKey, volume, false)
                playTonesLooping(allNotes)
            }
            "synth-chime", "synth-victory" -> {
                val melody = SoundMelodies.getMelody(soundKey, volume, false)
                val bass = SoundMelodies.getMelody(soundKey, volume, true)
                playTonesLooping(melody + bass)
            }
        }
    }

    private fun playTonesLooping(specs: List<ToneSpec>) {
        if (specs.isEmpty()) return
        val sampleRate = 44100
        val maxDurationMs = specs.maxOf { it.startMs + it.durationMs }
        val numSamples = (sampleRate * maxDurationMs / 1000)
        val buffer = FloatArray(numSamples)

        for (spec in specs) {
            val startSample = sampleRate * spec.startMs / 1000
            val durationSamples = sampleRate * spec.durationMs / 1000
            val endSample = minOf(numSamples, startSample + durationSamples)

            var phase = 0.0
            for (i in startSample until endSample) {
                val t = (i - startSample).toFloat() / sampleRate
                
                // Add vibrato: periodic pitch modulation for lead and bass channels
                val hasVibrato = spec.type == "pulse25" || spec.type == "square" || spec.type == "triangle"
                val freq = if (hasVibrato && spec.freq > 0f) {
                    val vibratoDepth = 0.007f // depth (~12 cents)
                    val vibratoRate = 6.0f   // rate in Hz
                    spec.freq * (1.0f + vibratoDepth * sin(2.0 * Math.PI * vibratoRate * t).toFloat())
                } else {
                    spec.freq
                }

                val phaseStep = 2.0 * Math.PI * freq / sampleRate
                phase = (phase + phaseStep) % (2.0 * Math.PI)
                var sample = generateWaveform(phase, t, spec)

                val ageMs = (i - startSample) * 1000 / sampleRate
                val envelope = computeEnvelope(ageMs, spec)
                sample *= envelope * spec.volume
                buffer[i] += sample
            }
        }

        val shortBuffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val mixed = buffer[i] * 2.8f
            shortBuffer[i] = (softClip(mixed) * 21845).toInt().coerceIn(-32768, 32767).toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(shortBuffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(shortBuffer, 0, shortBuffer.size)
            track.setLoopPoints(0, shortBuffer.size - 1, -1)
            track.play()
            activeTrack = track
        } catch (e: Exception) { e.printStackTrace() }
    }
}