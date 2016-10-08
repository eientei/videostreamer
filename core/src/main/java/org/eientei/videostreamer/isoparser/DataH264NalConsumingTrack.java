package org.eientei.videostreamer.isoparser;

import org.mp4parser.streaming.StreamingSample;
import org.mp4parser.streaming.input.StreamingSampleImpl;
import org.mp4parser.streaming.input.h264.H264NalConsumingTrack;
import org.mp4parser.streaming.input.h264.H264NalUnitHeader;
import org.mp4parser.streaming.input.h264.spspps.SliceHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class DataH264NalConsumingTrack extends H264NalConsumingTrack {
    private int desiredFramerate;

    @Override
    public void consumeNal(ByteBuffer nal) throws IOException {
        super.consumeNal(nal);
        setTimescale(desiredFramerate);
        setFrametick(1);
    }

    @Override
    protected StreamingSample createSample(List<ByteBuffer> nals, SliceHeader sliceHeader, H264NalUnitHeader nu) throws IOException {
        configure();
        StreamingSample ss = new StreamingSampleImpl(
                nals,
                1);
        ss.addSampleExtension(createSampleFlagsSampleExtension(nu, sliceHeader));
        //ss.addSampleExtension(createPictureOrderCountType0SampleExtension(sliceHeader));
        return ss;
    }

    public void setDesiredFramerate(int desiredFramerate) {
        this.desiredFramerate = desiredFramerate;
    }

    @Override
    public long getTimescale() {
        return desiredFramerate;
    }
}
