package io.github.sboyanovich.scannergenerator.automata;

import java.util.*;

/**
 * Represent state graph for automaton with 'numberOfStates' states.
 */
public class NFAStateGraph {
    private int numberOfStates;
    private List<List<Optional<Set<Integer>>>> edges;

    // created only through builder
    NFAStateGraph(int numberOfStates, List<List<Optional<Set<Integer>>>> edges) {
        this.numberOfStates = numberOfStates;
        this.edges = new ArrayList<>();
        for (int i = 0; i < numberOfStates; i++) {
            this.edges.add(new ArrayList<>());
            for (int j = 0; j < numberOfStates; j++) {
                this.edges.get(i).add(
                        edges.get(i).get(j).map(HashSet::new)
                ); // defensive copy
            }
        }
    }

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public boolean edgeExists(int i, int j) {
        // validate
        return this.edges.get(i).get(j).isPresent();
    }

    public boolean isLambdaEdge(int i, int j) {
        // validate
        Optional<Set<Integer>> marker = getEdgeMarkerAux(i, j);
        return marker.isPresent() && marker.get().isEmpty();
    }

    public boolean isNonTrivialEdge(int i, int j) {
        // validate
        return edgeExists(i, j) && !isLambdaEdge(i, j);
    }

    public Optional<Set<Integer>> getEdgeMarker(int i, int j) {
        // validate
        Optional<Set<Integer>> marker = getEdgeMarkerAux(i, j);
        return marker.map(Collections::unmodifiableSet);
    }

    private Optional<Set<Integer>> getEdgeMarkerAux(int i, int j) {
        // validate
        return this.edges.get(i).get(j);
    }
}