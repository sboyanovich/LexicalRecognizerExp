package io.github.sboyanovich.scannergenerator.automata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static io.github.sboyanovich.scannergenerator.utility.Utility.SPACE;
import static io.github.sboyanovich.scannergenerator.utility.Utility.displayAsSegments;

public class Utility {
    static final int NOT_FINAL_PRIORITY_RANK = -2;
    static final int FINAL_DUMMY_PRIORITY_RANK = -1;

    /**
     * @param transitionTable Table[STATE][SYMBOL] contains state reachable from STATE through SYMBOL.
     * @return Equivalent NFAStateGraph.
     */
    public static NFAStateGraph computeEdgeLabels(DFATransitionTable transitionTable) {
        int numberOfStates = transitionTable.getNumberOfStates();
        int alphabetSize = transitionTable.getAlphabetSize();

        NFAStateGraphBuilder result = new NFAStateGraphBuilder(numberOfStates, alphabetSize);

        for (int state = 0; state < numberOfStates; state++) {
            for (int symbol = 0; symbol < alphabetSize; symbol++) {
                int to = transitionTable.transition(state, symbol);
                result.addSymbolToEdge(state, to, symbol);
            }
        }

        return result.build();
    }

    /**
     * @param transitionTable Table[STATE][SYMBOL] contains state reachable from STATE through SYMBOL.
     * @return Equivalent NFAStateGraph.
     */
    public static NFAStateGraph computeEdgeLabels(int[][] transitionTable) {
        int numberOfStates = transitionTable.length;
        int alphabetSize = transitionTable[0].length;

        NFAStateGraphBuilder result = new NFAStateGraphBuilder(numberOfStates, alphabetSize);

        for (int state = 0; state < numberOfStates; state++) {
            for (int symbol = 0; symbol < alphabetSize; symbol++) {
                int to = transitionTable[state][symbol];
                result.addSymbolToEdge(state, to, symbol);
            }
        }

        return result.build();
    }

    public static String defaultAlphabetInterpretation(int letter) {
        return "a_" + letter;
    }

    public static String edgeLabelAsString(Set<Integer> edgeLabel) {
        return edgeLabelAsString(edgeLabel, Utility::defaultAlphabetInterpretation);
    }

    public static String edgeLabelAsString(
            Set<Integer> edgeLabel, Function<Integer, String> alphabetInterpretation) {
        return edgeLabelAsString(edgeLabel, alphabetInterpretation, true);
    }

    public static String edgeLabelAsString(
            Set<Integer> edgeLabel, Function<Integer, String> alphabetInterpretation, boolean compressEdgeLabels) {
        Objects.requireNonNull(edgeLabel);

        if (!compressEdgeLabels) {

            StringBuilder result = new StringBuilder();
            if (edgeLabel.isEmpty()) {
                result.append(io.github.sboyanovich.scannergenerator.utility.Utility.LAMBDA);
            } else {
                List<Integer> letters = new ArrayList<>(edgeLabel);
                result.append(alphabetInterpretation.apply(letters.get(0)));
                for (int i = 1; i < letters.size(); i++) {
                    result.append(io.github.sboyanovich.scannergenerator.utility.Utility.COMMA + SPACE);
                    result.append(alphabetInterpretation.apply(letters.get(i)));
                }
            }
            return result.toString();
        } else {
            return edgeLabelAsStringCompressed(edgeLabel, alphabetInterpretation);
        }
    }

    private static String edgeLabelAsStringCompressed(
            Set<Integer> edgeLabel, Function<Integer, String> alphabetInterpretation
    ) {
        return displayAsSegments(edgeLabel, alphabetInterpretation);
    }
}
