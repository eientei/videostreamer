package org.eientei.video.backend.controller;


import com.google.common.base.Strings;
import org.eientei.video.backend.config.security.VideostreamerUser;
import org.eientei.video.backend.controller.exception.HttpNotFoundException;
import org.eientei.video.backend.controller.model.ChatRoom;
import org.eientei.video.backend.dto.StreamDTO;
import org.eientei.video.backend.dto.StreamOwnDTO;
import org.eientei.video.backend.orm.entity.Stream;
import org.eientei.video.backend.orm.error.AlreadyExists;
import org.eientei.video.backend.orm.error.InvalidName;
import org.eientei.video.backend.orm.error.TooManyStreams;
import org.eientei.video.backend.orm.service.StreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping(value = "running", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StreamDTO> running() {
        List<StreamDTO> dtos = new ArrayList<StreamDTO>();
        for (Stream stream : streamService.running()) {
            dtos.add(new StreamDTO(stream, chat.getRoom(stream.getApp(), stream.getName())));
        }
        return dtos;
    }

    @RequestMapping(value = "stream/{app}/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public StreamDTO stream(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name) {
        Stream stream = streamService.getStream(app, name);
        if (stream == null) {
            throw new HttpNotFoundException();
        }
        if (stream.getAuthor().getId().equals(user.getEntity().getId())) {
            return new StreamOwnDTO(stream, chat.getRoom(app, name));
        }
        return new StreamDTO(stream, chat.getRoom(app, name));
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "mine", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StreamOwnDTO> mine(@AuthenticationPrincipal VideostreamerUser user) {
        List<StreamOwnDTO> dtos = new ArrayList<StreamOwnDTO>();
        for (Stream stream : streamService.belongs(user.getEntity())) {
            dtos.add(new StreamOwnDTO(stream));
        }
        return dtos;
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "allocate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public StreamOwnDTO allocate(@AuthenticationPrincipal VideostreamerUser user) throws TooManyStreams {
        Stream stream = streamService.allocateStream(user.getEntity());
        return new StreamOwnDTO(stream);
    }


    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "deallocate/{app}/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void deallocate(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name) {
        streamService.deallocate(user.getEntity(), app, name);
        ChatRoom room = chat.getRoom(app, name);
        if (room != null) {
            room.destroy();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "rename/{app}/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void rename(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody String newname) throws AlreadyExists, InvalidName {
        if (newname == null || newname.length() < 3 || newname.length() > 30 || newname.contains("/")) {
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
    @RequestMapping(value = "topic/{app}/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void topic(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody String newtopic) {
        if (newtopic != null && newtopic.trim().isEmpty()) {
            newtopic = null;
        } else if (newtopic != null) {
            newtopic = newtopic.trim();
        }
        Stream stream = streamService.forUser(user.getEntity(), app, name);
        if (Strings.nullToEmpty(newtopic).equals(Strings.nullToEmpty(stream.getTopic()))) {
            return;
        }
        streamService.updateStreamTopic(stream, newtopic);
        ChatRoom room = chat.getRoom(app, name);
        if (room != null) {
            streamService.refresh(chat.getRoom(app, name).getStream());
            room.updateTopic();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "image/{app}/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void image(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody String newimage) {
        streamService.updateStreamImage(streamService.forUser(user.getEntity(), app, name), newimage);
        ChatRoom room = chat.getRoom(app, name);
        if (room != null) {
            streamService.refresh(room.getStream());
            room.updateImage();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "restricted/{app}/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void restricted(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name, @RequestBody String restricted) {
        streamService.updateStreamPrivate(streamService.forUser(user.getEntity(), app, name), Boolean.parseBoolean(restricted));
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "gentoken/{app}/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String gentoken(@AuthenticationPrincipal VideostreamerUser user, @PathVariable String app, @PathVariable String name) {
        return "\"" + streamService.generateStreamToken(streamService.forUser(user.getEntity(), app, name)) + "\"";
    }
}
