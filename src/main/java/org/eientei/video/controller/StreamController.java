package org.eientei.video.controller;

import org.eientei.video.orm.entity.Stream;
import org.eientei.video.orm.service.StreamService;
import org.eientei.video.security.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-13
 * Time: 15:47
 */
@Controller
@RequestMapping("")
public class StreamController extends BaseController {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class AppNameNotFound extends Exception { }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class TokenNotFound extends Exception { }

    @Autowired
    private ChatController chatController;

    @Autowired
    private StreamService streamService;

    @RequestMapping("")
    public String index(Model model) {
        List<Stream> streams = streamService.getActiveStreams();
        for (Stream stream: streams) {
            stream.setChatRoom(chatController.getRoom(stream.getApp() + '/' + stream.getName()));
        }
        model.addAttribute("activeStreams", streams);
        return "index";
    }

    @RequestMapping("error/{errorCode}")
    public String error(Model model, @PathVariable int errorCode) {
        model.addAttribute("errorCode", errorCode);
        switch (errorCode) {
            case 404:
                model.addAttribute("errorMessage", "Not found");
                break;
            case 403:
                model.addAttribute("errorMessage", "Forbidden");
                break;
        }
        return "error";
    }

    @RequestMapping("info")
    public String info() {
        return "info";
    }

    @RequestMapping(value = "check_access", method = RequestMethod.POST)
    public RedirectView checkAccess(Model model, @RequestParam("name") String name, @RequestParam("addr") String addr) throws TokenNotFound {
        Stream stream = streamService.getStreamByToken(name);
        if (stream != null) {
            stream.setRemote(addr);
            streamService.saveStream(stream);
            model.asMap().clear();
            return new RedirectView(stream.getName(), false, false, false);
        }
        throw new TokenNotFound();
    }

    @RequestMapping(value = "finish_stream", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void finishStream(@RequestParam("name") String name) throws TokenNotFound {
        Stream stream = streamService.getStreamByToken(name);
        if (stream != null) {
            stream.setRemote(null);
            streamService.saveStream(stream);
            return;
        }
        throw new TokenNotFound();
    }

    @RequestMapping(value = "{app:(?!js$|css$|swf$|chat$).*}/{name}/title", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void appName(@RequestParam("title") String title) {
        AppUserDetails appUserDetails = (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        streamService.updateStreamTopic(title, appUserDetails.getDataUser());
    }

    @RequestMapping(value = "{app:(?!js$|css$|swf$|chat$).*}/{name}")
    public String appName(Model model, @PathVariable String app, @PathVariable String name,
                          @RequestParam(defaultValue = "false") boolean novideo,
                          @RequestParam(defaultValue = "false") boolean nochat,
                          @RequestParam(defaultValue = "1.0") Double buffer,
                          @RequestParam(defaultValue = "rtmp") String player
                          ) throws AppNameNotFound {
        Stream stream = streamService.getStream(app, name);
        if (stream == null) {
            throw new AppNameNotFound();
        }
        model.addAttribute("stream", stream);
        model.addAttribute("novideo", novideo);
        model.addAttribute("nochat", nochat);
        model.addAttribute("buffer", buffer);
        model.addAttribute("player", player);
        model.addAttribute("app", app);
        model.addAttribute("name", name);
        model.addAttribute("showtitle", true);
        AppUserDetails appUserDetails = (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (stream.getAuthor().getId() == appUserDetails.getDataUser().getId()) {
            model.addAttribute("streameditable", true);
        } else {
            model.addAttribute("streameditable", false);
        }

        if (stream.getTopic() != null) {
            model.addAttribute("streamtitle", stream.getTopic());
        }
        return "playback";
    }
}
