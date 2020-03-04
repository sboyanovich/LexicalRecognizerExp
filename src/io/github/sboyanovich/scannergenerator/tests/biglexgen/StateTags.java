package io.github.sboyanovich.scannergenerator.tests.biglexgen;

import io.github.sboyanovich.scannergenerator.scanner.StateTag;

public enum StateTags implements StateTag {
    WHITESPACE_IN_REGEX,
    WHITESPACE,
    IDENTIFIER,
    DOMAINS_GROUP_MARKER,
    RULE_END,
    MODES_SECTION_MARKER,
    DEFINER,
    RULES_SECTION_MARKER,
    COMMA,
    L_ANGLE_BRACKET,
    R_ANGLE_BRACKET,
    CHAR_CLASS_OPEN,
    CHAR_CLASS_CLOSE,
    CHAR_CLASS_NEG,
    CHAR_CLASS_RANGE_OP,
    REPETITION_OP,
    CLASS_MINUS_OP,
    NAMED_EXPR,
    LPAREN,
    RPAREN,
    CHAR,
    CLASS_CHAR,
    DOT,
    ITERATION_OP,
    POS_ITERATION_OP,
    UNION_OP,
    OPTION_OP,
    COMMENT_START,
    COMMENT_CLOSE,
    NO_ASTERISK_SEQ,
    ASTERISK
}