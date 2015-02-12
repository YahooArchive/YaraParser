/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */


package YaraParser.TransitionBasedSystem.Configuration;

import YaraParser.Accessories.Pair;
import YaraParser.Structures.Sentence;
import YaraParser.TransitionBasedSystem.Parser.Actions;
import YaraParser.TransitionBasedSystem.Parser.ArcEager;

import java.util.HashMap;
import java.util.HashSet;

public class GoldConfiguration {
    protected HashMap<Integer, Pair<Integer, Integer>> goldDependencies;
    protected HashMap<Integer, HashSet<Integer>> reversedDependencies;
    protected Sentence sentence;

    public GoldConfiguration(Sentence sentence, HashMap<Integer, Pair<Integer, Integer>> goldDependencies) {
        this.goldDependencies = new HashMap<Integer, Pair<Integer, Integer>>();
        reversedDependencies = new HashMap<Integer, HashSet<Integer>>();
        for (int head : goldDependencies.keySet())
            this.goldDependencies.put(head, goldDependencies.get(head).clone());
        for (int dependent : goldDependencies.keySet()) {
            int head = goldDependencies.get(dependent).first;
            if (!reversedDependencies.containsKey(head))
                reversedDependencies.put(head, new HashSet<Integer>());
            reversedDependencies.get(head).add(dependent);
        }
        this.sentence = sentence;
    }


    public Sentence getSentence() {
        return sentence;
    }

    public int head(int dependent) {
        if (!goldDependencies.containsKey(dependent))
            return -1;
        return goldDependencies.get(dependent).first;
    }

    public String relation(int dependent) {
        if (!goldDependencies.containsKey(dependent))
            return "_";
        return goldDependencies.get(dependent).second + "";
    }

    public HashMap<Integer, Pair<Integer, Integer>> getGoldDependencies() {
        return goldDependencies;
    }

    /**
     * Shows whether the tree to train is projective or not
     *
     * @return true if the tree is non-projective
     */
    public boolean isNonprojective() {
        for (int dep1 : goldDependencies.keySet()) {
            int head1 = goldDependencies.get(dep1).first;
            for (int dep2 : goldDependencies.keySet()) {
                int head2 = goldDependencies.get(dep2).first;
                if (head1 < 0 || head2 < 0)
                    continue;
                if (dep1 > head1 && head1 != head2)
                    if ((dep1 > head2 && dep1 < dep2 && head1 < head2) || (dep1 < head2 && dep1 > dep2 && head1 < dep2))
                        return true;
                if (dep1 < head1 && head1 != head2)
                    if ((head1 > head2 && head1 < dep2 && dep1 < head2) || (head1 < head2 && head1 > dep2 && dep1 < dep2))
                        return true;
            }
        }
        return false;
    }

    public boolean isPartial(boolean rootFirst) {
        for (int i = 0; i < sentence.size(); i++) {
            if (rootFirst || i < sentence.size() - 1) {
                if (!goldDependencies.containsKey(i + 1))
                    return true;
            }
        }
        return false;
    }

    public HashMap<Integer, HashSet<Integer>> getReversedDependencies() {
        return reversedDependencies;
    }

    /**
     * For the cost of an action given the gold dependencies
     * For more information see:
     * Yoav Goldberg and Joakim Nivre. "Training Deterministic Parsers with Non-Deterministic Oracles."
     * TACL 1 (2013): 403-414.
     *
     * @param action
     * @param dependency
     * @param state
     * @return oracle cost of the action
     * @throws Exception
     */
    public int actionCost(Actions action, int dependency, State state) throws Exception {
        if (!ArcEager.canDo(action, state))
            return Integer.MAX_VALUE;
        int cost = 0;

        // added by me to take care of labels
        if (action == Actions.LeftArc) { // left arc
            int bufferHead = state.bufferHead();
            int stackHead = state.peek();

            if (goldDependencies.containsKey(stackHead) && goldDependencies.get(stackHead).first == bufferHead
                    && goldDependencies.get(stackHead).second != (dependency))
                cost += 1;
        } else if (action == Actions.RightArc && cost == 0) { //right arc
            int bufferHead = state.bufferHead();
            int stackHead = state.peek();
            if (goldDependencies.containsKey(bufferHead) && goldDependencies.get(bufferHead).first == stackHead
                    && goldDependencies.get(bufferHead).second != (dependency))
                cost += 1;
        }

        if (action == Actions.Shift && cost == 0) { //shift
            int bufferHead = state.bufferHead();
            for (int stackItem : state.getStack()) {
                if (goldDependencies.containsKey(stackItem) && goldDependencies.get(stackItem).first == (bufferHead))
                    cost += 1;
                if (goldDependencies.containsKey(bufferHead) && goldDependencies.get(bufferHead).first == (stackItem))
                    cost += 1;
            }

        } else if (action == Actions.Reduce && cost == 0) { //reduce
            int stackHead = state.peek();
            if (!state.bufferEmpty())
                for (int bufferItem = state.bufferHead(); bufferItem <= state.maxSentenceSize; bufferItem++) {
                    if (goldDependencies.containsKey(bufferItem) && goldDependencies.get(bufferItem).first == (stackHead))
                        cost += 1;
                }
        } else if (action == Actions.LeftArc && cost == 0) { //left arc
            int stackHead = state.peek();
            if (!state.bufferEmpty())
                for (int bufferItem = state.bufferHead(); bufferItem <= state.maxSentenceSize; bufferItem++) {
                    if (goldDependencies.containsKey(bufferItem) && goldDependencies.get(bufferItem).first == (stackHead))
                        cost += 1;
                    if (goldDependencies.containsKey(stackHead) && goldDependencies.get(stackHead).first == (bufferItem))
                        if (bufferItem != state.bufferHead())
                            cost += 1;
                }
        } else if (action == Actions.RightArc && cost == 0) { //right arc
            int stackHead = state.peek();
            int bufferHead = state.bufferHead();
            for (int stackItem : state.getStack()) {
                if (goldDependencies.containsKey(bufferHead) && goldDependencies.get(bufferHead).first == (stackItem))
                    if (stackItem != stackHead)
                        cost += 1;

                if (goldDependencies.containsKey(stackItem) && goldDependencies.get(stackItem).first == (bufferHead))
                    cost += 1;
            }
            if (!state.bufferEmpty())
                for (int bufferItem = state.bufferHead(); bufferItem <= state.maxSentenceSize; bufferItem++) {
                    if (goldDependencies.containsKey(bufferHead) && goldDependencies.get(bufferHead).first == (bufferItem))
                        cost += 1;
                }
        }
        return cost;
    }
}
