package org.eientei.videostreamer.dto.parser;

import org.parboiled.BaseParser;
import org.parboiled.Rule;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-14
 * Time: 15:10
 */
public class MessageParser extends BaseParser<MessageNode> {
    private final static String BLANK = " \t\r\n";
    private final static String SPACE = " \t\r";
    private final static String CNTRL = "%_*";

    public Rule Input() {
        return Sequence(push(new MessageRootNode()), Optional(Message()), EOI);
    }

    Rule Message() {
        return Sequence(ZeroOrMore(AnyOf(BLANK)), ZeroOrMore(TestNot(FirstOf(AnyOf(BLANK), EOI)), Paragraph()));
    }

    Rule Paragraph() {
        return Sequence(push(new MessageParagraphNode()), FirstOf(Quote(), LineContent()), ZeroOrMore(AnyOf(BLANK)), push(pop().commitTo(pop())));
    }

    Rule LineContent() {
        return OneOrMore(TestNot(FirstOf(AnyOf(BLANK), EOI)), FirstOf(Markup(), PlainText(NOTHING)));
    }

    Rule Quote() {
        return Sequence(
                push(new MessageQuoteNode()),
                TestNot(Ref()),
                OneOrMore(">"),
                peek().add(match()),
                ZeroOrMore(AnyOf(SPACE)),
                LineContent(),
                ZeroOrMore(AnyOf(SPACE)),
                push(pop().commitTo(pop()))
        );
    }

    Rule Markup() {
        return Sequence(
                TestNot(FirstOf(AnyOf(BLANK), EOI)),
                FirstOf(
                        Ref(),
                        Url(),
                        Pair(MessageSpoilerNode.class, "%%"),
                        Pair(MessageBoldNode.class, "**"),
                        Pair(MessageBoldNode.class, "__"),
                        Pair(MessageItalicNode.class, "*"),
                        Pair(MessageItalicNode.class, "_")
                ),
                ZeroOrMore(AnyOf(SPACE))
        );
    }

    Rule Ref() {
        return Sequence(
                ">>",
                OneOrMore(CharRange('0', '9')),
                push(new MessageRefNode()),
                peek().add(match()),
                push(pop().commitTo(pop()))
        );
    }

    Rule Url() {
        return Sequence(
                Sequence(
                        OneOrMore(
                                TestNot("://"),
                                NoneOf(BLANK + CNTRL)
                        ),
                        "://",
                        OneOrMore(NoneOf(BLANK))
                ),
                push(new MessageUrlNode()),
                peek().add(match()),
                push(pop().commitTo(pop()))
        );
    }

    Rule Pair(Class<? extends MessageNode> node, String quote) {
        return Sequence(
                quote,
                ZeroOrMore(AnyOf(SPACE)),
                TestNot(quote.substring(0, 1)),
                push(MessageNode.make(node)),
                OneOrMore(FirstOf(Markup(), PlainText(quote))),
                quote,
                TestNot(quote.substring(0, 1)),
                ZeroOrMore(AnyOf(SPACE)),
                push(pop().commitTo(pop()))
        );
    }

    Rule PlainText(Object terminator) {
        return Sequence(
                push(new MessageTextNode()),
                OneOrMore(
                        TestNot(FirstOf(AnyOf(BLANK), EOI)),
                        TestNot(terminator),
                        TestNot(Markup()),
                        Word(terminator),
                        peek().add(match()),
                        ZeroOrMore(AnyOf(SPACE))
                ),
                push(pop().commitTo(pop()))
        );
    }

    Rule Word(Object terminator) {
        if (terminator instanceof String) {
            String r = ((String) terminator).substring(0, 1);
            String rs = CNTRL.replace(r, "");
            return OneOrMore(TestNot(terminator, ZeroOrMore(AnyOf(CNTRL)), FirstOf(AnyOf(BLANK), EOI)), NoneOf(BLANK));
        }
        return OneOrMore(TestNot(terminator, FirstOf(AnyOf(BLANK), EOI)), NoneOf(BLANK));
    }
}
