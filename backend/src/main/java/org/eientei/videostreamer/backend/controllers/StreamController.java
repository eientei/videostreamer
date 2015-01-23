package org.eientei.videostreamer.backend.controllers;

import org.eientei.videostreamer.backend.orm.entity.Stream;
import org.eientei.videostreamer.backend.orm.service.StreamService;
import org.eientei.videostreamer.backend.pojo.stream.*;
import org.eientei.videostreamer.backend.security.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-17
 * Time: 16:00
 */
@RequestMapping("stream")
@Controller
public class StreamController {
    @Autowired
    private StreamService streamService;

    @Autowired
    private ConfigBootstrap config;

    @Autowired
    private ChatController chatController;

    @RequestMapping(value = "own", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object own(@AuthenticationPrincipal AppUserDetails appUserDetails) {
        return makeOwnStreams(appUserDetails);
    }

    @RequestMapping(value = "updatekey", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object updateKey(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody StreamUpdateKey streamUpdateKey) {
        streamService.updateStreamKey(appUserDetails.getDataUser(), streamUpdateKey.getId());
        return makeOwnStreams(appUserDetails);
    }

    @RequestMapping(value = "updatename", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object updateName(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody StreamUpdateName streamUpdateName) {
        streamService.updateStreamName(appUserDetails.getDataUser(), streamUpdateName.getId(), streamUpdateName.getName());
        return makeOwnStreams(appUserDetails);
    }

    @RequestMapping(value = "updatetopic", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object updateTopic(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody StreamUpdateTopic streamUpdateTopic) {
        Stream stream = streamService.updateStreamTopic(appUserDetails.getDataUser(), streamUpdateTopic.getId(), streamUpdateTopic.getTopic());
        chatController.sendTopic(stream.getApp(), stream.getName());
        return makeOwnStreams(appUserDetails);
    }

    @RequestMapping(value = "updateimage", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object updateImage(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody StreamUpdateImage streamUpdateImage) {
        streamService.updateStreamImage(appUserDetails.getDataUser(), streamUpdateImage.getId(), streamUpdateImage.getImage());
        return makeOwnStreams(appUserDetails);
    }

    @RequestMapping(value = "updateprivate", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object updatePrivate(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody StreamUpdatePrivate streamUpdatePrivate) {
        streamService.updateStreamPrivate(appUserDetails.getDataUser(), streamUpdatePrivate.getId());
        return makeOwnStreams(appUserDetails);
    }


    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object delete(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody StreamDelete streamDelete) {
        streamService.deleteStream(appUserDetails.getDataUser(), streamDelete.getId());
        return makeOwnStreams(appUserDetails);
    }

    @RequestMapping(value = "bootstrap", method = RequestMethod.POST)
    @ResponseBody
    public Object bootstrap(@AuthenticationPrincipal AppUserDetails appUserDetails, @RequestBody StreamBootstrap bootstrap) {
        Stream stream = streamService.getStream(bootstrap.getApp(), bootstrap.getName());
        StreamBootstrapReply builder = new StreamBootstrapReply();
        if (stream != null) {
            builder.setOk(true);
            if (stream.getIdleImage() != null) {
                builder.setIdleImage(stream.getIdleImage());
            }
            if (stream.getTopic() != null) {
                builder.setTopic(stream.getTopic());
            }
            builder.setId(stream.getId());
            builder.setOwn(stream.getAuthor().getId() == appUserDetails.getDataUser().getId());
        } else {
            builder.setOk(false);
        }
        return builder;
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Object add(@AuthenticationPrincipal AppUserDetails appUserDetails) {
        if (streamService.getUserStreams(appUserDetails.getDataUser()).size() >= config.getMaxStreams()) {
            throw new StreamService.StreamExhaustion();
        }

        streamService.allocateStream(appUserDetails.getDataUser(), "live");
        return makeOwnStreams(appUserDetails);
    }

    private Object makeOwnStreams(AppUserDetails appUserDetails) {
        List<Stream> streams = streamService.getUserStreams(appUserDetails.getDataUser());
        StreamOwn streamOwn = new StreamOwn();

        for (Stream s : streams) {
            StreamOwnItem sb = new StreamOwnItem();
            sb.setId(s.getId());
            sb.setApp(s.getApp());
            sb.setName(s.getName());
            sb.setKey(s.getToken());
            if (s.getRemote() != null) {
                sb.setRemote(s.getRemote());
            }
            sb.setTopic(s.getTopic());
            sb.setRestricted(s.isRestricted());
            if (s.getIdleImage() != null) {
                sb.setImageUrl(s.getIdleImage());
            }
            streamOwn.getItems().add(sb);
        }

        return streamOwn;
    }
}
