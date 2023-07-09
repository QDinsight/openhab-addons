/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.voice.whisperstt.internal.utils;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.givimad.libfvadjni.VoiceActivityDetector;

/**
 * The {@link VAD} class is a voice activity detector implementation over libfvad-jni.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class VAD implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(VAD.class);
    private final VoiceActivityDetector libfvad;
    private final short[] stepSamples;
    private final int totalPartialDetections;
    private final int detectionThreshold;

    /**
     *
     * @param mode desired vad mode.
     * @param sampleRate audio sample rate.
     * @param frameSize detector input frame size.
     * @param stepMs detector partial step ms.
     * @param sensitivity detector sensitivity percent in range 0 - 1.
     * @throws IOException
     */
    public VAD(VoiceActivityDetector.Mode mode, int sampleRate, int frameSize, int stepMs, float sensitivity)
            throws IOException {
        this.libfvad = VoiceActivityDetector.newInstance();
        this.libfvad.setMode(mode);
        this.libfvad.setSampleRate(VoiceActivityDetector.SampleRate.fromValue(sampleRate));
        this.stepSamples = new short[sampleRate / 1000 * stepMs];
        this.totalPartialDetections = (frameSize / stepSamples.length);
        this.detectionThreshold = (int) ((((float) totalPartialDetections) / 100f) * (sensitivity * 100));
    }

    public boolean isVoice(short[] samples) throws IOException {
        int partialVADCounter = 0;
        for (int i = 0; i < totalPartialDetections; i++) {
            System.arraycopy(samples, i * stepSamples.length, stepSamples, 0, stepSamples.length);
            if (libfvad.process(stepSamples, stepSamples.length)) {
                partialVADCounter++;
            }
        }
        logger.debug("VAD: {}/{} - required: {}", partialVADCounter, totalPartialDetections, detectionThreshold);
        return partialVADCounter >= detectionThreshold;
    }

    @Override
    public void close() {
        libfvad.close();
    }
}
