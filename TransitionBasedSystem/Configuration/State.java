package TransitionBasedSystem.Configuration;

import Accessories.Pair;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.TreeSet;

/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/
public class State implements Comparable, Cloneable {
    /**
     * This is the additional information for the case of parsing with tree constraint
     * For more information see:
     * Joakim Nivre and Daniel Fernández-González. "Arc-Eager Parsing with the Tree Constraint."
     * Computational Linguistics(2014).
     */
    protected boolean emptyFlag;

    public int rootIndex;

    /**
     * Keeps dependent->head information
     */
    protected HashMap<Integer, Pair<Integer, String>> arcs;

    /**
     * Keeps head->{left_dependents,right_dependent} information
     */
    protected HashMap<Integer, Pair<TreeSet<Integer>, TreeSet<Integer>>> reversedArcs;
    protected ArrayDeque<Integer> stack;
    public  int maxSentenceSize;
    int bufferH;

    public State(int size) {
        emptyFlag = false;
        stack = new ArrayDeque<Integer>();
        arcs = new HashMap<Integer, Pair<Integer, String>>();
        reversedArcs = new HashMap<Integer, Pair<TreeSet<Integer>, TreeSet<Integer>>>();
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
        arcs = new HashMap<Integer, Pair<Integer, String>>(maxSentenceSize);
    }

    public ArrayDeque<Integer> getStack() {
        return stack;
    }

    public int pop() throws Exception {
        if (stack.size() == 0)
            throw new Exception("stack is empty");
        return stack.pop();
    }

    public void push(int index) {
        stack.push(index);
    }

    public void addArc(int dependent, int head, String dependency) {
        if (dependent == head) {
            System.out.print("DEBUG!");
        }
        arcs.put(dependent, new Pair<Integer, String>(head, dependency));
        if (!reversedArcs.containsKey(head))
            reversedArcs.put(head, new Pair<TreeSet<Integer>, TreeSet<Integer>>(new TreeSet<Integer>(), new TreeSet<Integer>()));
        if (dependent > head) // right modifier
            reversedArcs.get(head).second.add(dependent);
        else // left modifier
            reversedArcs.get(head).first.add(dependent);

    }

    public TreeSet<String> rightDependentLabels(int position) {
        TreeSet<String> deps = new TreeSet<String>();
        if (reversedArcs.containsKey(position)) {
            for (int dep : reversedArcs.get(position).second)
                deps.add(arcs.get(dep).second);
        }
        return deps;
    }

    public TreeSet<String> leftDependentLabels(int position) {
        TreeSet<String> deps = new TreeSet<String>();
        if (reversedArcs.containsKey(position)) {
            for (int dep : reversedArcs.get(position).first)
                deps.add(arcs.get(dep).second);
        }
        return deps;
    }

    public boolean isEmptyFlag() {
        return emptyFlag;
    }

    public void setEmptyFlag(boolean emptyFlag) {
        this.emptyFlag = emptyFlag;
    }

    public int bufferHead() throws Exception {
        if (bufferH == -1)
            throw new Exception("buffer is empty!");
        return bufferH;
    }

    public int peek() throws Exception {
        if (stack.size() == 0)
            throw new Exception("stack is empty!");
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
        if (stack.size()==0 && bufferH == rootIndex)
            return true;
        return false;
    }

    public boolean hasHead(int dependent) {
        if (arcs.containsKey(dependent))
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
        if (reversedArcs.containsKey(index) && reversedArcs.get(index).second.size() > 0) {
            int rightMost = reversedArcs.get(index).second.last();
            if (rightMost > index)
                return rightMost;
        }
        return -1;
    }

    public int leftMostModifier(int index) {
        if (reversedArcs.containsKey(index) && reversedArcs.get(index).first.size() > 0) {
            int leftMost = reversedArcs.get(index).first.first();
            if (leftMost < index)
                return leftMost;
        }
        return -1;
    }

    /**
     * @param head
     * @return the current number of dependents
     */
    public int valence(int head) {
        if (reversedArcs.containsKey(head))
            return reversedArcs.get(head).first.size() + reversedArcs.get(head).second.size();
        return 0;
    }

    /**
     * @param head
     * @return the current number of right modifiers
     */
    public int rightValency(int head) {
        if (reversedArcs.containsKey(head))
            return reversedArcs.get(head).second.size();
        return 0;
    }

    /**
     * @param head
     * @return the current number of left modifiers
     */
    public int leftValency(int head) {
        if (reversedArcs.containsKey(head))
            return reversedArcs.get(head).first.size();
        return 0;
    }

    public int getHead(int index) {
        if (arcs.containsKey(index))
            return arcs.get(index).first;
        return -1;
    }

    public String getDependency(int index) {
        if (arcs.containsKey(index))
            return arcs.get(index).second;
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
            if (state.stack.peek()!=stack.peek())
                return false;
            if (maxSentenceSize != state.maxSentenceSize || rootIndex != state.rootIndex || bufferH != state.bufferH)
                return false;
            for (int dependent : arcs.keySet())
                if (!state.arcs.containsKey(dependent) || !state.arcs.get(dependent).equals(arcs.get(dependent)))
                    return false;

            return true;
        }
        return false;
    }

    @Override
    public State clone() {
        State state = new State(maxSentenceSize);
        state.stack=new ArrayDeque<Integer>(stack);
        for (int dependent : arcs.keySet()) {
            Pair<Integer, String> head = arcs.get(dependent);
            state.arcs.put(dependent, head);
            if (!state.reversedArcs.containsKey(head.first))
                state.reversedArcs.put(head.first, new Pair<TreeSet<Integer>, TreeSet<Integer>>(new TreeSet<Integer>(), new TreeSet<Integer>()));
            if (dependent > head.first) // right modifier
                state.reversedArcs.get(head.first).second.add(dependent);
            else // left modifier
                state.reversedArcs.get(head.first).first.add(dependent);
        }
        state.rootIndex = rootIndex;
        state.bufferH = bufferH;
        state.maxSentenceSize = maxSentenceSize;
        state.emptyFlag = emptyFlag;
        return state;
    }

    @Override
    public int hashCode() {

        int hashCode = stack.peek()*bufferH;
       // for (int s : stack)
        //    hashCode += s;

        for (int dep : arcs.keySet()) {
            Pair<Integer,String> pair=arcs.get(dep);
            hashCode += dep * (pair.first + pair.second.hashCode());
        }

        return hashCode ;
    }
}
