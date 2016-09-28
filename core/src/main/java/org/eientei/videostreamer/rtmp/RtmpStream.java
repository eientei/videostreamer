package org.eientei.videostreamer.rtmp;

import org.eientei.videostreamer.rtmp.message.RtmpAmfMetaMessage;
import org.eientei.videostreamer.rtmp.message.RtmpVideoMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Alexander Tumin on 2016-09-28
 */
public class RtmpStream {
    private final RtmpServerContext context;
    private List<RtmpStreamClient> clients = new CopyOnWriteArrayList<>();
    private RtmpClientContext source;
    private RtmpStreamMetadata metadata;
    private byte[] keyframe;

    public RtmpStream(RtmpServerContext serverContext) {
        this.context = serverContext;
    }

    public RtmpClientContext getSource() {
        return source;
    }

    public void setSource(RtmpClientContext source) {
        if (source == null) {
            // remove bootstrap
        }
        this.source = source;
    }

    public List<RtmpStreamClient> getClients() {
        return clients;
    }

    public void bootstrap(RtmpStreamClient client) {
        client.accept(metadata.getMessage());
        client.accept(new RtmpVideoMessage(keyframe));
    }

    @SuppressWarnings("unchecked")
    public void broadcast(RtmpMessage message) {
        if (message instanceof RtmpAmfMetaMessage) {
            Map<String, Object> data = (Map<String, Object>) ((RtmpAmfMetaMessage) message).getValues().get(2);
            metadata = new RtmpStreamMetadata(data);
            message = metadata.getMessage();
        } else if (message instanceof RtmpVideoMessage) {
            byte[] data = ((RtmpVideoMessage) message).getData();
            if (data[1] == 0x00) {
                keyframe = data;
            }
        }

        for (RtmpStreamClient client : clients) {
            client.accept(message);
        }
    }
}
