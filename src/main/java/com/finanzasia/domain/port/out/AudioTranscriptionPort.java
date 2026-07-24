package com.finanzasia.domain.port.out;

/**
 * Concrete implementation uses Groq Whisper.
 */
public interface AudioTranscriptionPort {

    /**
     * @param filename original filename including extension (e.g. "voice.m4a");
     *                   used by the remote API to infer the audio codec
     * @return the transcribed text; never null, may be blank if no speech was detected
     * @throws com.finanzasia.domain.exceptions.AIExtractionException if the transcription service fails
     */
    String transcribe(byte[] audioBytes, String filename);
}
