package Learning;

import java.util.HashMap;

/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/
public class Perceptron extends OnlineClassifier {
    /**
     * The perceptorn algorithm
     * Rosenblatt, Frank. "The Perceptron: a probabilistic model for information storage and organization in the brain."
     * Psychological review 65, no. 6 (1958): 386.
     */
    public Perceptron(int size, HashMap<String, Integer> labels, int numberOfThreads) {
        super(size, labels, numberOfThreads);
    }

}
