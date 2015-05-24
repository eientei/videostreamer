package org.eientei.videostreamer.dto;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-23
 * Time: 11:46
 */
public class ChatMigrateDTO {
    private String newname;

    public ChatMigrateDTO() {
    }

    public ChatMigrateDTO(String newname) {
        this.newname = newname;
    }

    public String getNewname() {
        return newname;
    }

    public void setNewname(String newname) {
        this.newname = newname;
    }
}
