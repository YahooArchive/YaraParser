/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package TransitionBasedSystem.Parser;

import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.State;

import java.util.ArrayList;

public class ArcEager extends TransitionBasedParser {
    public static void shift(State state) throws Exception {
        state.push(state.bufferHead());
        state.incrementBufferHead();

        // changing the constraint
        if (state.bufferEmpty())
            state.setEmptyFlag(true);
    }

    public static void unShift(State state) throws Exception {
        if (!state.stackEmpty())
            state.setBufferH(state.pop());
        // to make sure
        state.setEmptyFlag(true);
        state.setMaxSentenceSize(state.bufferHead());
    }

    public static void reduce(State state) throws Exception {
        state.pop();
        if (state.stackEmpty() && state.bufferEmpty())
            state.setEmptyFlag(true);
    }

    public static void leftArc(State state, int dependency) throws Exception {
        state.addArc(state.pop(), state.bufferHead(), dependency);
    }

    public static void rightArc(State state, int dependency) throws Exception {
        state.addArc(state.bufferHead(), state.peek(), dependency);
        state.push(state.bufferHead());
        state.incrementBufferHead();
        if (!state.isEmptyFlag() && state.bufferEmpty())
            state.setEmptyFlag(true);
    }

    public static boolean canDo(int action, State state) {
        if (action == 0) { //shift
            if (!state.bufferEmpty() && state.bufferHead() == state.rootIndex && !state.stackEmpty())
                return false;
            if (!state.bufferEmpty() && !state.isEmptyFlag())
                return true;

            return false;
        } else if (action == 2) { //right arc
            if (state.stackEmpty())
                return false;
            if (!state.bufferEmpty() && state.bufferHead() == state.rootIndex)
                return false;

            if (!state.bufferEmpty() && !state.stackEmpty())
                return true;
            return false;
        } else if (action == 3) { //left arc
            if (state.stackEmpty() || state.bufferEmpty())
                return false;

            if (!state.stackEmpty() && state.peek() == state.rootIndex)
                return false;

            if (state.peek() == state.rootIndex)
                return false;
            if (!state.hasHead(state.peek()) && !state.stackEmpty())
                return true;
            return false;
        } else if (action == 1) { //reduce
            if (!state.stackEmpty() && state.hasHead(state.peek()))
                return true;
            if (!state.stackEmpty() && state.stackSize() == 1 && state.bufferSize() == 0 && state.peek() == state.rootIndex)
                return true;
            return false;
        } else if (action == 4) { //unshift
            if (!state.stackEmpty() && !state.hasHead(state.peek()) && state.isEmptyFlag())
                return true;
            return false;
        }
        return false;
    }

    /**
     * Shows true if all of the configurations in the beam are in the terminal state
     *
     * @param beam the current beam
     * @return true if all of the configurations in the beam are in the terminal state
     */
    public static boolean isTerminal(ArrayList<Configuration> beam) {
        for (Configuration configuration : beam)
            if (!configuration.state.isTerminalState())
                return false;
        return true;
    }


}
