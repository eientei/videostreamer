package org.eientei.videostreamer.backend.controllers;

import org.eientei.videostreamer.backend.orm.entity.Stream;
import org.eientei.videostreamer.backend.orm.service.StreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Date;

/**
 * User: iamtakingiteasy
 * Date: 2015-01-18
 * Time: 14:06
 */
@RequestMapping("nginx")
@Controller
public class NginxController {
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class TokenNotFound extends Exception { }

    @Autowired
    private StreamService streamService;

    @RequestMapping(value = "check_access", method = RequestMethod.POST)
    public RedirectView checkAccess(Model model, @RequestParam("name") String name, @RequestParam("addr") String addr) throws TokenNotFound {
        Stream stream = streamService.getStreamByToken(name);
        if (stream != null) {
            stream.setRemote(addr);
            stream.setSince(new Date());
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
            stream.setSince(null);
            streamService.saveStream(stream);
            return;
        }
        throw new TokenNotFound();
    }
}
