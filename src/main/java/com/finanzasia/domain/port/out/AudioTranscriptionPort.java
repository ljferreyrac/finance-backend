package com.finanzasia.domain.port.out;

/**
 * Output port for audio-to-text transcription.
 * The infrastructure layer provides the concrete implementation (Groq Whisper).
 */
public interface AudioTranscriptionPort {

    /**
     * Transcribes raw audio bytes to plain text.
     *
     * @param audioBytes the raw audio file content
     * @param filename   original filename including extension (e.g. "voice.m4a");
     *                   used by the remote API to infer the audio codec
     * @return the transcribed text; never null, may be blank if no speech was detected
     * @throws com.finanzasia.domain.exceptions.AIExtractionException if the transcription service fails
     */
    String transcribe(byte[] audioBytes, String filename);
}
