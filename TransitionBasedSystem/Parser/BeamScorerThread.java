/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package TransitionBasedSystem.Parser;

import Learning.AveragedPerceptron;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.BeamElement;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.State;
import TransitionBasedSystem.Features.FeatureExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;


public class BeamScorerThread implements Callable<ArrayList<BeamElement>> {

    boolean isDecode;
    AveragedPerceptron classifier;
    Configuration configuration;
    ArrayList<Integer> dependencyRelations;
    int featureLength;
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet;
    int b;
    boolean rootFirst;

    public BeamScorerThread(boolean isDecode, AveragedPerceptron classifier, Configuration configuration, ArrayList<Integer> dependencyRelations, int featureLength, HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet, int b,boolean rootFirst) {
        this.isDecode = isDecode;
        this.classifier = classifier;
        this.configuration = configuration;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        this.headDepSet = headDepSet;
        this.b = b;
        this.rootFirst=rootFirst;
    }


    public ArrayList<BeamElement> call() {
        ArrayList<BeamElement> elements = new ArrayList<BeamElement>(dependencyRelations.size() * 2 + 3);

        State currentState = configuration.state;
        float prevScore = configuration.score;
        Sentence sentence = configuration.sentence;

        boolean canShift = ArcEager.canDo(0, currentState);
        boolean canReduce = ArcEager.canDo(1, currentState);
        boolean canRightArc = ArcEager.canDo(2, currentState);
        boolean canLeftArc = ArcEager.canDo(3, currentState);
        long[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);

        if (canShift) {
            float score = classifier.score(features, 0, isDecode);
            float addedScore = score + prevScore;
            elements.add(new BeamElement(addedScore, b, 0, -1));
        }
        if (canReduce) {
            float score = classifier.score(features, 1, isDecode);
            float addedScore = score + prevScore;
            elements.add(new BeamElement(addedScore, b, 1, -1));

        }

        if (canRightArc) {
            int headPos = sentence.posAt(configuration.state.peek());
            int depPos = sentence.posAt(configuration.state.bufferHead());
            for (int dependency : dependencyRelations) {
                if ((!canLeftArc && !canShift && !canReduce) || (rootFirst && canRightArc) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                        && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                    float score = classifier.score(features, 3 + dependency, isDecode);
                    float addedScore = score + prevScore;
                    elements.add(new BeamElement(addedScore, b, 2, dependency));
                }
            }
        }
        if (canLeftArc) {
            int headPos = sentence.posAt(configuration.state.bufferHead());
            int depPos = sentence.posAt(configuration.state.peek());
            for (int dependency : dependencyRelations) {
                if ((!canShift && !canRightArc && !canReduce) || (rootFirst && canLeftArc) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                        && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                    float score = classifier.score(features, 3 + dependencyRelations.size() + dependency, isDecode);
                    float addedScore = score + prevScore;
                    elements.add(new BeamElement(addedScore, b, 3, dependency));

                }
            }
        }
        return elements;
    }

}
