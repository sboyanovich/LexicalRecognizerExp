package utility;

import lab.Fragment;
import lab.lex.Text;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Utility {

    public static int asCodePoint(String symbol) {
        return Character.codePointAt(symbol, 0);
    }

    public static String asString(int codePoint) {
        return new String(new int[]{codePoint}, 0, 1);
    }

    // all pivots are distinct valid code points
    public static EquivalenceMap getCoarseSymbolClassMap(List<Integer> pivots) {
        int totalValidCodePoints = Character.MAX_CODE_POINT - Character.MIN_CODE_POINT + 1;

        int[] resultMap = new int[totalValidCodePoints];
        List<Integer> sortedPivots = new ArrayList<>(pivots);
        Collections.sort(sortedPivots);

        int classCounter = 0;
        int start = 0;
        for (int pivot : sortedPivots) {
            for (int i = start; i < pivot; i++) {
                resultMap[i] = classCounter;
            }
            classCounter++;
            resultMap[pivot] = classCounter;
            classCounter++;
            start = pivot + 1;
        }
        // last one
        for (int i = start; i < resultMap.length; i++) {
            resultMap[i] = classCounter;
        }

        int classNo = resultMap[Character.MAX_CODE_POINT] + 1;

        return new EquivalenceMap(Character.MAX_CODE_POINT + 1, classNo, resultMap);
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

    // no validation for now
    public static int[][] compressTransitionTable(int[][] transitionTable, EquivalenceMap map) {
        int n = transitionTable.length;
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

    // modifies transition table, adding transition from <from> to <to> for <symbols>,
    public static void addEdge(int[][] transitionTable, int from, int to, EquivalenceMap map, Set<String> symbols) {
        for (String symbol : symbols) {
            int eqClass = map.getEqClass(asCodePoint(symbol));
            transitionTable[from][eqClass] = to;
        }
    }

    public static void addEdgeSubtractive(int[][] transitionTable, int from, int to, EquivalenceMap map, Set<String> exceptions) {
        Set<Integer> aux = new HashSet<>();
        exceptions.forEach(s -> aux.add(
                map.getEqClass(
                        asCodePoint(s)
                )
        ));

        for (int i = 0; i < transitionTable[from].length; i++) {
            if (!aux.contains(i)) {
                transitionTable[from][i] = to;
            }
        }
    }

    public static String getTextFragmentAsString(Text text, Fragment span) {
        return text.subtext(
                span.getStarting().getIndex(),
                span.getFollowing().getIndex()
        )
                .toString();
    }
}