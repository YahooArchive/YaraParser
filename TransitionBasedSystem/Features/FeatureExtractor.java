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
    public static String[] extractAllParseFeatures(Configuration configuration, int length) throws Exception {
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
    private static String[] extractExtendedFeatures(Configuration configuration, int length) throws Exception {
        String[] featureMap = new String[length];

        State state = configuration.state;
        Sentence sentence = configuration.sentence;

        boolean basic = false;

        int b0Position = 0;
        int b1Position = 0;
        int b2Position = 0;
        int s0Position = 0;

        int svr = 0; // stack right valency
        int svl = 0; // stack left valency
        int bvl = 0; // buffer left valency

        String b0w = "";
        String b0p = "";
        String b0wp = "";

        String b1w = "";
        String b1p = "";
        String b1wp = "";

        String b2w = "";
        String b2p = "";
        String b2wp = "";

        String s0w = "";
        String s0p = "";
        String s0wp = "";
        String s0l = "";

        String bl0p = "";
        String bl0w = "";
        String bl0l = "";

        String bl1w = "";
        String bl1p = "";
        String bl1l = "";

        String sr0p = "";
        String sr0w = "";
        String sr0l = "";

        String sh0w = "";
        String sh0p = "";
        String sh0l = "";

        String sl0p = "";
        String sl0w = "";
        String sl0l = "";

        String sr1w = "";
        String sr1p = "";
        String sr1l = "";

        String sh1w = "";
        String sh1p = "";

        String sl1w = "";
        String sl1p = "";
        String sl1l = "";

        String sdl = null;
        String sdr = null;
        String bdl = null;

        String[] words = sentence.getWordStr();
        String[] tags = sentence.getTagStr();

        if (0 < state.bufferSize()) {
            b0Position = state.bufferHead();
            b0w = b0Position == 0 ? "0" : words[b0Position - 1];
            b0p = b0Position == 0 ? "0" : tags[b0Position - 1];
            b0wp = b0w + "_" + b0p;
            bvl = state.leftValency(b0Position);

            int leftMost = state.leftMostModifier(state.getBufferItem(0));
            if (leftMost >= 0) {
                bl0p = leftMost == 0 ? "0" : tags[leftMost - 1];
                bl0w = leftMost == 0 ? "0" : words[leftMost - 1];
                bl0l = state.getDependency(leftMost);

                int l2 = state.leftMostModifier(leftMost);
                if (l2 >= 0) {
                    bl1w = l2 == 0 ? "0" : words[l2 - 1];
                    bl1p = l2 == 0 ? "0" : tags[l2 - 1];
                    bl1l = state.getDependency(l2);
                }
            }

            if (1 < state.bufferSize()) {
                b1Position = state.getBufferItem(1);
                b1w = b1Position == 0 ? "0" : words[b1Position - 1];
                b1p = b1Position == 0 ? "0" : tags[b1Position - 1];
                b1wp = b1w + "_" + b1p;

                if (2 < state.bufferSize()) {
                    b2Position = state.getBufferItem(2);

                    b2w = b2Position == 0 ? "0" : words[b2Position - 1];
                    b2p = b2Position == 0 ? "0" : tags[b2Position - 1];
                    b2wp = b2w + "_" + b2p;
                }
            }
        }


        if (0 < state.stackSize()) {
            s0Position = state.peek();
            s0w = s0Position == 0 ? "0" : words[s0Position - 1];
            s0p = s0Position == 0 ? "0" : tags[s0Position - 1];
            s0wp = s0w + "_" + s0p;
            s0l = state.getDependency(s0Position);
            svl = state.leftValency(s0Position);
            svr = state.rightValency(s0Position);

            int leftMost = state.leftMostModifier(s0Position);
            if (leftMost >= 0) {
                sl0p = leftMost == 0 ? "0" : tags[leftMost - 1];
                sl0w = leftMost == 0 ? "0" : words[leftMost - 1];
                sl0l = state.getDependency(leftMost);
            }

            int rightMost = state.rightMostModifier(s0Position);
            if (rightMost >= 0) {
                sr0p = rightMost == 0 ? "0" : tags[rightMost - 1];
                sr0w = rightMost == 0 ? "0" : words[rightMost - 1];
                sr0l = state.getDependency(rightMost);
            }

            int headIndex = state.getHead(s0Position);
            if (headIndex >= 0) {
                sh0w = headIndex == 0 ? "0" : words[headIndex - 1];
                sh0p = headIndex == 0 ? "0" : tags[headIndex - 1];
                sh0l = state.getDependency(headIndex);
            }

            if (leftMost >= 0) {
                int l2 = state.leftMostModifier(leftMost);
                if (l2 >= 0) {
                    sl1w = l2 == 0 ? "0" : words[l2 - 1];
                    sl1p = l2 == 0 ? "0" : tags[l2 - 1];
                    sl1l = state.getDependency(l2);
                }
            }
            if (headIndex >= 0) {
                if (state.hasHead(headIndex)) {
                    int h2 = state.getHead(headIndex);
                    sh1w = h2 == 0 ? "0" : words[h2 - 1];
                    sh1p = h2 == 0 ? "0" : tags[h2 - 1];
                }
            }
            if (rightMost >= 0) {
                int r2 = state.rightMostModifier(rightMost);
                if (r2 >= 0) {
                    sr1w = r2 == 0 ? "0" : words[r2 - 1];
                    sr1p = r2 == 0 ? "0" : tags[r2 - 1];
                    sr1l = state.getDependency(r2);
                }
            }
        }

        int index = 0;
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
        featureMap[index++] = s0wp + "_" + b0wp;
        featureMap[index++] = s0wp + "_" + b0w;
        featureMap[index++] = s0w + "_" + b0wp;
        featureMap[index++] = s0wp + "_" + b0p;
        featureMap[index++] = s0p + "_" + b0wp;
        featureMap[index++] = s0w + "_" + b0w;
        featureMap[index++] = s0p + "_" + b0p;
        featureMap[index++] = b0p + "_" + b1p;

        /**
         * from three words
         */
        featureMap[index++] = b0p + "_" + b1p + "_" + b2p;
        featureMap[index++] = s0p + "_" + b0p + "_" + b1p;
        featureMap[index++] = sh0p + "_" + s0p + "_" + b0p;
        featureMap[index++] = s0p + "_" + sl0p + "_" + b0p;
        featureMap[index++] = s0p + "_" + sr0p + "_" + b0p;
        featureMap[index++] = s0p + "_" + b0p + "_" + bl0p;


        if (!basic) {
            /**
             * distance
             */
            float distance = 0;
            if (s0Position > 0 && b0Position > 0)
                distance = b0Position - s0Position;
            featureMap[index++] = s0w + "_" + distance;
            featureMap[index++] = s0p + "_" + distance;
            featureMap[index++] = b0w + "_" + distance;
            featureMap[index++] = b0p + "_" + distance;
            featureMap[index++] = s0w + "_" + b0w + "_" + distance;
            featureMap[index++] = s0p + "_" + b0p + "_" + distance;

            /**
             * Valency information
             */

            featureMap[index++] = s0w + "_" + svr;
            featureMap[index++] = s0p + "_" + svr;
            featureMap[index++] = s0w + "_" + svl;
            featureMap[index++] = s0p + "_" + svl;
            featureMap[index++] = b0w + "_" + bvl;
            featureMap[index++] = b0p + "_" + bvl;


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
            featureMap[index++] = s0p + "_" + sl0p + "_" + sl1p;
            featureMap[index++] = s0p + "_" + sr0p + "_" + sr1p;
            featureMap[index++] = s0p + "_" + sh0p + "_" + sh1p;
            featureMap[index++] = b0p + "_" + bl0p + "_" + bl1p;


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

            featureMap[index++] = s0w + "|" + sdr;
            featureMap[index++] = s0p + "|" + sdr;
            featureMap[index++] = s0w + "|" + sdl;
            featureMap[index++] = s0p + "|" + sdl;
            featureMap[index++] = b0w + "|" + bdl;
            featureMap[index++] = b0p + "|" + bdl;
        }
        return featureMap;
    }

    /**
     * Given a list of templates, extracts all features for the given state
     *
     * @param configuration
     * @return
     * @throws Exception
     */
    private static String[] extractBasicFeatures(Configuration configuration, int length) throws Exception {
        String[] featureMap = new String[length];

        State state = configuration.state;
        Sentence sentence = configuration.sentence;

        int b0Position = 0;
        int b1Position = 0;
        int b2Position = 0;
        int s0Position = 0;

        String b0w = "";
        String b0p = "";
        String b0wp = "";

        String b1w = "";
        String b1p = "";
        String b1wp = "";

        String b2w = "";
        String b2p = "";
        String b2wp = "";

        String s0w = "";
        String s0p = "";
        String s0wp = "";

        String bl0p = "";

        String sr0p = "";

        String sh0p = "";

        String sl0p = "";


        String[] tags = sentence.getTagStr();
        String[] words = sentence.getWordStr();

        if (0 < state.bufferSize()) {
            b0Position = state.bufferHead();
            b0w = words[b0Position - 1];
            b0p = tags[b0Position - 1];
            b0wp = b0w + "_" + b0p;

            int leftMost = state.leftMostModifier(state.getBufferItem(0));
            if (leftMost >= 0) {
                bl0p = "0";
                if (leftMost > 0)
                    bl0p = tags[leftMost - 1];
            }

            if (1 < state.bufferSize()) {
                b1Position = state.getBufferItem(1);
                b1w = words[b1Position - 1];
                b1p = tags[b1Position - 1];
                b1wp = b1w + "_" + b1p;

                if (2 < state.bufferSize()) {
                    b2Position = state.getBufferItem(2);

                    b2w = words[b2Position - 1];
                    b2p = tags[b2Position - 1];
                    b2wp = b2w + "_" + b2p;
                }
            }
        }


        if (0 < state.stackSize()) {
            s0Position = state.peek();
            s0w = s0Position == 0 ? "0" : words[s0Position - 1];
            s0p = s0Position == 0 ? "0" : tags[s0Position - 1];
            s0wp = s0w + "_" + s0p;
            int leftMost = state.leftMostModifier(s0Position);
            if (leftMost >= 0) {
                sl0p = leftMost == 0 ? "0" : tags[leftMost - 1];
            }

            int rightMost = state.rightMostModifier(s0Position);
            if (rightMost >= 0) {
                sr0p = rightMost == 0 ? "0" : tags[rightMost - 1];
            }

            int headIndex = state.getHead(s0Position);
            if (headIndex >= 0) {
                sh0p = headIndex == 0 ? "0" : tags[headIndex - 1];
            }
        }

        int index = 0;
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
        featureMap[index++] = s0wp + "_" + b0wp;
        featureMap[index++] = s0wp + "_" + b0w;
        featureMap[index++] = s0w + "_" + b0wp;
        featureMap[index++] = s0wp + "_" + b0p;
        featureMap[index++] = s0p + "_" + b0wp;
        featureMap[index++] = s0w + "_" + b0w;
        featureMap[index++] = s0p + "_" + b0p;
        featureMap[index++] = b0p + "_" + b1p;

        /**
         * from three words
         */
        featureMap[index++] = b0p + "_" + b1p + "_" + b2p;
        featureMap[index++] = s0p + "_" + b0p + "_" + b1p;
        featureMap[index++] = sh0p + "_" + s0p + "_" + b0p;
        featureMap[index++] = s0p + "_" + sl0p + "_" + b0p;
        featureMap[index++] = s0p + "_" + sr0p + "_" + b0p;
        featureMap[index++] = s0p + "_" + b0p + "_" + bl0p;

        return featureMap;
    }
}
