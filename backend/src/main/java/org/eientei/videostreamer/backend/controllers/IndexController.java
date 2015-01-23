package org.eientei.videostreamer.backend.controllers;

import org.eientei.videostreamer.backend.chat.ChatRoom;
import org.eientei.videostreamer.backend.orm.entity.Stream;
import org.eientei.videostreamer.backend.orm.service.StreamService;
import org.eientei.videostreamer.backend.pojo.stream.StreamIndex;
import org.eientei.videostreamer.backend.pojo.stream.StreamIndexItem;
import org.eientei.videostreamer.backend.utils.VideostreamerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-21
 * Time: 00:45
 */
@RequestMapping("index")
@Controller
public class IndexController {
    @Autowired
    private StreamService streamService;

    @Autowired
    private ChatController chatController;

    @RequestMapping(value = "list", method = RequestMethod.POST)
    @ResponseBody
    public Object list() {
        StreamIndex streamIndex = new StreamIndex();
        for (Stream stream : streamService.getActiveStreams()) {
            if (stream.isRestricted()) {
                continue;
            }

            StreamIndexItem item = new StreamIndexItem();
            item.setApp(stream.getApp());
            item.setName(stream.getName());
            item.setTopic(stream.getTopic());
            item.setAuthorName(stream.getAuthor().getName());
            item.setAuthorHash(VideostreamerUtils.determineUserHash(stream.getAuthor(), (String)null));
            if (stream.getSince() != null) {
                item.setSince(stream.getSince().getTime());
            }
            streamIndex.getItems().add(item);

            ChatRoom room = chatController.getRoom(stream.getApp(), stream.getName());
            if (room != null) {
                item.getOnlines().addAll(room.getOnlines());
            }
        }
        return streamIndex;
    }
}
