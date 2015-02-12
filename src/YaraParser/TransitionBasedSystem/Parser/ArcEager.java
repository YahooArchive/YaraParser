/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package YaraParser.TransitionBasedSystem.Parser;

import YaraParser.TransitionBasedSystem.Configuration.Configuration;
import YaraParser.TransitionBasedSystem.Configuration.State;

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

    public static boolean canDo(Actions action, State state) {
        if (action == Actions.Shift) { //shift
            return !(!state.bufferEmpty() && state.bufferHead() == state.rootIndex && !state.stackEmpty()) && !state.bufferEmpty() && !state.isEmptyFlag();
        } else if (action == Actions.RightArc) { //right arc
            if (state.stackEmpty())
                return false;
            return !(!state.bufferEmpty() && state.bufferHead() == state.rootIndex) && !state.bufferEmpty() && !state.stackEmpty();

        } else if (action == Actions.LeftArc) { //left arc
            if (state.stackEmpty() || state.bufferEmpty())
                return false;

            if (!state.stackEmpty() && state.peek() == state.rootIndex)
                return false;

            return state.peek() != state.rootIndex && !state.hasHead(state.peek()) && !state.stackEmpty();
        } else if (action == Actions.Reduce) { //reduce
            return !state.stackEmpty() && state.hasHead(state.peek()) || !state.stackEmpty() && state.stackSize() == 1 && state.bufferSize() == 0 && state.peek() == state.rootIndex;
        } else if (action == Actions.Unshift) { //unshift
            return !state.stackEmpty() && !state.hasHead(state.peek()) && state.isEmptyFlag();
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
