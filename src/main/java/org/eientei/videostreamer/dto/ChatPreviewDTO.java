package org.eientei.videostreamer.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-12
 * Time: 08:55
 */
public class ChatPreviewDTO {
    private ChatMessageDTO preview;
    private Long id;

    public ChatMessageDTO getPreview() {
        return preview;
    }

    public void setPreview(ChatMessageDTO preview) {
        this.preview = preview;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
