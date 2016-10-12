package org.eientei.videostreamer.isoparser;

import org.eientei.videostreamer.h264.SliceNalUnit;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.MovieFragmentBox;
import org.mp4parser.boxes.iso14496.part12.MovieFragmentHeaderBox;
import org.mp4parser.boxes.iso14496.part12.TrackFragmentBox;
import org.mp4parser.boxes.iso14496.part12.TrackRunBox;
import org.mp4parser.streaming.StreamingSample;
import org.mp4parser.streaming.StreamingTrack;
import org.mp4parser.streaming.output.SampleSink;
import org.mp4parser.streaming.output.mp4.FragmentedMp4Writer;
import org.mp4parser.tools.IsoTypeWriter;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.List;

/**
 * Created by Alexander Tumin on 2016-10-08
 */
public class DelegatingFragmentedMp4Writer extends FragmentedMp4Writer implements SampleSink {
    private final List<StreamingTrack> source;
    private final WebSocketSession session;
    private final WebsocketWritableByteChannel sink;
    private boolean first = true;

    public DelegatingFragmentedMp4Writer(List<StreamingTrack> source, WebSocketSession session) throws IOException {
        super(source, new WebsocketWritableByteChannel(session));
        this.source = source;
        this.session = session;
        this.sink = (WebsocketWritableByteChannel) super.sink;
    }

    @Override
    public void close() throws IOException {
        sink.close();
    }

    @Override
    public void acceptSample(StreamingSample streamingSample, StreamingTrack streamingTrack) throws IOException {
        if (first) {
            first = false;
            Box ftyp = createFtyp();
            Box moov = createMoov();

            ftyp.getBox(sink);
            moov.getBox(sink);
            sink.flush();
        }

        ByteBuffer sampleBuffer = (ByteBuffer) streamingSample.getContent().rewind();
        while (sampleBuffer.remaining() > 0) {
            int len = streamingSample.getContent().getInt(streamingSample.getContent().position())+4;
            SliceNalUnit slice = new SliceNalUnit((ByteBuffer) sampleBuffer.slice().position(4));
            if (slice.sliceType == 0) {
                sampleBuffer.position(sampleBuffer.position()+len);
                continue;
            }

            MovieFragmentBox moof = (MovieFragmentBox) createMoof(streamingTrack, Collections.singletonList(streamingSample));
            Box mdat = createMdat((ByteBuffer) sampleBuffer.slice().limit(len));
            moof.getBox(sink);
            mdat.getBox(sink);
            sink.flush();
            sampleBuffer.position(sampleBuffer.position()+len);
        }
    }

    private Box createMoof(StreamingTrack streamingTrack, List<StreamingSample> samples) {
        MovieFragmentBox moof = new MovieFragmentBox();
        createMfhd(sequenceNumber, moof);
        createTraf(streamingTrack, moof, samples);
        TrackRunBox firstTrun = moof.getTrackRunBoxes().get(0);
        firstTrun.setDataOffset(1); // dummy to make size correct
        firstTrun.setDataOffset((int) (8 + moof.getSize())); // mdat header + moof size
        return moof;
    }

    private Box createMdat(final ByteBuffer sampleData) {

        return new Box() {
            public String getType() {
                return "mdat";
            }

            public long getSize() {
                return sampleData.remaining() + 8;
            }

            public void getBox(WritableByteChannel writableByteChannel) throws IOException {
                long l = 8;
                l += sampleData.remaining();
                ByteBuffer bb = ByteBuffer.allocate(8);
                IsoTypeWriter.writeUInt32(bb, l);
                bb.put(IsoFile.fourCCtoBytes(getType()));
                writableByteChannel.write((ByteBuffer) bb.rewind());

                writableByteChannel.write(sampleData);
            }

        };
    }

    private void createMfhd(long sequenceNumber, MovieFragmentBox moof) {
        MovieFragmentHeaderBox mfhd = new MovieFragmentHeaderBox();
        mfhd.setSequenceNumber(sequenceNumber);
        moof.addBox(mfhd);
    }

    private void createTraf(StreamingTrack streamingTrack, MovieFragmentBox moof, List<StreamingSample> samples) {
        TrackFragmentBox traf = new TrackFragmentBox();
        moof.addBox(traf);
        createTfhd(streamingTrack, traf);
        createTfdt(streamingTrack, traf);
        createTrun(streamingTrack, traf, samples);
    }

    protected void createTrun(StreamingTrack streamingTrack, TrackFragmentBox parent, ByteBuffer sample) {
        TrackRunBox trun = new TrackRunBox();
        trun.setVersion(0);
        trun.setDataOffsetPresent(true);
        trun.setSampleDurationPresent(true);
        trun.setSampleSizePresent(true);
        TrackRunBox.Entry entry = new TrackRunBox.Entry();
        entry.setSampleSize(sample.remaining());
        entry.setSampleDuration(1);
        trun.setEntries(Collections.singletonList(entry));
        parent.addBox(trun);



        //trun.setSampleCompositionTimeOffsetPresent(streamingTrack.getTrackExtension(CompositionTimeTrackExtension.class) != null);

//        DefaultSampleFlagsTrackExtension defaultSampleFlagsTrackExtension = streamingTrack.getTrackExtension(DefaultSampleFlagsTrackExtension.class);
//        trun.setSampleFlagsPresent(defaultSampleFlagsTrackExtension == null);
        /*if (defaultSampleFlagsTrackExtension == null) {
            SampleFlagsSampleExtension sampleFlagsSampleExtension = streamingSample.getSampleExtension(SampleFlagsSampleExtension.class);
            assert sampleFlagsSampleExtension != null : "SampleDependencySampleExtension missing even though SampleDependencyTrackExtension was present";
            SampleFlags sflags = new SampleFlags();
            sflags.setIsLeading(sampleFlagsSampleExtension.getIsLeading());
            sflags.setSampleIsDependedOn(sampleFlagsSampleExtension.getSampleIsDependedOn());
            sflags.setSampleDependsOn(sampleFlagsSampleExtension.getSampleDependsOn());
            sflags.setSampleHasRedundancy(sampleFlagsSampleExtension.getSampleHasRedundancy());
            sflags.setSampleIsDifferenceSample(sampleFlagsSampleExtension.isSampleIsNonSyncSample());
            sflags.setSamplePaddingValue(sampleFlagsSampleExtension.getSamplePaddingValue());
            sflags.setSampleDegradationPriority(sampleFlagsSampleExtension.getSampleDegradationPriority());

            entry.setSampleFlags(sflags);
        }*/

        /*if (trun.isSampleCompositionTimeOffsetPresent()) {
            CompositionTimeSampleExtension compositionTimeSampleExtension = streamingSample.getSampleExtension(CompositionTimeSampleExtension.class);
            assert compositionTimeSampleExtension != null : "CompositionTimeSampleExtension missing even though CompositionTimeTrackExtension was present";
            entry.setSampleCompositionTimeOffset(l2i(compositionTimeSampleExtension.getCompositionTimeOffset()));
        }*/
    }
}
