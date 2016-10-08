package org.eientei.videostreamer.mp4;

/**
 * Created by Alexander Tumin on 2016-10-07
 */
public class MetaData {
    private final int framerate;
    private final int width;
    private final int height;
    private final int firstTime;
    private byte[] avcc;
    private int videoCodecId;
    private int audioCodecId;
    private int audioChannels;
    private int audioSampleSize;
    private int audioSampleRate;
    private byte[] audioDsi;
    private int timeScale;
    private int frameTick;

    public MetaData(int framerate, int width, int height, int firstTime) {
        this.framerate = framerate;
        this.width = width;
        this.height = height;
        this.firstTime = firstTime;
    }

    public int getFramerate() {
        return framerate;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFirstTime() {
        return firstTime;
    }

    public byte[] getAvcc() {
        return avcc;
    }

    public void setAvcc(byte[] avcc) {
        this.avcc = avcc;
    }

    public void setVideoCodecId(int videoCodecId) {
        this.videoCodecId = videoCodecId;
    }

    public int getVideoCodecId() {
        return videoCodecId;
    }

    public void setAudioCodecId(int audioCodecId) {
        this.audioCodecId = audioCodecId;
    }

    public int getAudioCodecId() {
        return audioCodecId;
    }

    public void setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public void setAudioSampleSize(int audioSampleSize) {
        this.audioSampleSize = audioSampleSize;
    }

    public int getAudioSampleSize() {
        return audioSampleSize;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioDsi(byte[] audioDsi) {
        this.audioDsi = audioDsi;
    }

    public byte[] getAudioDsi() {
        return audioDsi;
    }

    public void setTimeScale(int timeScale) {
        this.timeScale = timeScale;
    }

    public int getTimeScale() {
        return framerate;
        //return timeScale;
    }

    public int getFrameTick() {
        return 1;
        //return frameTick;
    }

    public void setFrameTick(int frameTick) {
        this.frameTick = frameTick;
    }
}
