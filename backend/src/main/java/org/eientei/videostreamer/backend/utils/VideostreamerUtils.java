package org.eientei.videostreamer.backend.utils;

import org.eientei.videostreamer.backend.orm.entity.Message;
import org.eientei.videostreamer.backend.orm.entity.User;
import org.eientei.videostreamer.backend.pojo.chat.ServerChatMessageItem;
import org.eientei.videostreamer.backend.pojo.site.UserInfo;
import org.eientei.videostreamer.backend.security.AppUserDetails;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.DefaultValueStack;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.ValueStack;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-20
 * Time: 21:53
 */
public class VideostreamerUtils {
    private static RestTemplate restTemplate = new RestTemplate();

    public static String hashMd5(String hashSource) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        byte[] bytes = md.digest(hashSource.getBytes());
        StringBuilder sb = new StringBuilder(2 * bytes.length);

        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }

    public static String getIp(HttpServletRequest request) {
        String remote = request.getHeader("X-Forwarded-For");
        if (remote == null) {
            remote = request.getRemoteAddr();
        }
        return remote;
    }

    public static String determineUserHash(User user, HttpServletRequest request) {
        return determineUserHash(user, getIp(request));
    }


    public static String determineUserHash(User user, String remote) {
        String email = null;
        String hash;

        if (user != null && !user.getName().equals("Anonymous")) {
            email = user.getEmail();
        }

        if (email != null && !email.isEmpty()) {
            hash = hashMd5(email);
        } else {
            if (user == null || user.getName().equals("Anonymous")) {
                hash = hashMd5(remote);
            } else {
                hash = hashMd5(user.getName());
            }
        }
        return hash;
    }

    public static UserInfo makeUserInfo(AppUserDetails appUserDetails, HttpServletRequest request) {
        UserInfo userInfo = new UserInfo();
        User user = appUserDetails.getDataUser();
        if (!user.getName().equals("Anonymous")) {
            userInfo.setId(user.getId());
            userInfo.setName(user.getName());
            if (user.getEmail() != null) {
                userInfo.setEmail(user.getEmail());
            }
            userInfo.setAuthed(true);
        } else {
            userInfo.setName("Anonymous");
            userInfo.setAuthed(false);
        }
        userInfo.setHash(VideostreamerUtils.determineUserHash(user, request));
        return userInfo;
    }

    public static boolean checkCaptcha(HttpServletRequest req, String captchaChallenge, String captchaResponse, String recaptchaPrivate) {
        MultiValueMap<String, String> r = new LinkedMultiValueMap<String, String>();
        r.add("privatekey", recaptchaPrivate);
        r.add("remoteip",  VideostreamerUtils.getIp(req));
        r.add("challenge", captchaChallenge);
        r.add("response", captchaResponse);
        String res = restTemplate.postForObject("http://www.google.com/recaptcha/api/verify", r, String.class);
        return res.startsWith("true");
    }

    public static String preProcess(String message) {
        MessageParser parser = Parboiled.createParser(MessageParser.class);
        ParseRunner<String> runner = new BasicParseRunner<String>(parser.Input());
        ValueStack<String> stack = new DefaultValueStack<String>();
        stack.push("");
        ParsingResult<String> result = runner.withValueStack(stack).run(message);
        return result.valueStack.pop();
    }

    public static ServerChatMessageItem buildMessageItem(Message message) {
        ServerChatMessageItem item = new ServerChatMessageItem();
        item.setAuthor(VideostreamerUtils.determineUserHash(message.getAuthor(), message.getRemote()));
        item.setDate(message.getPosted().getTime());
        item.setId(message.getId());
        item.setAdmin(message.isAdmin());
        item.setOwner(message.isAuthor());
        item.setRemote(VideostreamerUtils.hashMd5(message.getRemote()));
        item.setText(VideostreamerUtils.preProcess(message.getMessage()));
        return item;
    }
}
