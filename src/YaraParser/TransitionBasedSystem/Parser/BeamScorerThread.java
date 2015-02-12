/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package YaraParser.TransitionBasedSystem.Parser;

import YaraParser.Learning.AveragedPerceptron;
import YaraParser.TransitionBasedSystem.Configuration.BeamElement;
import YaraParser.TransitionBasedSystem.Configuration.Configuration;
import YaraParser.TransitionBasedSystem.Configuration.State;
import YaraParser.TransitionBasedSystem.Features.FeatureExtractor;

import java.util.ArrayList;
import java.util.concurrent.Callable;


public class BeamScorerThread implements Callable<ArrayList<BeamElement>> {

    boolean isDecode;
    AveragedPerceptron classifier;
    Configuration configuration;
    ArrayList<Integer> dependencyRelations;
    int featureLength;
    int b;
    boolean rootFirst;

    public BeamScorerThread(boolean isDecode, AveragedPerceptron classifier, Configuration configuration, ArrayList<Integer> dependencyRelations, int featureLength, int b, boolean rootFirst) {
        this.isDecode = isDecode;
        this.classifier = classifier;
        this.configuration = configuration;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        this.b = b;
        this.rootFirst = rootFirst;
    }


    public ArrayList<BeamElement> call() {
        ArrayList<BeamElement> elements = new ArrayList<BeamElement>(dependencyRelations.size() * 2 + 3);

        State currentState = configuration.state;
        float prevScore = configuration.score;

        boolean canShift = ArcEager.canDo(Actions.Shift, currentState);
        boolean canReduce = ArcEager.canDo(Actions.Reduce, currentState);
        boolean canRightArc = ArcEager.canDo(Actions.RightArc, currentState);
        boolean canLeftArc = ArcEager.canDo(Actions.LeftArc, currentState);
        Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);

        if (canShift) {
            float score = classifier.shiftScore(features, isDecode);
            float addedScore = score + prevScore;
            elements.add(new BeamElement(addedScore, b, 0, -1));
        }
        if (canReduce) {
            float score = classifier.reduceScore(features, isDecode);
            float addedScore = score + prevScore;
            elements.add(new BeamElement(addedScore, b, 1, -1));

        }

        if (canRightArc) {
            float[] rightArcScores = classifier.rightArcScores(features, isDecode);
            for (int dependency : dependencyRelations) {
                float score = rightArcScores[dependency];
                float addedScore = score + prevScore;
                elements.add(new BeamElement(addedScore, b, 2, dependency));
            }
        }
        if (canLeftArc) {
            float[] leftArcScores = classifier.leftArcScores(features, isDecode);
            for (int dependency : dependencyRelations) {
                float score = leftArcScores[dependency];
                float addedScore = score + prevScore;
                elements.add(new BeamElement(addedScore, b, 3, dependency));
            }
        }
        return elements;
    }
}