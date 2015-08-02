package org.eientei.video.backend.controller;

import io.swagger.annotations.Api;
import org.eientei.video.backend.orm.entity.Stream;
import org.eientei.video.backend.orm.error.TokenNotFound;
import org.eientei.video.backend.orm.service.StreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-23
 * Time: 15:13
 */
@RestController
@RequestMapping("control/nginx")
@Api(hidden = true)
public class Nginx {
    @Autowired
    private StreamService streamService;

    @RequestMapping("check_access")
    public RedirectView checkAccess(Model model, @RequestParam("name") String name, @RequestParam("addr") String addr) throws TokenNotFound {
        Stream stream = streamService.getStreamByToken(name);
        if (stream != null) {
            streamService.updateRemote(stream, addr);
            model.asMap().clear();
            return new RedirectView(stream.getName(), false, false, false);
        }
        throw new TokenNotFound();
    }

    @RequestMapping(value = "finish_stream")
    public void finishStream(@RequestParam("name") String name) throws TokenNotFound {
        Stream stream = streamService.getStreamByToken(name);
        if (stream != null) {
            streamService.updateRemote(stream, null);
            return;
        }
        throw new TokenNotFound();
    }
}
