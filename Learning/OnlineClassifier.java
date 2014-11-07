package Learning;

import Accessories.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/
public class OnlineClassifier implements Serializable {
    /**
     * For the weights for all features
     */
    protected Object[][] featureWeights;

    protected int numberOfThreads;

    protected HashMap<String, Integer> labelsMap;

    int iteration;

    public OnlineClassifier(int size, ArrayList<String> labels, int numberOfThreads) {
        labelsMap = new HashMap<String, Integer>();
        for (int i = 0; i < labels.size(); i++)
            labelsMap.put(labels.get(i), i);

        featureWeights = new Object[labelsMap.size()][size];
        for (int i = 0; i < featureWeights.length; i++)
            for (int j = 0; j < featureWeights[i].length; j++)
                featureWeights[i][j] = new HashMap<String, Double>();

        this.numberOfThreads = numberOfThreads;
    }

    public OnlineClassifier(int size, HashMap<String, Integer> labelsMap, int numberOfThreads) {
        this.labelsMap = labelsMap;

        featureWeights = new Object[labelsMap.size()][size];
        for (int i = 0; i < featureWeights.length; i++)
            for (int j = 0; j < featureWeights[i].length; j++)
                featureWeights[i][j] = new HashMap<String, Double>();
        this.numberOfThreads = numberOfThreads;
    }

    public OnlineClassifier(Object[][] featureWeights, HashMap<String, Integer> labelsMap, int numberOfThreads) {
        this.featureWeights = featureWeights;
        this.labelsMap = labelsMap;
        iteration = 1;
        this.numberOfThreads = numberOfThreads;
    }

    public static OnlineClassifier loadModel(String modelPath, int numberOfThreads) throws IOException, ClassNotFoundException {
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(modelPath));
        return new OnlineClassifier((Object[][]) reader.readObject(), (HashMap<String, Integer>) reader.readObject(), numberOfThreads);
    }

    /**
     * This method updates the weight of a feature with the specific change value
     *
     * @param featureName the name (type) of the feature
     * @param change      can be either negative or positive
     */
    public double changeWeight(int slotNum, String featureName, String label, double change) {
        double newWeight = change;
        if (!labelsMap.containsKey(label)) {
            System.out.println("DEBUG");
        }
        int labelIndex = labelsMap.get(label);

        HashMap<String, Double> map = (HashMap<String, Double>) featureWeights[labelIndex][slotNum];
        if (!map.containsKey(featureName))
            map.put(featureName, change);
        else {
            newWeight = map.get(featureName) + change;
            map.put(featureName, newWeight);
        }
        return newWeight;
    }

    /**
     * Compresses the feature weights by removing zero-valued features
     *
     * @return Number of removed features
     */
    public int compressFeatureWeights() {
        ArrayList<String> zeroFeatures = new ArrayList<String>();
        for (int i = 0; i < featureWeights.length; i++) {
            for (int j = 0; j < featureWeights[i].length; j++) {
                HashMap<String, Double> map = (HashMap<String, Double>) featureWeights[i][j];
                for (String name : map.keySet()) {
                    if (map.get(name).equals(0))
                        zeroFeatures.add(name);
                }
                for (String name : zeroFeatures) {
                    map.remove(name);
                }
            }
        }

        return zeroFeatures.size();
    }

    public double score(Object[] features, String label, boolean decode) throws InterruptedException {
        double score = 0;
        int labelIndex = labelsMap.get(label);
        Object[] weights = featureWeights[labelIndex];

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

    public Object getWeights(int slotNum, String label, boolean decode) {
        int labelIndex = labelsMap.get(label);
        Object weights = featureWeights[labelIndex][slotNum];
        return weights;
    }

    /**
     * Adds to the iterations
     */
    public void incrementIteration() {
        iteration++;
    }

    /**
     * Puts the whole data into a file
     *
     * @param modelPath path to the model file
     * @throws IOException
     */
    public void saveModel(String modelPath) throws IOException {
        compressFeatureWeights();
        ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(modelPath));
        writer.writeObject(this.featureWeights);
        writer.writeObject(this.labelsMap);
        writer.flush();
        writer.close();
    }

    public int getIteration() {
        return iteration;
    }

    public int size() {
        int size = 0;
        for (int i = 0; i < featureWeights.length; i++)
            for (int j = 0; i < featureWeights[i].length; j++)
                size += ((HashMap<String, Double>) featureWeights[i][j]).size();
        return size;
    }
}
