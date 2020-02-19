package io.github.sboyanovich.scannergenerator.utility;

import io.github.sboyanovich.scannergenerator.automata.DFA;
import io.github.sboyanovich.scannergenerator.automata.NFA;
import io.github.sboyanovich.scannergenerator.automata.NFAStateGraph;
import io.github.sboyanovich.scannergenerator.automata.NFAStateGraphBuilder;
import io.github.sboyanovich.scannergenerator.scanner.Fragment;
import io.github.sboyanovich.scannergenerator.scanner.LexicalRecognizer;
import io.github.sboyanovich.scannergenerator.scanner.StateTag;
import io.github.sboyanovich.scannergenerator.scanner.Text;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.sboyanovich.scannergenerator.scanner.StateTag.FINAL_DUMMY;

public class Utility {

    public static final String SPACE = " ";
    public static final String LAMBDA = "\u03BB";
    public static final String NEWLINE = "\n";
    public static final String TAB = "\t";
    public static final String SEMICOLON = ";";
    public static final String COMMA = ",";
    public static final String ARROW = "-->";
    public static final String EMPTY = "";
    public static final String BNF_OR = " | ";
    public static final String NONTERMINAL_NAME_PREFIX = "Q_";
    public static final String MINUS = "-";
    public static final String DOT_ARROW = "->";
    public static final String EQDEF = ":=";

    public static int asCodePoint(String symbol) {
        return Character.codePointAt(symbol, 0);
    }

    public static String asString(int codePoint) {
        return new String(new int[]{codePoint}, 0, 1);
    }

    public static String defaultUnicodeInterpretation(int codepoint) {
        switch (codepoint) {
            case 9:
                return "TAB";
            case 10:
                return "NEWLINE";
            case 13:
                return "CR";
            case 32:
                return "SPACE";
            case 42:
                return "*";
            case 47:
                return "/";
        }
        if (isInRange(codepoint, asCodePoint("A"), asCodePoint("Z")) ||
                isInRange(codepoint, asCodePoint("a"), asCodePoint("z")) ||
                isInRange(codepoint, asCodePoint("0"), asCodePoint("9"))
        ) {
            return asString(codepoint);
        }

        return "U+#" + codepoint;
    }

    /**
     * Assigns distinct equivalence classes to all pivots. Each interval between two closest pivots
     * (left and right alphabet border act as implicit pivots) is assigned its own equivalence class.
     */
    public static EquivalenceMap getCoarseSymbolClassMap(List<Integer> pivots, int alphabetSize) {
        int[] resultMap = new int[alphabetSize];
        List<Integer> sortedPivots = new ArrayList<>(pivots);
        Collections.sort(sortedPivots);

        int classCounter = 0;
        int start = 0;
        for (int pivot : sortedPivots) {
            int i;
            for (i = start; i < pivot; i++) {
                resultMap[i] = classCounter;
            }
            // covers if there is nothing between prev and curr pivot
            if (i > start) {
                classCounter++;
            }
            resultMap[pivot] = classCounter;
            classCounter++;
            start = pivot + 1;
        }
        // last one
        for (int i = start; i < resultMap.length; i++) {
            resultMap[i] = classCounter;
        }
        int classNo = resultMap[alphabetSize - 1] + 1;

        return new EquivalenceMap(alphabetSize, classNo, resultMap);
    }

    public static EquivalenceMap getCoarseSymbolClassMap(List<Integer> pivots) {
        return getCoarseSymbolClassMap(pivots, Character.MAX_CODE_POINT + 1);
    }

    //EXPERIMENTAL

    /**
     * Assigns distinct equivalence classes to all pivots. All unmentioned symbols are in class 0.
     */
    public static EquivalenceMap getCoarseSymbolClassMapExp(List<Integer> pivots, int alphabetSize) {
        int[] resultMap = new int[alphabetSize];
        List<Integer> sortedPivots = new ArrayList<>(pivots);
        Collections.sort(sortedPivots);

        int classNo;

        if (pivots.size() < alphabetSize) {
            int classCounter = 1;
            for (int pivot : sortedPivots) {
                resultMap[pivot] = classCounter;
                classCounter++;
            }
            classNo = pivots.size() + 1;
        } else {
            for (int i = 0; i < alphabetSize; i++) {
                resultMap[i] = i;
            }
            classNo = alphabetSize;
        }

        return new EquivalenceMap(alphabetSize, classNo, resultMap);
    }

    /// EXPERIMENTAL
    public static EquivalenceMap getCoarseSymbolClassMapExp(List<Integer> pivots) {
        return getCoarseSymbolClassMapExp(pivots, Character.MAX_CODE_POINT + 1);
    }

    private static boolean areSymbolsEquivalent(int a, int b, int[][] transitionTable) {
        for (int[] aTransitionTable : transitionTable) {
            if (aTransitionTable[a] != aTransitionTable[b]) {
                return false;
            }
        }
        return true;
    }

    // numbers distinct elements from 0 and renames (returns new array)
    private static int[] normalizeMapping(int[] map) {
        int n = map.length;
        int[] result = new int[n];
        Map<Integer, Integer> known = new HashMap<>();

        int c = 0;
        for (int i = 0; i < map.length; i++) {
            int elem = map[i];
            if (!known.containsKey(elem)) {
                known.put(elem, c);
                c++;
            }
            result[i] = known.get(elem);
        }

        return result;
    }

    public static EquivalenceMap composeEquivalenceMaps(EquivalenceMap map1, EquivalenceMap map2) {
        // not checking parameters for validity for now
        int m = map1.getDomain();
        int[] resultMap = new int[m];
        for (int i = 0; i < resultMap.length; i++) {
            resultMap[i] = map2.getEqClass(map1.getEqClass(i));
        }
        return new EquivalenceMap(m, map2.getEqClassDomain(), resultMap);
    }

    // map eqDomain == transitionTable alphabet
    public static EquivalenceMap refineEquivalenceMap(EquivalenceMap map, int[][] transitionTable) {
        int n = map.getEqClassDomain();

        int[] auxMap = new int[n];
        // everyone is equivalent to themselves
        for (int i = 0; i < auxMap.length; i++) {
            auxMap[i] = i;
        }

        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                if ((auxMap[i] != auxMap[j]) && areSymbolsEquivalent(i, j, transitionTable)) {
                    auxMap[j] = auxMap[i];
                }
            }
        }

        auxMap = normalizeMapping(auxMap);

        List<Integer> aux = new ArrayList<>();
        for (int elem : auxMap) {
            aux.add(elem);
        }
        int c = Collections.max(aux) + 1;

        return new EquivalenceMap(n, c, auxMap);
    }

    // map maps alphabetSize -> eqDomain, where alphabetSize = transitionTable[0].length
    public static int[][] compressTransitionTable(int[][] transitionTable, EquivalenceMap map) {
        Objects.requireNonNull(transitionTable);
        Objects.requireNonNull(map);
        for (int i = 0; i < transitionTable.length; i++) {
            Objects.requireNonNull(transitionTable[i]);
            if (transitionTable[i].length != map.getDomain()) {
                throw new IllegalArgumentException(
                        "Map domain must be  [0, alphabetSize-1]!\n" +
                                "\talphabetSize = " + transitionTable[i].length + "\n" +
                                "\tmapDomain = " + map.getDomain());
            }
        }

        int n = transitionTable.length; // number of states
        int m = map.getEqClassDomain();

        int[][] result = new int[n][m];

        for (int i = 0; i < transitionTable.length; i++) {
            for (int j = 0; j < transitionTable[i].length; j++) {
                int state = i;
                int symbol = map.getEqClass(j);
                result[state][symbol] = transitionTable[i][j];
            }
        }
        return result;
    }

    // EXPERIMENTAL
    //  hint domain must be equal to alphabetSize
    public static Pair<EquivalenceMap, DFA> compressAutomaton(EquivalenceMap hint, DFA automaton) {


        int[][] transitionTable = automaton.getTransitionTable();

        int numberOfStates = automaton.getNumberOfStates();
        int initialState = automaton.getInitialState();

        int[][] table = compressTransitionTable(transitionTable, hint);

        EquivalenceMap rmap = refineEquivalenceMap(
                hint,
                table
        );

        int newAlphabetSize = rmap.getEqClassDomain();
        Map<Integer, StateTag> labelsMap = new HashMap<>();
        for (int i = 0; i < numberOfStates; i++) {
            labelsMap.put(i, automaton.getStateTag(i));
        }

        EquivalenceMap emap = composeEquivalenceMaps(hint, rmap);

        int[][] newTransitionTable = compressTransitionTable(
                table,
                rmap
        );

        DFA dfa = new DFA(numberOfStates, newAlphabetSize, initialState, labelsMap, newTransitionTable);

        return new Pair<>(emap, dfa);
    }

    public static Pair<EquivalenceMap, DFA> compressAutomaton(DFA automaton) {
        return compressAutomaton(
                EquivalenceMap.identityMap(automaton.getAlphabetSize()),
                automaton
        );
    }

    public static <T> Set<T> union(Set<T> s1, Set<T> s2) {
        Set<T> result = new HashSet<>();
        result.addAll(s1);
        result.addAll(s2);
        return result;
    }

    public static <T> Set<T> difference(Set<T> s1, Set<T> s2) {
        Set<T> result = new HashSet<>(s1);
        result.removeAll(s2);
        return result;
    }

    public static String getTextFragmentAsString(Text text, Fragment span) {
        return text.subtext(
                span.getStarting().getIndex(),
                span.getFollowing().getIndex()
        )
                .toString();
    }

    public static int[][] copyTable(int[][] table) {
        int[][] result = new int[table.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = Arrays.copyOf(table[i], table[i].length);
        }
        return result;
    }

    public static boolean isInRange(int x, int n, int m) {
        return (x >= n && x <= m);
    }

    /**
     * reads text from file res/filename
     */
    public static String getText(String filename) {
        StringBuilder lines = new StringBuilder();

        FileReader fr;
        try {
            fr = new FileReader("res/" + filename);
            BufferedReader br = new BufferedReader(fr);
            String currLine = br.readLine();
            while (currLine != null) {
                lines.append(currLine).append("\n");
                currLine = br.readLine();
            }

            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("FILE NOT FOUND");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines.toString().substring(0, lines.length() - 1);
    }

    // for DEBUG
    public static void printTransitionTable(int[][] transitionTable, int paddingTo) {
        for (int[] aTransitionTable : transitionTable) {
            for (int anATransitionTable : aTransitionTable) {
                System.out.print(pad(anATransitionTable, paddingTo) + " ");
            }
            System.out.println();
        }
    }

    private static String pad(int arg, int paddingTo) {
        StringBuilder result = new StringBuilder();
        int n = paddingTo - String.valueOf(arg).length();
        result.append(arg);
        for (int i = 0; i < n; i++) {
            result.append(" ");
        }
        return result.toString();
    }

    /**
     * Represents a collection of integers into a list of sorted segments, i.e.
     * <p>
     * 0, 1, 2, 3, 6, 7, 8, 10, 11, 14, 15, 16 ==> 0-3, 6-8, 10, 11, 14-16
     */
    public static List<Pair<Integer, Integer>> compressIntoSegments(Collection<Integer> data) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        List<Integer> sortedData = new ArrayList<>(data);
        Collections.sort(sortedData);

        int n = sortedData.size();
        int segstart = 0;
        int seglen = 1;

        while (segstart < n) {
            while (segstart + seglen < n &&
                    (sortedData.get(segstart + seglen) - sortedData.get(segstart) == seglen)) {
                seglen++;
            }

            int a = sortedData.get(segstart);
            int b = sortedData.get(segstart + seglen - 1);

            result.add(new Pair<>(a, b));

            segstart += seglen;
            seglen = 1;
        }

        return result;
    }

    private static String displaySegment(
            Pair<Integer, Integer> segment, Function<Integer, String> interpretation
    ) {
        int a = segment.getFirst();
        int b = segment.getSecond();

        int seglen = b - a + 1;

        if (seglen > 2) {
            return interpretation.apply(a) + MINUS + interpretation.apply(b);
        } else if (seglen > 1) {
            return interpretation.apply(a) + COMMA + SPACE + interpretation.apply(b);
        } else {
            return interpretation.apply(a);
        }
    }

    public static String displayAsSegments(Collection<Integer> data, Function<Integer, String> interpretation) {
        List<Pair<Integer, Integer>> segments = compressIntoSegments(data);
        StringBuilder result = new StringBuilder();

        if (!segments.isEmpty()) {
            result.append(displaySegment(segments.get(0), interpretation));
        }
        for (int i = 1; i < segments.size(); i++) {
            result.append(COMMA).append(SPACE);
            result.append(displaySegment(segments.get(i), interpretation));
        }

        return result.toString();
    }

    /// EXPERIMENTAL METHODS SECTION

    public static NFA acceptsAllTheseSymbols(int alphabetSize, Set<String> symbols) {
        NFAStateGraphBuilder edges = new NFAStateGraphBuilder(2, alphabetSize);
        Set<Integer> codePoints = symbols.stream().map(Utility::asCodePoint).collect(Collectors.toSet());
        edges.setEdge(0, 1, codePoints);
        return new NFA(2, alphabetSize, 0, Map.of(1, FINAL_DUMMY), edges.build());
    }

    public static NFA acceptThisWord(int alphabetSize, String word) {
        List<Integer> codePoints = word.codePoints().boxed().collect(Collectors.toList());
        int n = codePoints.size();
        NFAStateGraphBuilder edges = new NFAStateGraphBuilder(n + 1, alphabetSize);
        for (int i = 0; i < n; i++) {
            int codePoint = codePoints.get(i);
            edges.addSymbolToEdge(i, i + 1, codePoint);
        }
        return new NFA(n + 1, alphabetSize, 0, Map.of(n, StateTag.FINAL_DUMMY), edges.build());
    }

    // now this exists mostly for test compatibility reasons
    public static NFA acceptThisWord(int alphabetSize, List<String> symbols) {
        int n = symbols.size();
        NFAStateGraphBuilder edges = new NFAStateGraphBuilder(n + 1, alphabetSize);
        for (int i = 0; i < n; i++) {
            int codePoint = asCodePoint(symbols.get(i));
            edges.addSymbolToEdge(i, i + 1, codePoint);
        }
        return new NFA(n + 1, alphabetSize, 0, Map.of(n, StateTag.FINAL_DUMMY), edges.build());
    }

    public static void addEdge(NFAStateGraphBuilder edges, int from, int to, Set<String> edge) {
        for (String symbol : edge) {
            edges.addSymbolToEdge(from, to, asCodePoint(symbol));
        }
    }

    public static void addEdgeSubtractive(NFAStateGraphBuilder edges, int from, int to, Set<String> edge) {
        int alphabetSize = edges.getAlphabetSize();
        Set<Integer> codePoints = edge.stream().map(Utility::asCodePoint).collect(Collectors.toSet());
        for (int i = 0; i < alphabetSize; i++) {
            if (!codePoints.contains(i)) {
                edges.addSymbolToEdge(from, to, i);
            }
        }
    }

    static boolean isSubtractive(Set<Integer> marker, int alphabetSize, int limit) {
        return (alphabetSize - marker.size()) < limit;
    }

    public static List<Integer> mentioned(NFA nfa) {
        int alphabetSize = nfa.getAlphabetSize();
        int n = nfa.getNumberOfStates();
        NFAStateGraph edges = nfa.getEdges();

        Set<Integer> aux = new HashSet<>();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Optional<Set<Integer>> marker = edges.getEdgeMarker(i, j);
                if (marker.isPresent()) {
                    Set<Integer> markerSet = marker.get();
                    if (!isSubtractive(markerSet, alphabetSize, 5)) {
                        aux.addAll(markerSet);
                    } else {
                        for (int k = 0; k < alphabetSize; k++) {
                            if (!markerSet.contains(k)) {
                                aux.add(k);
                            }
                        }
                    }
                }
            }
        }

        List<Integer> result = new ArrayList<>(aux);

        return result;
    }

    // with hint heuristic
    public static LexicalRecognizer createRecognizer(NFA lang, Map<StateTag, Integer> priorityMap) {
        int alphabetSize = lang.getAlphabetSize();
        // this one seems to be important for performance! (of mentioned(...) )
        lang = lang.removeLambdaSteps();
        EquivalenceMap hint = getCoarseSymbolClassMap(mentioned(lang), alphabetSize);
        DFA dfa = lang.determinize(priorityMap);
        return new LexicalRecognizer(hint, dfa);
    }
}
