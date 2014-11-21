/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Learning;

import java.io.*;
import java.util.HashMap;

public class AveragedPerceptron {
    /**
     * This class tries to implement averaged Perceptron algorithm
     * Collins, Michael. "Discriminative training methods for hidden Markov models: Theory and experiments with Perceptron algorithms."
     * In Proceedings of the ACL-02 conference on Empirical methods in natural language processing-Volume 10, pp. 1-8.
     * Association for Computational Linguistics, 2002.
     *
     * The averaging update is also optimized by using the trick introduced in Hal Daume's dissertation.
     * For more information see the second chapter of his thesis:
     * Harold Charles Daume' III. "Practical Structured Learning Techniques for Natural Language Processing", PhD thesis, ISI USC, 2006.
     * http://www.umiacs.umd.edu/~hal/docs/daume06thesis.pdf
     **/

    /**
     * For the weights for all features
     */
    protected Object[][] featureWeights;
    protected int numberOfThreads;
    protected HashMap<String, Integer> labelsMap;
    int iteration;
    /**
     * This is the main part of the extension to the original perceptron algorithm which the averaging over all the history
     */
    private Object[][] averagedWeights;

    public AveragedPerceptron(int size, HashMap<String, Integer> labels, int numberOfThreads) {
        this.labelsMap = labels;
        featureWeights = new Object[labelsMap.size()][size];
        for (int i = 0; i < featureWeights.length; i++)
            for (int j = 0; j < featureWeights[i].length; j++)
                featureWeights[i][j] = new HashMap<String, Float>();
        this.numberOfThreads = numberOfThreads;
        iteration = 1;
        this.averagedWeights = new Object[labelsMap.size()][size];
        for (int i = 0; i < averagedWeights.length; i++)
            for (int j = 0; j < averagedWeights[i].length; j++)
                averagedWeights[i][j] = new HashMap<String, Float>();
    }

    public AveragedPerceptron(int size, Object[][] averagedWeights, HashMap<String, Integer> labelsMap, int numberOfThreads) {
        this.labelsMap = labelsMap;

        featureWeights = new Object[labelsMap.size()][size];
        for (int i = 0; i < featureWeights.length; i++)
            for (int j = 0; j < featureWeights[i].length; j++)
                featureWeights[i][j] = new HashMap<String, Float>();

        this.numberOfThreads = numberOfThreads;
        iteration = 1;
        this.averagedWeights = averagedWeights;
    }

    public static AveragedPerceptron loadModel(String modelPath, int numberOfThreads) throws IOException, ClassNotFoundException {
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(modelPath));
        Object[][] avg = (Object[][]) reader.readObject();
        HashMap<String, Integer> labelsMap = (HashMap<String, Integer>) reader.readObject();
        reader.close();

        return new AveragedPerceptron(avg.length, avg, labelsMap, numberOfThreads);
    }

    public float changeWeight(int slotNum, String featureName, String label, float change) {
        int labelIndex = labelsMap.get(label);

        float newWeight = change;
        if (!labelsMap.containsKey(label)) {
            System.out.println("DEBUG");
        }

        HashMap<String, Float> map = (HashMap<String, Float>) featureWeights[labelIndex][slotNum];
        Float value = map.get(featureName);
        if (value != null)
            map.put(featureName, change + value);
        else
            map.put(featureName, change);

        map = (HashMap<String, Float>) averagedWeights[labelIndex][slotNum];

        value = map.get(featureName);
        if (value != null)
            map.put(featureName, (iteration * change) + value);
        else
            map.put(featureName, iteration * change);

        return newWeight;
    }

    public void saveModel(String modelPath) throws IOException {
        Object[][] avg = new Object[labelsMap.size()][featureWeights[0].length];
        for (int i = 0; i < avg.length; i++) {
            for (int j = 0; j < avg[i].length; j++) {
                avg[i][j] = new HashMap<String, Float>();
                HashMap<String, Float> map = (HashMap<String, Float>) featureWeights[i][j];
                HashMap<String, Float> avgMap = (HashMap<String, Float>) averagedWeights[i][j];
                for (String feat : map.keySet()) {
                    float weight = map.get(feat) - (avgMap.get(feat) / iteration);
                    if (weight != 0)
                        ((HashMap<String, Float>) avg[i][j]).put(feat, weight);
                }
            }
        }

        ObjectOutput writer = new ObjectOutputStream(new FileOutputStream(modelPath));
        writer.writeObject(avg);
        writer.writeObject(labelsMap);
        writer.flush();
        writer.close();

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
    public float score(String[] features, String label, boolean decode) throws InterruptedException {
        float score = 0;

        int labelIndex = labelsMap.get(label);
        Object[] weights;
        if (!decode) {
            weights = featureWeights[labelIndex];
        } else {
            weights = averagedWeights[labelIndex];
        }

        for (int i = 0; i < features.length; i++) {
            if (features[i] == null)
                continue;

            Float value = ((HashMap<String, Float>) weights[i]).get(features[i]);

            if (value != null)
                score += value;
        }

        return score;
    }

    public int size() {
        int size = 0;
        for (int i = 0; i < averagedWeights.length; i++)
            for (int j = 0; j < averagedWeights[i].length; j++)
                size += ((HashMap<String, Float>) averagedWeights[i][j]).size();
        return size;
    }

    public int featureSize() {
        return averagedWeights[0].length;
    }
}
