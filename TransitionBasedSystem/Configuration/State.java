/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package TransitionBasedSystem.Configuration;

import Accessories.Pair;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;

public class State implements Comparable, Cloneable {
    public static HashMap<String, Integer> labelMap = new HashMap<String, Integer>();
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
    protected Pair<Integer, String>[] arcs;
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
        arcs = new Pair[size+1];

        leftMostArcs =new int[size + 1];
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

    public void addArc(int dependent, int head, String dependency) {
        arcs[dependent]= new Pair<Integer, String>(head, dependency);

        int depIndex = labelMap.get(dependency);
        long value = 1L << depIndex;

        if (dependent > head) { //right dep
            if (rightMostArcs[head]==0 || dependent > rightMostArcs[head])
                rightMostArcs[head]= dependent;
            rightValency[head] += 1;
            rightDepLabels[head] = rightDepLabels[head] | value;

        } else { //left dependency
              if (leftMostArcs[head]==0 || dependent > leftMostArcs[head])
                  leftMostArcs[head]= dependent;
            leftDepLabels[head] = leftDepLabels[head] | value;
            leftValency[head] += 1;
        }
    }

    public String rightDependentLabels(int position) {
        return rightValency[position] + "";
    }

    public String leftDependentLabels(int position) {
        return leftDepLabels[position] + "";
    }

    public boolean isEmptyFlag() {
        return emptyFlag;
    }

    public void setEmptyFlag(boolean emptyFlag) {
        this.emptyFlag = emptyFlag;
    }

    public int bufferHead() throws Exception {
        return bufferH;
    }

    public int peek() throws Exception {
        return stack.peek();
    }

    public int getBufferItem(int position) throws Exception {
        if (position > maxSentenceSize)
            throw new Exception("index out of bound for getting an item from the buffer");

        return bufferH + position;
    }

    public boolean isTerminalState() {
        if (bufferEmpty() && stackEmpty())
            return true;
        if (stack.size() == 0 && bufferH == rootIndex)
            return true;
        return false;
    }

    public boolean hasHead(int dependent) {
        if (arcs[dependent]!=null)
            return true;
        return false;
    }

    public boolean bufferEmpty() {
        if (bufferH == -1)
            return true;
        return false;
    }

    public boolean stackEmpty() {
        if (stack.size() == 0)
            return true;
        return false;
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
      return (rightMostArcs[index]==0?-1:rightMostArcs[index]);
    }

    public int leftMostModifier(int index) {
        return (leftMostArcs[index]==0?-1:leftMostArcs[index]);
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
        if (arcs[index]!=null)
            return arcs[index].first;
        return -1;
    }

    public String getDependency(int index) {
        if (arcs[index]!=null)
            return arcs[index].second;
        return "_";
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
    public int compareTo(Object o) {
        if (equals(o))
            return 0;

        return hashCode() - o.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof State) {
            State state = (State) o;
            if (state.stack.peek() != stack.peek())
                return false;
            if (maxSentenceSize != state.maxSentenceSize || rootIndex != state.rootIndex || bufferH != state.bufferH)
                return false;
            for (int dependent=0;dependent<arcs.length;dependent++) {
                if(arcs[dependent]!=null) {
                    if (state.arcs[dependent]==null || !state.arcs[dependent].equals(arcs[dependent]))
                        return false;
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public State clone() {
        State state = new State(maxSentenceSize);
        state.stack = new ArrayDeque<Integer>(stack);
/*
        state.arcs= arcs.clone();
        state.rightMostArcs=rightMostArcs.clone();
        state.leftMostArcs=leftMostArcs.clone();
        state.leftValency=leftValency.clone();
        state.rightValency=rightValency.clone();
        state.leftDepLabels=leftDepLabels.clone();
        state.rightDepLabels=rightDepLabels.clone();
*/


        for (int dependent=0;dependent<arcs.length;dependent++) {
            if (arcs[dependent] != null) {
                Pair<Integer, String> head = arcs[dependent];
                state.arcs[dependent]= head;
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

    @Override
    public int hashCode() {
        int hashCode = stack.peek() * bufferH;

        for (int dependent=0;dependent<arcs.length;dependent++) {
            if (arcs[dependent] != null) {
                Pair<Integer, String> pair = arcs[dependent];
                hashCode += dependent * (pair.first + pair.second.hashCode());
            }
        }

        return hashCode;
    }
}
