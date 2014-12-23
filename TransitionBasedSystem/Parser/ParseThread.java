/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */


package TransitionBasedSystem.Parser;

import Accessories.Pair;
import Learning.AveragedPerceptron;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.BeamElement;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.State;
import TransitionBasedSystem.Features.FeatureExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;


public class ParseThread implements Callable<Pair<Configuration, Integer>> {
    AveragedPerceptron classifier;

    ArrayList<Integer> dependencyRelations;

    int featureLength;

    // for pruning irrelevant search space
    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet;

    Sentence sentence;
    boolean rootFirst;
    int beamWidth;

    int id;

    public ParseThread(int id, AveragedPerceptron classifier, ArrayList<Integer> dependencyRelations, int featureLength, HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet, Sentence sentence, boolean rootFirst, int beamWidth) {
        this.id = id;
        this.classifier = classifier;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        this.headDepSet = headDepSet;
        this.sentence = sentence;
        this.rootFirst = rootFirst;
        this.beamWidth = beamWidth;
    }

    @Override
    public Pair<Configuration, Integer> call() throws Exception {
        return parse();
    }

    Pair<Configuration, Integer> parse() throws Exception {
        Configuration initialConfiguration = new Configuration(sentence, rootFirst);

        ArrayList<Configuration> beam = new ArrayList<Configuration>(beamWidth);
        beam.add(initialConfiguration);

        while (!ArcEager.isTerminal(beam)) {
            if (beamWidth != 1) {
                TreeSet<BeamElement> beamPreserver = new TreeSet<BeamElement>();
                for (int b = 0; b < beam.size(); b++) {
                    Configuration configuration = beam.get(b);
                    State currentState = configuration.state;
                    float prevScore = configuration.score;
                    boolean canShift = ArcEager.canDo(Actions.Shift, currentState);
                    boolean canReduce = ArcEager.canDo(Actions.Reduce, currentState);
                    boolean canRightArc = ArcEager.canDo(Actions.RightArc, currentState);
                    boolean canLeftArc = ArcEager.canDo(Actions.LeftArc, currentState);
                    long[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                    if (!canShift
                            && !canReduce
                            && !canRightArc
                            && !canLeftArc) {
                        beamPreserver.add(new BeamElement(prevScore, b, 4, -1));

                        if (beamPreserver.size() > beamWidth)
                            beamPreserver.pollFirst();
                    }

                    if (canShift) {
                        float score = classifier.score(features, 0, true);
                        float addedScore = score + prevScore;
                        beamPreserver.add(new BeamElement(addedScore, b, 0, -1));

                        if (beamPreserver.size() > beamWidth)
                            beamPreserver.pollFirst();
                    }

                    if (canReduce) {
                        float score = classifier.score(features, 1, true);
                        float addedScore = score + prevScore;
                        beamPreserver.add(new BeamElement(addedScore, b, 1, -1));

                        if (beamPreserver.size() > beamWidth)
                            beamPreserver.pollFirst();
                    }

                    if (canRightArc) {
                        int headPos = sentence.posAt(configuration.state.peek());
                        int depPos = sentence.posAt(configuration.state.bufferHead());
                        for (int dependency : dependencyRelations) {
                            if ((!canLeftArc && !canShift && !canReduce) || (rootFirst && canRightArc) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                    && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                float score = classifier.score(features, 3 + dependency, true);
                                float addedScore = score + prevScore;
                                beamPreserver.add(new BeamElement(addedScore, b, 2, dependency));

                                if (beamPreserver.size() > beamWidth)
                                    beamPreserver.pollFirst();
                            }
                        }
                    }

                    if (canLeftArc) {
                        int headPos = sentence.posAt(configuration.state.bufferHead());
                        int depPos = sentence.posAt(configuration.state.peek());
                        for (int dependency : dependencyRelations) {
                            if ((!canShift && !canRightArc && !canReduce) || (rootFirst && canLeftArc) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                    && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                float score = classifier.score(features, 3 + dependencyRelations.size() + dependency, true);
                                float addedScore = score + prevScore;
                                beamPreserver.add(new BeamElement(addedScore, b, 3, dependency));

                                if (beamPreserver.size() > beamWidth)
                                    beamPreserver.pollFirst();
                            }
                        }
                    }
                }

                ArrayList<Configuration> repBeam = new ArrayList<Configuration>(beamWidth);
                for (BeamElement beamElement : beamPreserver.descendingSet()) {
                    if (repBeam.size() >= beamWidth)
                        break;
                    int b = beamElement.number;
                    int action = beamElement.action;
                    int label = beamElement.label;
                    float score = beamElement.score;

                    Configuration newConfig = beam.get(b).clone();

                    if (action == 0) {
                        ArcEager.shift(newConfig.state);
                        newConfig.addAction(0);
                    } else if (action == 1) {
                        ArcEager.reduce(newConfig.state);
                        newConfig.addAction(1);
                    } else if (action == 2) {
                        ArcEager.rightArc(newConfig.state, label);
                        newConfig.addAction(3 + label);
                    } else if (action == 3) {
                        ArcEager.leftArc(newConfig.state, label);
                        newConfig.addAction(3 + dependencyRelations.size() + label);
                    } else if (action == 4) {
                        ArcEager.unShift(newConfig.state);
                        newConfig.addAction(2);
                    }
                    newConfig.setScore(score);
                    repBeam.add(newConfig);
                }
                beam = repBeam;
            } else {
                Configuration configuration = beam.get(0);
                State currentState = configuration.state;
                long[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                float bestScore = Float.NEGATIVE_INFINITY;
                int bestAction = -1;

                boolean canShift = ArcEager.canDo(Actions.Shift, currentState);
                boolean canReduce = ArcEager.canDo(Actions.Reduce, currentState);
                boolean canRightArc = ArcEager.canDo(Actions.RightArc, currentState);
                boolean canLeftArc = ArcEager.canDo(Actions.LeftArc, currentState);

                if (!canShift
                        && !canReduce
                        && !canRightArc
                        && !canLeftArc) {

                    if (!currentState.stackEmpty()) {
                        ArcEager.unShift(currentState);
                        configuration.addAction(2);
                    } else if (!currentState.bufferEmpty() && currentState.stackEmpty()) {
                        ArcEager.shift(currentState);
                        configuration.addAction(0);
                    }
                }

                if (canShift) {
                    float score = classifier.score(features, 0, true);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAction = 0;
                    }
                }
                if (canReduce) {
                    float score = classifier.score(features, 1, true);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAction = 1;
                    }
                }
                if (canRightArc) {
                    int headPos = sentence.posAt(configuration.state.peek());
                    int depPos = sentence.posAt(configuration.state.bufferHead());
                    for (int dependency : dependencyRelations) {
                        if ((!canShift && !canLeftArc && !canReduce) || (rootFirst && canRightArc) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                            float score = classifier.score(features, 3 + dependency, true);
                            if (score > bestScore) {
                                bestScore = score;
                                bestAction = 3 + dependency;
                            }
                        }
                    }
                }
                if (ArcEager.canDo(Actions.LeftArc, currentState)) {
                    int headPos = sentence.posAt(configuration.state.bufferHead());
                    int depPos = sentence.posAt(configuration.state.peek());
                    for (int dependency : dependencyRelations) {
                        if ((!canShift && !canRightArc && !canReduce) || (rootFirst && canLeftArc) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                            float score = classifier.score(features, 3 + dependencyRelations.size() + dependency, true);
                            if (score > bestScore) {
                                bestScore = score;
                                bestAction = 3 + dependencyRelations.size() + dependency;
                            }
                        }
                    }
                }

                if (bestAction != -1) {
                    if (bestAction == 0) {
                        ArcEager.shift(configuration.state);
                    } else if (bestAction == (1)) {
                        ArcEager.reduce(configuration.state);
                    } else {

                        if (bestAction >= 3 + dependencyRelations.size()) {
                            int label = bestAction - (3 + dependencyRelations.size());
                            ArcEager.leftArc(configuration.state, label);
                        } else {
                            int label = bestAction - 3;
                            ArcEager.rightArc(configuration.state, label);
                        }
                    }
                    configuration.addScore(bestScore);
                    configuration.addAction(bestAction);
                }
                if (beam.size() == 0) {
                    System.out.println("WHY?");
                }
            }
        }

        Configuration bestConfiguration = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Configuration configuration : beam) {
            if (configuration.getScore(true) > bestScore) {
                bestScore = configuration.getScore(true);
                bestConfiguration = configuration;
            }
        }
        return new Pair<Configuration, Integer>(bestConfiguration, id);
    }
}
