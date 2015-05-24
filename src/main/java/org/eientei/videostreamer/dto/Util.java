package org.eientei.videostreamer.dto;

import org.eientei.videostreamer.dto.parser.MessageNode;
import org.eientei.videostreamer.dto.parser.MessageParser;
import org.eientei.videostreamer.orm.entity.User;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.DefaultValueStack;
import org.parboiled.support.ValueStack;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;

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
}
