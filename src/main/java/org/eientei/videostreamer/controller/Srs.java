package org.eientei.videostreamer.controller;

import org.eientei.videostreamer.dto.SrsCheckAccessDTO;
import org.eientei.videostreamer.dto.SrsFinishStreamDTO;
import org.eientei.videostreamer.orm.entity.Stream;
import org.eientei.videostreamer.orm.error.TokenNotFound;
import org.eientei.videostreamer.orm.service.StreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-25
 * Time: 23:11
 */
@RestController
@RequestMapping("srs")
public class Srs {
    @Autowired
    private StreamService streamService;

    @RequestMapping("check_access")
    public RedirectView checkAccess(Model model, @RequestBody SrsCheckAccessDTO data) throws TokenNotFound {
        Stream stream = streamService.getStreamByToken(data.getStream());
        if (stream != null) {
            streamService.updateRemote(stream, data.getIp());
            model.asMap().clear();
            RedirectView redirectView = new RedirectView(stream.getName(), false, false, false);
            redirectView.setStatusCode(HttpStatus.FOUND);
            return redirectView;
        }
        throw new TokenNotFound();
    }

    @RequestMapping(value = "finish_stream")
    public void finishStream(@RequestBody SrsFinishStreamDTO data) throws TokenNotFound {
        Stream stream = streamService.getStreamByToken(data.getStream());
        if (stream != null) {
            streamService.updateRemote(stream, null);
            return;
        }
        throw new TokenNotFound();
    }
}
