package org.eientei.videostreamer.impl.core;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

/**
 * Created by Alexander Tumin on 2016-11-02
 */
public class Frame extends AbstractReferenceCounted {
    private final SampleList audioList;
    private final SampleList videoList;
    private final int audioSequence;
    private final int videoSequence;
    private final int sequence;

    public Frame(SampleList audioList, SampleList videoList) {
        this.audioList = audioList;
        this.videoList = videoList;

        this.audioSequence = audioList.getSamples().size();
        this.videoSequence = videoList.getSamples().size();

        sequence = 1;
    }

    public Frame(Frame previous, SampleList audioList, SampleList videoList) {
        this.audioList = audioList;
        this.videoList = videoList;

        // potential code WTF: this is FIELDS, not METHODS, as METHODS are COMPUTED: (totalSeq - currentSeq)
        audioSequence = previous.audioSequence+audioList.getSamples().size();
        videoSequence = previous.videoSequence+videoList.getSamples().size();

        sequence = previous.getSequence() + 1;
        previous.release();
    }

    public boolean isKey() {
        return videoList.isKey();
    }

    public SampleList getAudioList() {
        return audioList;
    }

    public SampleList getVideoList() {
        return videoList;
    }

    public int getAudioSequence() {
        return audioSequence - getAudioAdvance();
    }

    public int getVideoSequence() {
        return videoSequence - getVideoAdvance();
    }

    public int getAudioAdvance() {
        return audioList.getSamples().size();
    }

    public int getVideoAdvance() {
        return videoList.getSamples().size();
    }

    public int getSequence() {
        return sequence;
    }

    @Override
    protected void deallocate() {
        audioList.release();
        videoList.release();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}
