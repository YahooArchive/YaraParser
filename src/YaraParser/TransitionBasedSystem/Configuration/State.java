/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package YaraParser.TransitionBasedSystem.Configuration;

import YaraParser.Accessories.Pair;

import java.util.ArrayDeque;

public class State implements Cloneable {
    public int rootIndex;
    public int maxSentenceSize;

    /**
     * This is the additional information for the case of parsing with tree constraint
     * For more information see:
     * Joakim Nivre and Daniel FernÃ¡ndez-GonzÃ¡lez. "Arc-Eager Parsing with the Tree Constraint."
     * Computational Linguistics(2014).
     */
    protected boolean emptyFlag;

    /**
     * Keeps dependent->head information
     */
    protected Pair<Integer, Integer>[] arcs;
    protected int[] leftMostArcs;
    protected int[] rightMostArcs;
    protected int[] leftValency;
    protected int[] rightValency;
    protected long[] rightDepLabels;
    protected long[] leftDepLabels;
    protected ArrayDeque<Integer> stack;
    int bufferH;

    public State(int size) {
        emptyFlag = false;
        stack = new ArrayDeque<Integer>();
        arcs = new Pair[size + 1];

        leftMostArcs = new int[size + 1];
        rightMostArcs = new int[size + 1];
        leftValency = new int[size + 1];
        rightValency = new int[size + 1];
        rightDepLabels = new long[size + 1];
        leftDepLabels = new long[size + 1];

        rootIndex = 0;
        bufferH = 1;
        maxSentenceSize = 0;
    }

    /**
     * @param sentenceSize
     * @param rootFirst    if true, the ROOT token will be the first token, otherwise it will be the last one!
     */
    public State(int sentenceSize, boolean rootFirst) {
        this(sentenceSize);
        if (rootFirst) {
            stack.push(0);
            rootIndex = 0;
            maxSentenceSize = sentenceSize;
        } else {
            rootIndex = sentenceSize;
            maxSentenceSize = sentenceSize;
        }
    }

    public ArrayDeque<Integer> getStack() {
        return stack;
    }

    public int pop() throws Exception {
        return stack.pop();
    }

    public void push(int index) {
        stack.push(index);
    }

    public void addArc(int dependent, int head, int dependency) {
        arcs[dependent] = new Pair<Integer, Integer>(head, dependency);
        long value = 1L << (dependency);
        
        assert dependency<64;

        if (dependent > head) { //right dep
            if (rightMostArcs[head] == 0 || dependent > rightMostArcs[head])
                rightMostArcs[head] = dependent;
            rightValency[head] += 1;
            rightDepLabels[head] = rightDepLabels[head] | value;

        } else { //left dependency
            if (leftMostArcs[head] == 0 || dependent < leftMostArcs[head])
                leftMostArcs[head] = dependent;
            leftDepLabels[head] = leftDepLabels[head] | value;
            leftValency[head] += 1;
        }
    }

    public long rightDependentLabels(int position) {
        return rightDepLabels[position];
    }

    public long leftDependentLabels(int position) {
        return leftDepLabels[position];
    }

    public boolean isEmptyFlag() {
        return emptyFlag;
    }

    public void setEmptyFlag(boolean emptyFlag) {
        this.emptyFlag = emptyFlag;
    }

    public int bufferHead() {
        return bufferH;
    }

    public int peek() {
        if (stack.size() > 0)
            return stack.peek();
        return -1;
    }

    public int getBufferItem(int position) {
        return bufferH + position;
    }

    public boolean isTerminalState() {
        return bufferEmpty() && stackEmpty() || stack.size() == 0 && bufferH == rootIndex;
    }

    public boolean hasHead(int dependent) {
        return arcs[dependent] != null;
    }

    public boolean bufferEmpty() {
        return bufferH == -1;
    }

    public boolean stackEmpty() {
        return stack.size() == 0;
    }

    public int bufferSize() {
        if (bufferH < 0)
            return 0;
        return (maxSentenceSize - bufferH + 1);
    }

    public int stackSize() {
        return stack.size();
    }

    public int rightMostModifier(int index) {
        return (rightMostArcs[index] == 0 ? -1 : rightMostArcs[index]);
    }

    public int leftMostModifier(int index) {
        return (leftMostArcs[index] == 0 ? -1 : leftMostArcs[index]);
    }

    /**
     * @param head
     * @return the current number of dependents
     */
    public int valence(int head) {
        return rightValency(head) + leftValency(head);
    }

    /**
     * @param head
     * @return the current number of right modifiers
     */
    public int rightValency(int head) {
        return rightValency[head];
    }

    /**
     * @param head
     * @return the current number of left modifiers
     */
    public int leftValency(int head) {
        return leftValency[head];
    }

    public int getHead(int index) {
        if (arcs[index] != null)
            return arcs[index].first;
        return -1;
    }

    public int getDependency(int index) {
        if (arcs[index] != null)
            return arcs[index].second;
        return -1;
    }

    public void setMaxSentenceSize(int maxSentenceSize) {
        this.maxSentenceSize = maxSentenceSize;
    }

    public void incrementBufferHead() {
        if (bufferH == maxSentenceSize)
            bufferH = -1;
        else
            bufferH++;
    }

    public void setBufferH(int bufferH) {
        this.bufferH = bufferH;
    }

    @Override
    public State clone() {
        State state = new State(arcs.length - 1);
        state.stack = new ArrayDeque<Integer>(stack);

        for (int dependent = 0; dependent < arcs.length; dependent++) {
            if (arcs[dependent] != null) {
                Pair<Integer, Integer> head = arcs[dependent];
                state.arcs[dependent] = head;
                int h = head.first;

                if (rightMostArcs[h] != 0) {
                    state.rightMostArcs[h] = rightMostArcs[h];
                    state.rightValency[h] = rightValency[h];
                    state.rightDepLabels[h] = rightDepLabels[h];
                }

                if (leftMostArcs[h] != 0) {
                    state.leftMostArcs[h] = leftMostArcs[h];
                    state.leftValency[h] = leftValency[h];
                    state.leftDepLabels[h] = leftDepLabels[h];
                }
            }
        }
        state.rootIndex = rootIndex;
        state.bufferH = bufferH;
        state.maxSentenceSize = maxSentenceSize;
        state.emptyFlag = emptyFlag;
        return state;
    }
}
