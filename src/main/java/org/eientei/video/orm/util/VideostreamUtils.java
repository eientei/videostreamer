package org.eientei.video.orm.util;

import org.eientei.video.orm.entity.User;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.DefaultValueStack;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.ValueStack;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-15
 * Time: 07:57
 */
public class VideostreamUtils {
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
    public static String determineUserHash(User user, String remote) {
        String email = user.getEmail();
        String hash;

        if (email != null && !email.isEmpty()) {
            hash = hashMd5(email);
        } else {
            if (user.getName().equals("Anonymous")) {
                hash = hashMd5(remote);
            } else {
                hash = hashMd5(user.getName());
            }
        }
        return hash;
    }

    public static String preProcess(String message) {
        MessageParser parser = Parboiled.createParser(MessageParser.class);
        ParseRunner<String> runner = new BasicParseRunner<String>(parser.Input());
        ValueStack<String> stack = new DefaultValueStack<String>();
        stack.push("");
        ParsingResult<String> result = runner.withValueStack(stack).run(message);
        return result.valueStack.pop();
    }
}
