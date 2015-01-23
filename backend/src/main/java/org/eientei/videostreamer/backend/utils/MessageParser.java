package org.eientei.videostreamer.backend.utils;

import org.parboiled.BaseParser;
import org.parboiled.Rule;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-21
 * Time: 12:20
 */
public class MessageParser extends BaseParser<String> {
    private final static String BLANK = " \t\r\n";
    private final static String CNTRL = "%_*";

    public Rule Input() {
        return Sequence(Message(), AddSpace(), EOI);
    }

    public String filterString(String input) {
        return input.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
    }

    public String filterUrl(String input) {
        return input.replace(">", "&gt;").replace("<", "&lt;");
    }


    Rule Message() {
        return ZeroOrMore(FirstOf(
                Sequence(push("<span class=\"quote\">"), AddSpace(), Sequence(TestNot(Ref()), Test(">"), InputLine()), push(pop() + "</span>"), NewLine(), push(pop(1) + pop())),
                Sequence(AddSpace(), InputLine(), NewLine())
        ));
    }

    Rule InputLine() {
        return OneOrMore(Sequence(FirstOf(Markup(), Plaintext()), AddSpace()), push(pop(1) + pop()));
    }

    Rule Plaintext() {
        return Sequence(OneOrMore(NoneOf(BLANK)), push(filterString(match())));
    }

    Rule PlaintextUntilSpaced(Object terminator) {
        return OneOrMore(TestNot(terminator, FirstOf(OneOrMore(AnyOf(CNTRL + " \t\r\n")), EOI)), NoneOf(BLANK));
    }

    Rule Markup() {
        return FirstOf(Bold(true, "**"), Bold(true, "__"), Italic(true, "*"), Italic(true, "_"), Spoiler(true), Url(), Ref());
    }

    Rule MarkupNested() {
        return FirstOf(Bold(false, "**"), Bold(false, "__"), Italic(false, "*"), Italic(false, "_"), Spoiler(false), Url(), Ref());
    }

    Rule TextUntil(Object sym) {
        return FirstOf(
                Sequence(PushSpace(0), MarkupNested(), PushSpace(2), push(pop(1) + pop() + pop())),
                Sequence(Sequence(Space(), PlaintextUntilSpaced(sym), Space()), push(filterString(match())))
        );
    }

    Rule Surround(boolean toplevel, String sym, String before, String after) {
        if (toplevel) {
            return Sequence(sym, TextUntil(sym), push(before + pop() + after), sym, Test(FirstOf(OneOrMore(AnyOf(" \t\r\n")), EOI)));
        } else {
            return Sequence(sym, TextUntil(sym), push(before + pop() + after), sym);
        }
    }

    Rule Bold(boolean toplevel, String sym) {
        return Surround(toplevel, sym, "<strong>", "</strong>");
    }

    Rule Italic(boolean toplevel, String sym) {
        return Surround(toplevel, sym, "<em>", "</em>");
    }

    Rule Spoiler(boolean toplevel) {
        return Surround(toplevel, "%%", "<span class=\"spoiler\">", "</span>");
    }

    Rule Url() {
        return Sequence(
                Sequence(Sequence(OneOrMore(TestNot("://"), NoneOf(CNTRL + BLANK)), "://", OneOrMore(NoneOf(BLANK))), push(match())),
                push("<a target=\"_blank\" href=\"" + filterUrl(match()) + "\">" + filterString(pop()) + "</a>")
        );
    }

    Rule Ref() {
        return Sequence(
                ">>",
                OneOrMore(CharRange('0', '9')),
                push("<a href=\"javascript:void(0)\" data-refid=\"" + match() + "\">&gt;&gt;" + match() + "</a>")
        );
    }

    Rule NewLine() {
        return Sequence(ZeroOrMore("\n"), push(pop() + match()));
    }

    Rule Space() {
        return ZeroOrMore(AnyOf(" \t\r"));
    }

    Rule AddSpace() {
        return Sequence(ZeroOrMore(AnyOf(" \t\r")), push(pop() + match()));
    }

    Rule PushSpace(int p) {
        return Sequence(ZeroOrMore(AnyOf(" \t\r")), push(p, match()));
    }
}
