/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package TransitionBasedSystem.Features;

import Accessories.Pair;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.State;

import java.util.TreeSet;

public class FeatureExtractor {
    /**
     * Given a list of templates, extracts all features for the given state
     *
     * @param configuration
     * @return
     * @throws Exception
     */
    public static Object[] extractAllParseFeatures(Configuration configuration, int length) throws Exception {
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
    private static Object[] extractExtendedFeatures(Configuration configuration, int length) throws Exception {
        Object[] featureMap = new Object[length];

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

        String[] words=sentence.getWords();
        String[] tags=sentence.getTags();

        if (0 < state.bufferSize()) {
            b0Position = state.bufferHead();
            b0w =b0Position==0?"ROOT":words[b0Position-1];
            b0p = b0Position==0?"ROOT":tags[b0Position-1];
            b0wp = b0w + ":" + b0p;
            bvl = state.leftValency(b0Position);

            int leftMost = state.leftMostModifier(state.getBufferItem(0));
            if (leftMost >= 0) {
                bl0p =leftMost==0?"ROOT":tags[leftMost-1];
                bl0w =leftMost==0?"ROOT":words[leftMost-1];
                bl0l = state.getDependency(leftMost);

                int l2 = state.leftMostModifier(leftMost);
                if (l2 >= 0) {
                    bl1w =l2==0?"ROOT":words[l2-1];
                    bl1p =l2==0?"ROOT":tags[l2-1];
                    bl1l = state.getDependency(l2);
                }
            }

            if (1 < state.bufferSize()) {
                b1Position = state.getBufferItem(1);
                b1w = b1Position==0?"ROOT":words[b1Position-1];
                b1p =  b1Position==0?"ROOT":tags[b1Position-1];
                b1wp = b1w + ":" + b1p;

                if (2 < state.bufferSize()) {
                    b2Position = state.getBufferItem(2);

                    b2w = b2Position==0?"ROOT":words[b2Position-1];
                    b2p = b2Position==0?"ROOT":tags[b2Position-1];
                    b2wp = b2w + ":" + b2p;
                }
            }
        }


        if (0 < state.stackSize()) {
            s0Position = state.peek();
            s0w = s0Position==0?"ROOT":words[s0Position-1];
            s0p =s0Position==0?"ROOT":tags[s0Position-1];
            s0wp = s0w + ":" + s0p;
            s0l = state.getDependency(s0Position);
            svl = state.leftValency(s0Position);
            svr = state.rightValency(s0Position);

            int leftMost = state.leftMostModifier(s0Position);
            if (leftMost >= 0) {
                sl0p =leftMost==0?"ROOT":tags[leftMost-1];
                sl0w =leftMost==0?"ROOT":words[leftMost-1];
                sl0l = state.getDependency(leftMost);
            }

            int rightMost = state.rightMostModifier(s0Position);
            if (rightMost >= 0) {
                sr0p = rightMost==0?"ROOT":tags[rightMost-1];
                sr0w = rightMost==0?"ROOT":words[rightMost-1];
                sr0l = state.getDependency(rightMost);
            }

            int headIndex = state.getHead(s0Position);
            if (headIndex >= 0) {
                sh0w =headIndex==0?"ROOT":words[headIndex-1];
                sh0p = headIndex==0?"ROOT":tags[headIndex-1];
                sh0l = state.getDependency(headIndex);
            }

            if (leftMost >= 0) {
                int l2 = state.leftMostModifier(leftMost);
                if (l2 >= 0) {
                    sl1w = l2==0?"ROOT":words[l2-1];
                    sl1p = l2==0?"ROOT":tags[l2-1];
                    sl1l = state.getDependency(l2);
                }
            }
            if (headIndex >= 0) {
                if (state.hasHead(headIndex)) {
                    int h2 = state.getHead(headIndex);
                    sh1w = h2==0?"ROOT":words[h2-1];
                    sh1p =  h2==0?"ROOT":tags[h2-1];
                }
            }
            if (rightMost >= 0) {
                int r2 = state.rightMostModifier(rightMost);
                if (r2 >= 0) {
                    sr1w = r2==0?"ROOT":words[r2-1];
                    sr1p = r2==0?"ROOT":tags[r2-1];
                    sr1l = state.getDependency(r2);
                }
            }
        }

        int index = 0;
        /**
         * From single words
         */
        featureMap[index++] = new Pair<String, Double>(s0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b1wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(b1w, 1.0);
        featureMap[index++] = new Pair<String, Double>(b1p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b2wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(b2w, 1.0);
        featureMap[index++] = new Pair<String, Double>(b2p, 1.0);

        /**
         * from word pairs
         */
        featureMap[index++] = new Pair<String, Double>(s0wp + ":" + b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0wp + ":" + b0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0w + ":" + b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0wp + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0w + ":" + b0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0p + ":" + b1p, 1.0);

        /**
         * from three words
         */
        featureMap[index++] = new Pair<String, Double>(b0p + ":" + b1p + ":" + b2p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0p + ":" + b1p, 1.0);
        featureMap[index++] = new Pair<String, Double>(sh0p + ":" + s0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + sl0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + sr0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0p + ":" + bl0p, 1.0);


        if (!basic) {
            /**
             * distance
             */
            double distance = 0;
            if (s0Position > 0 && b0Position > 0)
                distance = b0Position - s0Position;
            featureMap[index++] = new Pair<String, Double>(s0w + ":" + distance, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0p + ":" + distance, 1.0);
            featureMap[index++] = new Pair<String, Double>(b0w + ":" + distance, 1.0);
            featureMap[index++] = new Pair<String, Double>(b0p + ":" + distance, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0w + ":" + b0w + ":" + distance, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0p + ":" + distance, 1.0);

            /**
             * Valency information
             */

            featureMap[index++] = new Pair<String, Double>(s0w + ":" + svr, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0p + ":" + svr, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0w + ":" + svl, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0p + ":" + svl, 1.0);
            featureMap[index++] = new Pair<String, Double>(b0w + ":" + bvl, 1.0);
            featureMap[index++] = new Pair<String, Double>(b0p + ":" + bvl, 1.0);


            /**
             * Unigrams
             */
            featureMap[index++] = new Pair<String, Double>(sh0w, 1.0);
            featureMap[index++] = new Pair<String, Double>(sh0p, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0l, 1.0);
            featureMap[index++] = new Pair<String, Double>(sl0w, 1.0);
            featureMap[index++] = new Pair<String, Double>(sl0p, 1.0);
            featureMap[index++] = new Pair<String, Double>(sl0l, 1.0);
            featureMap[index++] = new Pair<String, Double>(sr0w, 1.0);
            featureMap[index++] = new Pair<String, Double>(sr0p, 1.0);
            featureMap[index++] = new Pair<String, Double>(sr0l, 1.0);
            featureMap[index++] = new Pair<String, Double>(bl0w, 1.0);
            featureMap[index++] = new Pair<String, Double>(bl0p, 1.0);
            featureMap[index++] = new Pair<String, Double>(bl0l, 1.0);


            /**
             * From third order features
             */
            featureMap[index++] = new Pair<String, Double>(sh1w, 1.0);
            featureMap[index++] = new Pair<String, Double>(sh1p, 1.0);
            featureMap[index++] = new Pair<String, Double>(sh0l, 1.0);
            featureMap[index++] = new Pair<String, Double>(sl1w, 1.0);
            featureMap[index++] = new Pair<String, Double>(sl1p, 1.0);
            featureMap[index++] = new Pair<String, Double>(sl1l, 1.0);
            featureMap[index++] = new Pair<String, Double>(sr1w, 1.0);
            featureMap[index++] = new Pair<String, Double>(sr1p, 1.0);
            featureMap[index++] = new Pair<String, Double>(sr1l, 1.0);
            featureMap[index++] = new Pair<String, Double>(bl1w, 1.0);
            featureMap[index++] = new Pair<String, Double>(bl1p, 1.0);
            featureMap[index++] = new Pair<String, Double>(bl1l, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0p + ":" + sl0p + ":" + sl1p, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0p + ":" + sr0p + ":" + sr1p, 1.0);
            featureMap[index++] = new Pair<String, Double>(s0p + ":" + sh0p + ":" + sh1p, 1.0);
            featureMap[index++] = new Pair<String, Double>(b0p + ":" + bl0p + ":" + bl1p, 1.0);


            /**
             * label set
             */
            if (s0Position >= 0) {
                TreeSet<String> leftLabels = state.leftDependentLabels(s0Position);
                StringBuilder l = new StringBuilder();
                for (String label : leftLabels)
                    l.append(label + ":");
                sdl = l.toString();

                TreeSet<String> rightLabels = state.rightDependentLabels(s0Position);
                StringBuilder r = new StringBuilder();
                for (String label : rightLabels)
                    r.append(label + ":");
                sdr = r.toString();
            }

            if (b0Position >= 0) {
                TreeSet<String> leftLabels = state.leftDependentLabels(b0Position);
                StringBuilder l = new StringBuilder();
                for (String label : leftLabels)
                    l.append(label + ":");
                bdl = l.toString();
            }

            StringBuilder b1 = new StringBuilder();
            b1.append(s0w);
            b1.append("|");
            b1.append(sdr);
            StringBuilder b2 = new StringBuilder();
            b2.append(s0p);
            b2.append("|");
            b2.append(sdr);
            StringBuilder b3 = new StringBuilder();
            b3.append(s0w);
            b3.append("|");
            b3.append(sdl);
            StringBuilder b4 = new StringBuilder();
            b4.append(s0p);
            b4.append("|");
            b4.append(sdl);
            StringBuilder b5 = new StringBuilder();
            b5.append(b0w);
            b5.append("|");
            b5.append(bdl);
            StringBuilder b6 = new StringBuilder();
            b6.append(b0p);
            b6.append("|");
            b6.append(bdl);

            featureMap[index++] = new Pair<String, Double>(b1.toString(), 1.0);
            featureMap[index++] = new Pair<String, Double>(b2.toString(), 1.0);
            featureMap[index++] = new Pair<String, Double>(b3.toString(), 1.0);
            featureMap[index++] = new Pair<String, Double>(b4.toString(), 1.0);
            featureMap[index++] = new Pair<String, Double>(b5.toString(), 1.0);
            featureMap[index++] = new Pair<String, Double>(b6.toString(), 1.0);
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
    private static Object[] extractBasicFeatures(Configuration configuration, int length) throws Exception {
        Object[] featureMap = new Object[length];

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


        String[] tags=sentence.getTags();
        String[] words=sentence.getWords();

        if (0 < state.bufferSize()) {
            b0Position = state.bufferHead();
            b0w = words[b0Position-1];
            b0p =tags[b0Position-1];
            b0wp = b0w + ":" + b0p;

            int leftMost = state.leftMostModifier(state.getBufferItem(0));
            if (leftMost >= 0) {
                bl0p ="ROOT";
                if(leftMost>0)
                    bl0p=tags[leftMost-1];
            }

            if (1 < state.bufferSize()) {
                b1Position = state.getBufferItem(1);
                b1w = words[b1Position-1];
                b1p =tags[b1Position-1];
                b1wp = b1w + ":" + b1p;

                if (2 < state.bufferSize()) {
                    b2Position = state.getBufferItem(2);

                    b2w =  words[b2Position-1];
                    b2p = tags[b2Position-1];
                    b2wp = b2w + ":" + b2p;
                }
            }
        }


        if (0 < state.stackSize()) {
            s0Position =state.peek();
            s0w =s0Position==0? "ROOT":words[s0Position-1];
            s0p =s0Position==0?"ROOT":tags[s0Position-1];
            s0wp = s0w + ":" + s0p;
            int leftMost = state.leftMostModifier(s0Position);
            if (leftMost >= 0) {
                sl0p = leftMost==0?"ROOT":tags[leftMost-1];
            }

            int rightMost = state.rightMostModifier(s0Position);
            if (rightMost >= 0) {
                sr0p =rightMost==0?"ROOT":tags[rightMost-1];
            }

            int headIndex = state.getHead(s0Position);
            if (headIndex >= 0) {
                sh0p =headIndex==0?"ROOT":tags[headIndex-1];
            }
        }

        int index = 0;
        /**
         * From single words
         */
        featureMap[index++] = new Pair<String, Double>(s0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b1wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(b1w, 1.0);
        featureMap[index++] = new Pair<String, Double>(b1p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b2wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(b2w, 1.0);
        featureMap[index++] = new Pair<String, Double>(b2p, 1.0);

        /**
         * from word pairs
         */
        featureMap[index++] = new Pair<String, Double>(s0wp + ":" + b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0wp + ":" + b0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0w + ":" + b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0wp + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0wp, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0w + ":" + b0w, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(b0p + ":" + b1p, 1.0);

        /**
         * from three words
         */
        featureMap[index++] = new Pair<String, Double>(b0p + ":" + b1p + ":" + b2p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0p + ":" + b1p, 1.0);
        featureMap[index++] = new Pair<String, Double>(sh0p + ":" + s0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + sl0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + sr0p + ":" + b0p, 1.0);
        featureMap[index++] = new Pair<String, Double>(s0p + ":" + b0p + ":" + bl0p, 1.0);

        return featureMap;
    }
}
