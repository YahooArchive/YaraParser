/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project 0 for terms.
 */

package TransitionBasedSystem.Features;

import Structures.Sentence;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.State;

public class FeatureExtractor {
    /**
     * Given a list of templates, extracts all features for the given state
     *
     * @param configuration
     * @return
     * @throws Exception
     */
    public static long[] extractAllParseFeatures(Configuration configuration, int length) throws Exception {
        if (length == 26)
            return extractBasicFeatures(configuration, length);
        else
            return extractExtendedFeatures(configuration, length);
    }

    /**
     * Given a list of templates, extracts all features for the given state
     *
     * @param configuration
     * @return
     * @throws Exception
     */
    private static long[] extractExtendedFeatures(Configuration configuration, int length) throws Exception {
        long[] featureMap = new long[length];

        State state = configuration.state;
        Sentence sentence = configuration.sentence;

        int b0Position = 0;
        int b1Position = 0;
        int b2Position = 0;
        int s0Position = 0;

        int svr = 0; // stack right valency
        int svl = 0; // stack left valency
        int bvl = 0; // buffer left valency

        long b0w = 0;
        long b0p = 0;

        long b1w = 0;
        long b1p = 0;

        long b2w = 0;
        long b2p = 0;

        long s0w = 0;
        long s0p = 0;
        long s0l = 0;

        long bl0p = 0;
        long bl0w = 0;
        long bl0l = 0;

        long bl1w = 0;
        long bl1p = 0;
        long bl1l = 0;

        long sr0p = 0;
        long sr0w = 0;
        long sr0l = 0;

        long sh0w = 0;
        long sh0p = 0;
        long sh0l = 0;

        long sl0p = 0;
        long sl0w = 0;
        long sl0l = 0;

        long sr1w = 0;
        long sr1p = 0;
        long sr1l = 0;

        long sh1w = 0;
        long sh1p = 0;

        long sl1w = 0;
        long sl1p = 0;
        long sl1l = 0;

        long sdl = 0;
        long sdr = 0;
        long bdl = 0;

        int[] words = sentence.getWords();
        int[] tags = sentence.getTags();

        if (0 < state.bufferSize()) {
            b0Position = state.bufferHead();
            b0w = b0Position == 0 ? 0 : words[b0Position - 1];
            b0w += 2;
            b0p = b0Position == 0 ? 0 : tags[b0Position - 1];
            b0p += 2;
            bvl = state.leftValency(b0Position);

            int leftMost = state.leftMostModifier(state.getBufferItem(0));
            if (leftMost >= 0) {
                bl0p = leftMost == 0 ? 0 : tags[leftMost - 1];
                bl0p += 2;
                bl0w = leftMost == 0 ? 0 : words[leftMost - 1];
                bl0w += 2;
                bl0l = state.getDependency(leftMost);
                bl0l += 2;

                int l2 = state.leftMostModifier(leftMost);
                if (l2 >= 0) {
                    bl1w = l2 == 0 ? 0 : words[l2 - 1];
                    bl1w += 2;
                    bl1p = l2 == 0 ? 0 : tags[l2 - 1];
                    bl1p += 2;
                    bl1l = state.getDependency(l2);
                    bl1l += 2;
                }
            }

            if (1 < state.bufferSize()) {
                b1Position = state.getBufferItem(1);
                b1w = b1Position == 0 ? 0 : words[b1Position - 1];
                b1w += 2;
                b1p = b1Position == 0 ? 0 : tags[b1Position - 1];
                b1p += 2;

                if (2 < state.bufferSize()) {
                    b2Position = state.getBufferItem(2);

                    b2w = b2Position == 0 ? 0 : words[b2Position - 1];
                    b2w += 2;
                    b2p = b2Position == 0 ? 0 : tags[b2Position - 1];
                    b2p += 2;
                }
            }
        }

        if (0 < state.stackSize()) {
            s0Position = state.peek();
            s0w = s0Position == 0 ? 0 : words[s0Position - 1];
            s0w += 2;
            s0p = s0Position == 0 ? 0 : tags[s0Position - 1];
            s0p += 2;
            s0l = state.getDependency(s0Position);
            s0l += 2;

            svl = state.leftValency(s0Position);
            svr = state.rightValency(s0Position);

            int leftMost = state.leftMostModifier(s0Position);
            if (leftMost >= 0) {
                sl0p = leftMost == 0 ? 0 : tags[leftMost - 1];
                sl0p += 2;
                sl0w = leftMost == 0 ? 0 : words[leftMost - 1];
                sl0w += 2;
                sl0l = state.getDependency(leftMost);
                sl0l += 2;
            }

            int rightMost = state.rightMostModifier(s0Position);
            if (rightMost >= 0) {
                sr0p = rightMost == 0 ? 0 : tags[rightMost - 1];
                sr0p += 2;
                sr0w = rightMost == 0 ? 0 : words[rightMost - 1];
                sr0w += 2;
                sr0l = state.getDependency(rightMost);
                sr0l += 2;
            }

            int headIndex = state.getHead(s0Position);
            if (headIndex >= 0) {
                sh0w = headIndex == 0 ? 0 : words[headIndex - 1];
                sh0w += 2;
                sh0p = headIndex == 0 ? 0 : tags[headIndex - 1];
                sh0p += 2;
                sh0l = state.getDependency(headIndex);
                sh0l += 2;
            }

            if (leftMost >= 0) {
                int l2 = state.leftMostModifier(leftMost);
                if (l2 >= 0) {
                    sl1w = l2 == 0 ? 0 : words[l2 - 1];
                    sl1w += 2;
                    sl1p = l2 == 0 ? 0 : tags[l2 - 1];
                    sl1p += 2;
                    sl1l = state.getDependency(l2);
                    sl1l += 2;
                }
            }
            if (headIndex >= 0) {
                if (state.hasHead(headIndex)) {
                    int h2 = state.getHead(headIndex);
                    sh1w = h2 == 0 ? 0 : words[h2 - 1];
                    sh1w += 2;
                    sh1p = h2 == 0 ? 0 : tags[h2 - 1];
                    sh1p += 2;
                }
            }
            if (rightMost >= 0) {
                int r2 = state.rightMostModifier(rightMost);
                if (r2 >= 0) {
                    sr1w = r2 == 0 ? 0 : words[r2 - 1];
                    sr1w += 2;
                    sr1p = r2 == 0 ? 0 : tags[r2 - 1];
                    sr1p += 2;
                    sr1l = state.getDependency(r2);
                    sr1l += 2;
                }
            }
        }
        int index = 0;

        long b0wp = b0p;
        b0wp |= (b0w << 8);
        long b1wp = b1p;
        b1wp |= (b1w << 8);
        long s0wp = s0p;
        s0wp |= (s0w << 8);
        long b2wp = b2p;
        b2wp |= (b2w << 8);

        /**
         * From single words
         */
        featureMap[index++] = s0wp;
        featureMap[index++] = s0w;
        featureMap[index++] = s0p;
        featureMap[index++] = b0wp;
        featureMap[index++] = b0w;
        featureMap[index++] = b0p;
        featureMap[index++] = b1wp;
        featureMap[index++] = b1w;
        featureMap[index++] = b1p;
        featureMap[index++] = b2wp;
        featureMap[index++] = b2w;
        featureMap[index++] = b2p;

        /**
         * from word pairs
         */
        featureMap[index++] = (s0wp << 28) | b0wp;
        featureMap[index++] = (s0wp << 20) | b0w;
        featureMap[index++] = (s0w << 28) | b0wp;
        featureMap[index++] = (s0wp << 8) | b0p;
        featureMap[index++] = (s0p << 28) | b0wp;
        featureMap[index++] = (s0w << 20) | b0w;
        featureMap[index++] = (s0p << 8) | b0p;
        featureMap[index++] = (b0p << 8) | b1p;

        /**
         * from three words
         */
        featureMap[index++] = (b0p << 16) | (b1p << 8) | b2p;
        featureMap[index++] = (s0p << 16) | (b0p << 8) | b1p;
        featureMap[index++] = (sh0p << 16) | (s0p << 8) | b0p;
        featureMap[index++] = (s0p << 16) | (sl0p << 8) | b0p;
        featureMap[index++] = (s0p << 16) | (sr0p << 8) | b0p;
        featureMap[index++] = (s0p << 16) | (b0p << 8) | bl0p;

        /**
         * distance
         */
        int distance = 0;
        if (s0Position > 0 && b0Position > 0)
            distance = Math.abs(b0Position - s0Position);
        featureMap[index++] = s0w | (distance << 20);
        featureMap[index++] = s0p | (distance << 8);
        featureMap[index++] = b0w | (distance << 20);
        featureMap[index++] = b0p | (distance << 8);
        featureMap[index++] = s0w | (b0w << 20) | (distance << 40);
        featureMap[index++] = s0p | (b0p << 8) | (distance << 28);

        /**
         * Valency information
         */
        featureMap[index++] = s0w | (svr << 20);
        featureMap[index++] = s0p | (svr << 8);
        featureMap[index++] = s0w | (svl << 20);
        featureMap[index++] = s0p | (svl << 8);
        featureMap[index++] = b0w | (bvl << 20);
        featureMap[index++] = b0p | (bvl << 8);

        /**
         * Unigrams
         */
        featureMap[index++] = sh0w;
        featureMap[index++] = sh0p;
        featureMap[index++] = s0l;
        featureMap[index++] = sl0w;
        featureMap[index++] = sl0p;
        featureMap[index++] = sl0l;
        featureMap[index++] = sr0w;
        featureMap[index++] = sr0p;
        featureMap[index++] = sr0l;
        featureMap[index++] = bl0w;
        featureMap[index++] = bl0p;
        featureMap[index++] = bl0l;

        /**
         * From third order features
         */
        featureMap[index++] = sh1w;
        featureMap[index++] = sh1p;
        featureMap[index++] = sh0l;
        featureMap[index++] = sl1w;
        featureMap[index++] = sl1p;
        featureMap[index++] = sl1l;
        featureMap[index++] = sr1w;
        featureMap[index++] = sr1p;
        featureMap[index++] = sr1l;
        featureMap[index++] = bl1w;
        featureMap[index++] = bl1p;
        featureMap[index++] = bl1l;
        featureMap[index++] = s0p | (sl0p << 8) | (sl1p << 16);
        featureMap[index++] = s0p | (sr0p << 8) | (sr1p << 16);
        featureMap[index++] = s0p | (sh0p << 8) | (sh1p << 16);
        featureMap[index++] = b0p | (bl0p << 8) | (bl1p << 16);

        /**
         * label set
         */
        if (s0Position >= 0) {
            sdl = state.leftDependentLabels(s0Position);
            sdr = state.rightDependentLabels(s0Position);
        }

        if (b0Position >= 0) {
            bdl = state.leftDependentLabels(b0Position);
        }

        featureMap[index++] = s0w | (sdr << 20);
        featureMap[index++] = s0p | (sdr << 8);
        featureMap[index++] = s0w | (sdl << 20);
        featureMap[index++] = s0p | (sdl << 8);
        featureMap[index++] = b0w | (bdl << 20);
        featureMap[index++] = b0p | (bdl << 8);
        return featureMap;
    }

    /**
     * Given a list of templates, extracts all features for the given state
     *
     * @param configuration
     * @return
     * @throws Exception
     */
    private static long[] extractBasicFeatures(Configuration configuration, int length) throws Exception {
        long[] featureMap = new long[length];

        State state = configuration.state;
        Sentence sentence = configuration.sentence;

        int b0Position = 0;
        int b1Position = 0;
        int b2Position = 0;
        int s0Position = 0;

        long b0w = 0;
        long b0p = 0;

        long b1w = 0;
        long b1p = 0;

        long b2w = 0;
        long b2p = 0;

        long s0w = 0;
        long s0p = 0;
        long bl0p = 0;
        long sr0p = 0;
        long sh0p = 0;

        long sl0p = 0;

        int[] words = sentence.getWords();
        int[] tags = sentence.getTags();

        if (0 < state.bufferSize()) {
            b0Position = state.bufferHead();
            b0w = b0Position == 0 ? 0 : words[b0Position - 1];
            b0w += 2;
            b0p = b0Position == 0 ? 0 : tags[b0Position - 1];
            b0p += 2;

            int leftMost = state.leftMostModifier(state.getBufferItem(0));
            if (leftMost >= 0) {
                bl0p = leftMost == 0 ? 0 : tags[leftMost - 1];
                bl0p += 2;
            }

            if (1 < state.bufferSize()) {
                b1Position = state.getBufferItem(1);
                b1w = b1Position == 0 ? 0 : words[b1Position - 1];
                b1w += 2;
                b1p = b1Position == 0 ? 0 : tags[b1Position - 1];
                b1p += 2;

                if (2 < state.bufferSize()) {
                    b2Position = state.getBufferItem(2);

                    b2w = b2Position == 0 ? 0 : words[b2Position - 1];
                    b2w += 2;
                    b2p = b2Position == 0 ? 0 : tags[b2Position - 1];
                    b2p += 2;
                }
            }
        }


        if (0 < state.stackSize()) {
            s0Position = state.peek();
            s0w = s0Position == 0 ? 0 : words[s0Position - 1];
            s0w += 2;
            s0p = s0Position == 0 ? 0 : tags[s0Position - 1];
            s0p += 2;

            int leftMost = state.leftMostModifier(s0Position);
            if (leftMost >= 0) {
                sl0p = leftMost == 0 ? 0 : tags[leftMost - 1];
                sl0p += 2;
            }

            int rightMost = state.rightMostModifier(s0Position);
            if (rightMost >= 0) {
                sr0p = rightMost == 0 ? 0 : tags[rightMost - 1];
                sr0p += 2;
            }

            int headIndex = state.getHead(s0Position);
            if (headIndex >= 0) {
                sh0p = headIndex == 0 ? 0 : tags[headIndex - 1];
                sh0p += 2;
            }

        }
        int index = 0;

        long b0wp = b0p;
        b0wp |= (b0w << 8);
        long b1wp = b1p;
        b1wp |= (b1w << 8);
        long s0wp = s0p;
        s0wp |= (s0w << 8);
        long b2wp = b2p;
        b2wp |= (b2w << 8);

        /**
         * From single words
         */
        featureMap[index++] = s0wp;
        featureMap[index++] = s0w;
        featureMap[index++] = s0p;
        featureMap[index++] = b0wp;
        featureMap[index++] = b0w;
        featureMap[index++] = b0p;

        featureMap[index++] = b1wp;
        featureMap[index++] = b1w;
        featureMap[index++] = b1p;
        featureMap[index++] = b2wp;
        featureMap[index++] = b2w;
        featureMap[index++] = b2p;

        /**
         * from word pairs
         */
        featureMap[index++] = (s0wp << 28) | b0wp;
        featureMap[index++] = (s0wp << 20) | b0w;
        featureMap[index++] = (s0w << 28) | b0wp;
        featureMap[index++] = (s0wp << 8) | b0p;
        featureMap[index++] = (s0p << 28) | b0wp;
        featureMap[index++] = (s0w << 20) | b0w;
        featureMap[index++] = (s0p << 8) | b0p;
        featureMap[index++] = (b0p << 8) | b1p;

        /**
         * from three words
         */
        featureMap[index++] = (b0p << 16) | (b1p << 8) | b2p;
        featureMap[index++] = (s0p << 16) | (b0p << 8) | b1p;
        featureMap[index++] = (sh0p << 16) | (s0p << 8) | b0p;
        featureMap[index++] = (s0p << 16) | (sl0p << 8) | b0p;
        featureMap[index++] = (s0p << 16) | (sr0p << 8) | b0p;
        featureMap[index++] = (s0p << 16) | (b0p << 8) | bl0p;
        return featureMap;
    }
}
