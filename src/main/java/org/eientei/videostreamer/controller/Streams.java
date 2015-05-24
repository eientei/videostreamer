package org.eientei.videostreamer.controller;

import org.eientei.videostreamer.config.security.VideostreamerUser;
import org.eientei.videostreamer.controller.exception.HttpNotFoundException;
import org.eientei.videostreamer.controller.model.ChatRoom;
import org.eientei.videostreamer.dto.StreamDTO;
import org.eientei.videostreamer.dto.StreamOwnDTO;
import org.eientei.videostreamer.orm.entity.Stream;
import org.eientei.videostreamer.orm.error.AlreadyExists;
import org.eientei.videostreamer.orm.error.InvalidName;
import org.eientei.videostreamer.orm.error.TooManyStreams;
import org.eientei.videostreamer.orm.service.StreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 11:08
 */
@RestController
@RequestMapping("streams")
public class Streams {
    @Autowired
    private StreamService streamService;

    @Autowired
    private Chat chat;

    @RequestMapping("running")
    public List<StreamDTO> running() {
        List<StreamDTO> dtos = new ArrayList<StreamDTO>();
        for (Stream stream : streamService.running()) {
            dtos.add(new StreamDTO(stream, chat.getRoom(stream.getApp(), stream.getName())));
        }
        return dtos;
    }

    @RequestMapping("stream/{app}/{name}")
    public StreamDTO stream(@PathVariable String app, @PathVariable String name) {
        Stream stream = streamService.getStream(app, name);
        if (stream == null) {
            throw new HttpNotFoundException();
        }
        return new StreamDTO(stream, chat.getRoom(app, name));
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("mine")
    public List<StreamDTO> mine(@AuthenticationPrincipal VideostreamerUser user) {
        List<StreamDTO> dtos = new ArrayList<StreamDTO>();
        for (Stream stream : streamService.belongs(user.getEntity())) {
            dtos.add(new StreamOwnDTO(stream));
        }
        return dtos;
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("allocate")
    public StreamDTO allocate(@AuthenticationPrincipal VideostreamerUser user) throws TooManyStreams {
        Stream stream = streamService.allocateStream(user.getEntity());
        return new StreamOwnDTO(stream);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("deallocate/{app}/{name}")
    public void deallocate(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name) {
        streamService.deallocate(user.getEntity(), app, name);
        ChatRoom room = chat.getRoom(app, name);
        if (room != null) {
            room.destroy();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("rename/{app}/{name}")
    public void rename(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody String newname) throws AlreadyExists, InvalidName {
        if (newname == null || newname.length() < 3 || newname.contains("/")) {
            throw new InvalidName();
        }
        streamService.updateStreamName(user.getEntity(), streamService.forUser(user.getEntity(), app, name), newname);
        ChatRoom room = chat.getRoom(app, name);
        if (room != null) {
            streamService.refresh(room.getStream());
        }
        chat.migrateRoom(app, name, newname);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("topic/{app}/{name}")
    public void topic(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody String newtopic) {
        streamService.updateStreamTopic(streamService.forUser(user.getEntity(), app, name), newtopic);
        ChatRoom room = chat.getRoom(app, name);
        streamService.refresh(chat.getRoom(app, name).getStream());
        room.updateTopic();
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("screensaver/{app}/{name}")
    public void screensaver(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody String newurl) {
        streamService.updateStreamImage(streamService.forUser(user.getEntity(), app, name), newurl);
        ChatRoom room = chat.getRoom(app, name);
        if (room != null) {
            streamService.refresh(room.getStream());
            room.updateImage();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("restricted/{app}/{name}")
    public void restricted(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody Boolean restricted) {
        streamService.updateStreamPrivate(streamService.forUser(user.getEntity(), app, name), restricted);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping("gentoken/{app}/{name}")
    public String gentoken(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name) {
        return "\"" + streamService.generateStreamToken(streamService.forUser(user.getEntity(), app, name)) + "\"";
    }

}
