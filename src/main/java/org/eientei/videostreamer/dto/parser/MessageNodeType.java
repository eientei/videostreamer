package org.eientei.videostreamer.dto.parser;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-21
 * Time: 20:10
 */
public enum MessageNodeType {
    BOLD('B'),
    ITALIC('I'),
    PARAGRAPH('P'),
    QUOTE('Q'),
    REF('F'),
    ROOT('R'),
    SPOILER('S'),
    TEXT('T'),
    URL('U');

    private char code;

    MessageNodeType(char code) {
        this.code = code;
    }

    @JsonValue
    public char getCode() {
        return code;
    }
}
