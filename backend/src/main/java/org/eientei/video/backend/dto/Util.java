package org.eientei.video.backend.dto;

import org.eientei.video.backend.config.security.VideostreamerUser;
import org.eientei.video.backend.dto.parser.MessageNode;
import org.eientei.video.backend.dto.parser.MessageParser;
import org.eientei.video.backend.orm.entity.User;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.DefaultValueStack;
import org.parboiled.support.ValueStack;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 13:34
 */
public class Util {
    private static Md5PasswordEncoder passwordEncoder = new Md5PasswordEncoder();

    public static String hash(User user, String remote) {
        if (user.getEmail() == null) {
            if (user.getName().equalsIgnoreCase("anonymous")) {
                return passwordEncoder.encodePassword(remote, null);
            }
            return passwordEncoder.encodePassword(user.getName(), null);
        }
        return passwordEncoder.encodePassword(user.getEmail(), null);
    }

    public static String hash(User user) {
        if (user.getEmail() == null) {
            return passwordEncoder.encodePassword(user.getName(), null);
        }
        return passwordEncoder.encodePassword(user.getEmail(), null);
    }

    public static String hash(String string) {
        return passwordEncoder.encodePassword(string, null);
    }

    public static MessageNode parseMessage(String string) {
        MessageParser parser = Parboiled.createParser(MessageParser.class);
        ParseRunner<MessageNode> runner = new BasicParseRunner<MessageNode>(parser.Input());
        ValueStack<MessageNode> stack = new DefaultValueStack<MessageNode>();
        runner.withValueStack(stack).run(string);
        return stack.pop();
    }


    public static String determineHash(VideostreamerUser user, HttpServletRequest request) {
        return Util.hash(user.getEntity(), determineIp(request));
    }

    public static String determineIp(HttpServletRequest request) {
        String remote = request.getHeader("X-Forwarded-For");
        if (remote == null) {
            remote = request.getRemoteAddr();
        }
        return remote;
    }

    public static void setupCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        Cookie cookie = new Cookie("XSRF-TOKEN", csrf.getToken());
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
