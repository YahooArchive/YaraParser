/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package Learning;

import Accessories.Pair;

import java.io.*;
import java.util.HashMap;

public class AveragedPerceptron extends Perceptron {
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
     * This is the main part of the extension to the original perceptron algorithm which the averaging over all the history
     */
    private Object[][] averagedWeights;

    public AveragedPerceptron(int size, HashMap<String, Integer> labels, int numberOfThreads) {
        super(size, labels, numberOfThreads);
        iteration = 1;
        this.averagedWeights = new Object[labelsMap.size()][size];
        for (int i = 0; i < averagedWeights.length; i++)
            for (int j = 0; j < averagedWeights[i].length; j++)
                averagedWeights[i][j] = new HashMap<String, Double>();
    }

    public AveragedPerceptron(int size, Object[][] averagedWeights, HashMap<String, Integer> labelsMap, int numberOfThreads) {
        super(size, labelsMap, numberOfThreads);
        iteration = 1;
        this.averagedWeights = averagedWeights;
    }

    public static OnlineClassifier loadModel(String modelPath, int numberOfThreads) throws IOException, ClassNotFoundException {
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(modelPath));
        Object[][] avg = (Object[][]) reader.readObject();
        HashMap<String, Integer> labelsMap = (HashMap<String, Integer>) reader.readObject();
        reader.close();

        return new AveragedPerceptron(avg.length, avg, labelsMap, numberOfThreads);
    }

    @Override
    public double changeWeight(int slotNum, String featureName, String label, double change) {
        double newWeight = super.changeWeight(slotNum, featureName, label, change);
        int labelIndex = labelsMap.get(label);

        HashMap<String, Double> map = (HashMap<String, Double>) averagedWeights[labelIndex][slotNum];
        if (map.containsKey(featureName))
            map.put(featureName, (iteration * change) + map.get(featureName));
        else
            map.put(featureName, iteration * change);
        return newWeight;
    }

    @Override
    public void saveModel(String modelPath) throws IOException {
        Object[][] avg = new Object[labelsMap.size()][featureWeights[0].length];
        for (int i = 0; i < avg.length; i++) {
            for (int j = 0; j < avg[i].length; j++) {
                avg[i][j] = new HashMap<String, Double>();
                HashMap<String, Double> map = (HashMap<String, Double>) featureWeights[i][j];
                HashMap<String, Double> avgMap = (HashMap<String, Double>) averagedWeights[i][j];
                for (String feat : map.keySet()) {
                    double weight = map.get(feat) - (avgMap.get(feat) / iteration);
                    if (weight != 0)
                        ((HashMap<String, Double>) avg[i][j]).put(feat, weight);
                }
            }
        }

        ObjectOutput writer = new ObjectOutputStream(new FileOutputStream(modelPath));
        writer.writeObject(avg);
        writer.writeObject(labelsMap);
        writer.flush();
        writer.close();

    }

    @Override
    public Object getWeights(int slotNum, String label, boolean decode) {
        int labelIndex = labelsMap.get(label);
        if (decode) {
            Object weights = averagedWeights[labelIndex][slotNum];
            return weights;
        } else {
            Object weights = featureWeights[labelIndex][slotNum];
            return weights;
        }
    }

    /**
     * Returns the score of the specific feature
     *
     * @param features the features in the current instance
     * @return
     */
    @Override
    public double score(Object[] features, String label, boolean decode) throws InterruptedException {
        double score = 0;

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

            Pair<String, Double> pair = (Pair<String, Double>) features[i];
            HashMap<String, Double> map = (HashMap<String, Double>) weights[i];
            if (map.containsKey(pair.first))
                score += pair.second * map.get(pair.first);
        }

        return score;
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 0; i < averagedWeights.length; i++)
            for (int j = 0; j < averagedWeights[i].length; j++)
                size += ((HashMap<String, Double>) averagedWeights[i][j]).size();
        return size;
    }

    public int featureSize(){
        return averagedWeights[0].length;
    }
}
