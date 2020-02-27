package io.github.sboyanovich.scannergenerator.tests.lexgen2;

import io.github.sboyanovich.scannergenerator.automata.DFA;
import io.github.sboyanovich.scannergenerator.automata.NFA;
import io.github.sboyanovich.scannergenerator.scanner.*;
import io.github.sboyanovich.scannergenerator.scanner.token.Domain;
import io.github.sboyanovich.scannergenerator.scanner.token.Token;
import io.github.sboyanovich.scannergenerator.utility.Utility;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static io.github.sboyanovich.scannergenerator.tests.lexgen2.StateTags.*;
import static io.github.sboyanovich.scannergenerator.utility.Utility.NEWLINE;

public class Test {
    public static void main(String[] args) {
        int alphabetSize = Character.MAX_CODE_POINT + 1;
        //alphabetSize = 256;

/*
        NFA spaceNFA = NFA.singleLetterLanguage(alphabetSize, " ");
        NFA tabNFA = NFA.singleLetterLanguage(alphabetSize, "\t");
        NFA newlineNFA = NFA.singleLetterLanguage(alphabetSize, "\n");
        NFA carretNFA = NFA.singleLetterLanguage(alphabetSize, "\r");

        NFA whitespaceNFA = spaceNFA
                .union(tabNFA)
                .union(carretNFA.concatenation(newlineNFA))
                .union(newlineNFA)
                .positiveIteration()
                .setAllFinalStatesTo(WHITESPACE);
*/
        NFA classSingleCharNFA = NFA.acceptsAllSymbolsButThese(
                alphabetSize, Set.of("\r", "\b", "\t", "\n", "\f", "\\", "-", "^", "[", "]"));

        NFA decimalDigitsNFA = NFA.acceptsThisRange(alphabetSize, "0", "9");
        NFA hexDigitsNFA = decimalDigitsNFA
                .union(
                        NFA.acceptsThisRange(alphabetSize, "A", "F")
                );

        NFA decimalNumberNFA = decimalDigitsNFA.positiveIteration();
        NFA hexNumberNFA = hexDigitsNFA.positiveIteration();

        NFA decimalEscapeNFA = NFA.acceptsThisWord(alphabetSize, "\\U+#")
                .concatenation(decimalNumberNFA);
        NFA hexEscapeNFA = NFA.acceptsThisWord(alphabetSize, "\\U+")
                .concatenation(hexNumberNFA);

        NFA uEscapeNFA = decimalEscapeNFA.union(hexEscapeNFA);

        NFA classEscapeNFA = uEscapeNFA
                .union(
                        NFA.acceptsAllTheseWords(
                                alphabetSize,
                                Set.of(
                                        "\\b", "\\t", "\\n", "\\f", "\\r", "\\\\",
                                        "\\-", "\\^", "\\[", "\\]")
                        )
                );

        NFA classCharNFA = classSingleCharNFA.union(classEscapeNFA)
                .setAllFinalStatesTo(CLASS_CHAR);

        NFA escapeNFA = uEscapeNFA
                .union(
                        NFA.acceptsAllTheseWords(
                                alphabetSize,
                                Set.of(
                                        "\\b", "\\t", "\\n", "\\f", "\\r", "\\\\",
                                        "\\\"", "\\'", "\\*", "\\+", "\\|", "\\?",
                                        "\\.", "\\(", "\\)"
                                )
                        )
                );

        NFA inputCharNFA = NFA.acceptsAllSymbolsButThese(
                alphabetSize, Set.of("\r", "\n")
        );

        NFA underscoreNFA = NFA.singleLetterLanguage(alphabetSize, "_");
        NFA latinLettersNFA = NFA.acceptsThisRange(alphabetSize, "A", "Z")
                .union(NFA.acceptsThisRange(alphabetSize, "a", "z"));
        NFA idenStartNFA = latinLettersNFA.union(underscoreNFA);
        NFA idenPartNFA = idenStartNFA.union(decimalDigitsNFA);
        NFA identifierNFA = idenStartNFA.concatenation(idenPartNFA.iteration());

        NFA namedExprNFA = NFA.singleLetterLanguage(alphabetSize, "{")
                .concatenation(identifierNFA)
                .concatenation(NFA.singleLetterLanguage(alphabetSize, "}"))
                .setAllFinalStatesTo(NAMED_EXPR);

        NFA charNFA = inputCharNFA.union(escapeNFA)
                .setAllFinalStatesTo(CHAR);

        NFA charClassRangeOpNFA = NFA.singleLetterLanguage(alphabetSize, "-")
                .setAllFinalStatesTo(CHAR_CLASS_RANGE_OP);

        NFA charClassOpenNFA = NFA.singleLetterLanguage(alphabetSize, "[")
                .setAllFinalStatesTo(CHAR_CLASS_OPEN);

        NFA charClassCloseNFA = NFA.singleLetterLanguage(alphabetSize, "]")
                .setAllFinalStatesTo(CHAR_CLASS_CLOSE);

        NFA charClassNegNFA = NFA.singleLetterLanguage(alphabetSize, "^")
                .setAllFinalStatesTo(CHAR_CLASS_NEG);

        NFA dotNFA = NFA.singleLetterLanguage(alphabetSize, ".")
                .setAllFinalStatesTo(DOT);
        NFA iterationOpNFA = NFA.singleLetterLanguage(alphabetSize, "*")
                .setAllFinalStatesTo(ITERATION_OP);
        NFA posIterationOpNFA = NFA.singleLetterLanguage(alphabetSize, "+")
                .setAllFinalStatesTo(POS_ITERATION_OP);
        NFA unionOpNFA = NFA.singleLetterLanguage(alphabetSize, "|")
                .setAllFinalStatesTo(UNION_OP);
        NFA optionOpNFA = NFA.singleLetterLanguage(alphabetSize, "?")
                .setAllFinalStatesTo(OPTION_OP);

        NFA lParenNFA = NFA.singleLetterLanguage(alphabetSize, "(")
                .setAllFinalStatesTo(LPAREN);
        NFA rParenNFA = NFA.singleLetterLanguage(alphabetSize, ")")
                .setAllFinalStatesTo(RPAREN);

        NFA classMinusOpNFA = NFA.acceptsThisWord(alphabetSize, "{-}")
                .setAllFinalStatesTo(CLASS_MINUS_OP);

        NFA repetitionOpNFA = NFA.singleLetterLanguage(alphabetSize, "{")
                .concatenation(decimalNumberNFA)
                .concatenation(
                        NFA.singleLetterLanguage(alphabetSize, ",")
                                .concatenation(decimalNumberNFA.optional()).optional()
                )
                .concatenation(NFA.singleLetterLanguage(alphabetSize, "}"))
                .setAllFinalStatesTo(REPETITION_OP);

        List<StateTag> priorityList = new ArrayList<>(
                List.of(
                        //WHITESPACE,
                        CHAR,
                        CLASS_CHAR,
                        CHAR_CLASS_OPEN,
                        CHAR_CLASS_CLOSE,
                        CHAR_CLASS_NEG,
                        CHAR_CLASS_RANGE_OP,
                        DOT,
                        ITERATION_OP,
                        POS_ITERATION_OP,
                        UNION_OP,
                        OPTION_OP,
                        LPAREN,
                        RPAREN
                )
        );

        Map<StateTag, Integer> priorityMap = new HashMap<>();
        for (int i = 0; i < priorityList.size(); i++) {
            priorityMap.put(priorityList.get(i), i);
        }

        NFA mode0 = charNFA
                .union(charClassOpenNFA)
                .union(dotNFA)
                .union(iterationOpNFA)
                .union(posIterationOpNFA)
                .union(unionOpNFA)
                .union(optionOpNFA)
                .union(lParenNFA)
                .union(rParenNFA)
                .union(namedExprNFA)
                .union(classMinusOpNFA)
                .union(repetitionOpNFA);

        NFA mode1 = classCharNFA
                .union(charClassRangeOpNFA)
                .union(charClassNegNFA)
                .union(charClassCloseNFA);

        System.out.println("Mode 0");
        LexicalRecognizer m0 = buildRecognizer(mode0, priorityMap);
        System.out.println();
        System.out.println("Mode 1");
        LexicalRecognizer m1 = buildRecognizer(mode1, priorityMap);
        System.out.println();

        String text = Utility.getText("LGTest1.txt");

        List<LexicalRecognizer> recognizers = List.of(m0, m1);
        Map<StateTag, Integer> modeSwitches = Map.of(
                CHAR_CLASS_OPEN, 1,
                CHAR_CLASS_CLOSE, 0
        );

        ProCompiler compiler = new ProCompiler(recognizers);
        ProScanner scanner = compiler.getScanner(text, modeSwitches);

        Set<Domain> ignoredTokenTypes = Set.of(
                SimpleDomains.WHITESPACE,
                Domain.END_OF_PROGRAM,
                Domain.ERROR
        );

        int errCount = 0;

        Token t = scanner.nextToken();
        while (t.getTag() != Domain.END_OF_PROGRAM) {
            if (!ignoredTokenTypes.contains(t.getTag())) {
                System.out.println(t);
            }
            if (t.getTag() == Domain.ERROR) {
                errCount++;
                System.out.println(t.getCoords());
            }
            t = scanner.nextToken();
        }

        System.out.println();
        System.out.println("Errors: " + errCount);
        System.out.println("Compiler messages: ");
        SortedMap<Position, Message> messages = compiler.getSortedMessages();
        for (Map.Entry<Position, Message> entry : messages.entrySet()) {
            System.out.println(entry.getValue() + " at " + entry.getKey());
        }

    }

    static LexicalRecognizer buildRecognizer(NFA lang, Map<StateTag, Integer> priorityMap) {
        System.out.println(lang.getNumberOfStates());

        // This appears to be necessary for determinization to work properly. It shouldn't be.
        lang = lang.removeLambdaSteps();
        System.out.println("Lambda steps removed.");

        Instant start = Instant.now();
        DFA dfa = lang.determinize(priorityMap);
        Instant stop = Instant.now();
        long timeElapsed = Duration.between(start, stop).toMillis();

        System.out.println("Determinized!");
        System.out.println("\tin " + timeElapsed + "ms");
        System.out.println("States: " + dfa.getNumberOfStates());
        System.out.println("Classes: " + dfa.getTransitionTable().getEquivalenceMap().getEqClassDomain());

        start = Instant.now();
        LexicalRecognizer recognizer = new LexicalRecognizer(dfa);
        stop = Instant.now();
        timeElapsed = Duration.between(start, stop).toMillis();
        System.out.println("Recognizer built!");
        System.out.println("\tin " + timeElapsed + "ms");
        System.out.println("States: " + recognizer.getNumberOfStates());
        System.out.println("Classes: " + recognizer.getNumberOfColumns());

        String dot = recognizer.toGraphvizDotString(Object::toString, true);
        System.out.println(dot);
        String factorization = recognizer.displayEquivalenceMap(Utility::defaultUnicodeInterpretation);
        System.out.println(NEWLINE + factorization + NEWLINE);

        return recognizer;
    }
}