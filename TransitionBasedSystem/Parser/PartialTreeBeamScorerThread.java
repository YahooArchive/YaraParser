/**
 * Copyright 2014, Yahoo! Inc. and Mohammad Sadegh Rasooli
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package TransitionBasedSystem.Parser;

import Learning.AveragedPerceptron;
import TransitionBasedSystem.Configuration.BeamElement;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.GoldConfiguration;
import TransitionBasedSystem.Configuration.State;
import TransitionBasedSystem.Features.FeatureExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;


public class PartialTreeBeamScorerThread implements Callable<ArrayList<BeamElement>> {

    boolean isDecode;
    AveragedPerceptron classifier;
    Configuration configuration;
    GoldConfiguration goldConfiguration;
    ArrayList<Integer> dependencyRelations;
    int featureLength;
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet;
    int b;

    public PartialTreeBeamScorerThread(boolean isDecode, AveragedPerceptron classifier, GoldConfiguration goldConfiguration, Configuration configuration, ArrayList<Integer> dependencyRelations, int featureLength, HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet, int b) {
        this.isDecode = isDecode;
        this.classifier = classifier;
        this.configuration = configuration;
        this.goldConfiguration = goldConfiguration;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        this.headDepSet = headDepSet;
        this.b = b;
    }


    public ArrayList<BeamElement> call() throws Exception {
        ArrayList<BeamElement> elements = new ArrayList<BeamElement>(dependencyRelations.size() * 2 + 3);

        boolean isNonProjective = false;
        if (goldConfiguration.isNonprojective()) {
            isNonProjective = true;
        }

        State currentState = configuration.state;
        float prevScore = configuration.score;

        boolean canShift = ArcEager.canDo(Actions.Shift, currentState);
        boolean canReduce = ArcEager.canDo(Actions.Reduce, currentState);
        boolean canRightArc = ArcEager.canDo(Actions.RightArc, currentState);
        boolean canLeftArc = ArcEager.canDo(Actions.LeftArc, currentState);
        long[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);

        if (canShift) {
            if (isNonProjective || goldConfiguration.actionCost(Actions.Shift, -1, currentState) == 0) {
                float score = classifier.score(features, 0, isDecode);
                float addedScore = score + prevScore;
                elements.add(new BeamElement(addedScore, b, 0, -1));
            }
        }
        if (canReduce) {
            if (isNonProjective || goldConfiguration.actionCost(Actions.Reduce, -1, currentState) == 0) {
                float score = classifier.score(features, 1, isDecode);
                float addedScore = score + prevScore;
                elements.add(new BeamElement(addedScore, b, 1, -1));
            }

        }

        if (canRightArc) {
            for (int dependency : dependencyRelations) {
                if (isNonProjective || goldConfiguration.actionCost(Actions.RightArc, dependency, currentState) == 0) {
                    float score = classifier.score(features, 3 + dependency, isDecode);
                    float addedScore = score + prevScore;
                    elements.add(new BeamElement(addedScore, b, 2, dependency));
                }
            }
        }
        if (canLeftArc) {
            for (int dependency : dependencyRelations) {
                if (isNonProjective || goldConfiguration.actionCost(Actions.LeftArc, dependency, currentState) == 0) {
                    float score = classifier.score(features, 3 + dependencyRelations.size() + dependency, isDecode);
                    float addedScore = score + prevScore;
                    elements.add(new BeamElement(addedScore, b, 3, dependency));

                }
            }
        }

        if (elements.size() == 0) {
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
                for (int dependency : dependencyRelations) {
                    float score = classifier.score(features, 3 + dependency, isDecode);
                    float addedScore = score + prevScore;
                    elements.add(new BeamElement(addedScore, b, 2, dependency));
                }
            }
            if (canLeftArc) {
                for (int dependency : dependencyRelations) {
                    float score = classifier.score(features, 3 + dependencyRelations.size() + dependency, isDecode);
                    float addedScore = score + prevScore;
                    elements.add(new BeamElement(addedScore, b, 3, dependency));
                }
            }
        }

        return elements;
    }
}