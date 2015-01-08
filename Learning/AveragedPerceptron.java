/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Learning;

import Accessories.Options;
import Accessories.Pair;
import Structures.IndexMaps;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AveragedPerceptron {
    /**
     * This class tries to implement averaged Perceptron algorithm
     * Collins, Michael. "Discriminative training methods for hidden Markov models: Theory and experiments with Perceptron algorithms."
     * In Proceedings of the ACL-02 conference on Empirical methods in natural language processing-Volume 10, pp. 1-8.
     * Association for Computational Linguistics, 2002.
     * <p/>
     * The averaging update is also optimized by using the trick introduced in Hal Daume's dissertation.
     * For more information see the second chapter of his thesis:
     * Harold Charles Daume' III. "Practical Structured Learning Techniques for Natural Language Processing", PhD thesis, ISI USC, 2006.
     * http://www.umiacs.umd.edu/~hal/docs/daume06thesis.pdf
     */
    /**
     * For the weights for all features
     */
    public  HashMap<Long, Float>[][] featureWeights;
    public int iteration;
    /**
     * This is the main part of the extension to the original perceptron algorithm which the averaging over all the history
     */
    public HashMap<Long, Float>[][] averagedWeights;


    public AveragedPerceptron(int size, int len) {
        featureWeights = new HashMap[len][size];
        for (int i = 0; i < featureWeights.length; i++)
            for (int j = 0; j < featureWeights[i].length; j++)
                featureWeights[i][j] = new HashMap<Long, Float>();
        iteration = 1;
        this.averagedWeights = new HashMap[len][size];
        for (int i = 0; i < averagedWeights.length; i++)
            for (int j = 0; j < averagedWeights[i].length; j++)
                averagedWeights[i][j] = new HashMap<Long, Float>();
    }

    public AveragedPerceptron(int size, HashMap<Long, Float>[][] averagedWeights, int len) {

        featureWeights = new HashMap[len][size];
        for (int i = 0; i < featureWeights.length; i++)
            for (int j = 0; j < featureWeights[i].length; j++)
                featureWeights[i][j] = new HashMap<Long, Float>();

        iteration = 1;
        this.averagedWeights = averagedWeights;
    }

    public float changeWeight(int slotNum, Long featureName, int labelIndex, float change) {
        HashMap<Long, Float> map = featureWeights[labelIndex][slotNum];
        Float value = map.get(featureName);
        if (value != null)
            map.put(featureName, change + value);
        else
            map.put(featureName, change);

        map = averagedWeights[labelIndex][slotNum];

        value = map.get(featureName);
        if (value != null)
            map.put(featureName, (iteration * change) + value);
        else
            map.put(featureName, iteration * change);

        return change;
    }

    /**
     * Adds to the iterations
     */
    public void incrementIteration() {
        iteration++;
    }

    /**
     * Returns the score of the specific feature
     *
     * @param features the features in the current instance
     * @return
     */
    public float score(final long[] features, int labelIndex, boolean decode) {
        float score = 0;
        final HashMap<Long, Float>[] weights;
        if (!decode) {
            weights = featureWeights[labelIndex];

        } else {
            weights = averagedWeights[labelIndex];
        }

        for (int i = 0; i < features.length; i++) {
            if (labelIndex < 3 && (i >= 26 && i < 32))
                continue;
            Float value = (weights[i]).get(features[i]);

            if (value != null)
                score += value;
        }
        return score;
    }

    public int size() {
        int size = 0;
        for (int i = 0; i < averagedWeights.length; i++)
            for (int j = 0; j < averagedWeights[i].length; j++)
                size += (averagedWeights[i][j]).size();
        return size;
    }

    public int featureSize() {
        return averagedWeights[0].length;
    }
}
