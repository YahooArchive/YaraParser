/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package TransitionBasedSystem.Trainer;

import Accessories.Evaluator;
import Accessories.Pair;
import Learning.AveragedPerceptron;
import Structures.IndexMaps;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.BeamElement;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.GoldConfiguration;
import TransitionBasedSystem.Configuration.State;
import TransitionBasedSystem.Features.FeatureExtractor;
import TransitionBasedSystem.Parser.ArcEager;
import TransitionBasedSystem.Parser.KBeamArcEagerParser;

import java.util.*;

public class ArcEagerBeamTrainer {
    /**
     * Can be either "early", "max_violation" or "late"
     * For more information read:
     * Liang Huang, Suphan Fayong and Yang Guo. "Structured perceptron with inexact search."
     * In Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies,
     * pp. 142-151. Association for Computational Linguistics, 2012.
     */
    String updateMode;

    AveragedPerceptron classifier;

    /**
     * Can configure such that we can place the ROOT token either in the first or last place
     * For more information see:
     * Miguel Ballesteros and Joakim Nivre. "Going to the roots of dependency parsing."
     * Computational Linguistics 39, no. 1 (2013): 5-13.
     */
    boolean rootFirst;

    int beamWidth;

    ArrayList<String> dependencyRelations;

    int featureLength;

    boolean dynamicOracle;

    boolean isRandomDynamicSelection;


    // for pruning irrelevant search space
    HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet;


    Random randGen;
    IndexMaps maps;

    public ArcEagerBeamTrainer(String updateMode, AveragedPerceptron classifier, boolean rootFirst,
                               int beamWidth, ArrayList<String> dependencyRelations,
                               HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet, int featureLength
            , boolean dynamicOracle, boolean isRandomDynamicSelection, IndexMaps maps) {
        this.updateMode = updateMode;
        this.classifier = classifier;
        this.rootFirst = rootFirst;
        this.beamWidth = beamWidth;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        this.dynamicOracle = dynamicOracle;
        randGen = new Random();
        this.isRandomDynamicSelection = isRandomDynamicSelection;
        this.headDepSet = headDepSet;
        this.maps = maps;
    }

    public void train(ArrayList<GoldConfiguration> trainData, String devPath, int maxIteration, String modelPath, boolean lowerCased) throws Exception {
        for (int i = 1; i <= maxIteration; i++) {
            long start = System.currentTimeMillis();

            int dataCount = 0;

            for (GoldConfiguration goldConfiguration : trainData) {
                dataCount++;
                if (dataCount % 100 == 0)
                    System.out.print(dataCount + "...");

                Sentence sentence = goldConfiguration.getSentence();

                Configuration initialConfiguration = new Configuration(goldConfiguration.getSentence(), rootFirst);
                Configuration firstOracle = initialConfiguration.clone();
                ArrayList<Configuration> beam = new ArrayList<Configuration>(beamWidth);
                beam.add(initialConfiguration);

                /**
                 * The float is the oracle's cost
                 * For more information see:
                 * Yoav Goldberg and Joakim Nivre. "Training Deterministic Parsers with Non-Deterministic Oracles."
                 * TACL 1 (2013): 403-414.
                 * for the mean while we just use zero-cost oracles
                 */
                HashMap<Configuration, Float> oracles = new HashMap<Configuration, Float>();

                oracles.put(firstOracle, 0.0f);

                /**
                 * For keeping track of the violations
                 * For more information see:
                 * Liang Huang, Suphan Fayong and Yang Guo. "Structured perceptron with inexact search."
                 * In Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies,
                 * pp. 142-151. Association for Computational Linguistics, 2012.
                 */
                HashMap<Pair<Configuration, Configuration>, Float> violations = new HashMap<Pair<Configuration, Configuration>, Float>();
                Configuration bestScoringOracle = null;
                boolean oracleInBeam = false;

                while (!ArcEager.isTerminal(beam) && beam.size() > 0) {
                    /**
                     *  generating new oracles
                     *  it keeps the oracles which are in the terminal state
                     */
                    HashMap<Configuration, Float> newOracles = new HashMap<Configuration, Float>();
                    float bestScore = Float.NEGATIVE_INFINITY;

                    if (dynamicOracle) {
                        for (Configuration configuration : oracles.keySet()) {
                            if (!configuration.state.isTerminalState()) {
                                State currentState = configuration.state;
                                String[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                                int accepted = 0;
                                // I only assumed that we need zero cost ones
                                if (goldConfiguration.actionCost(0, "_", currentState) == 0) {
                                    Configuration newConfig = configuration.clone();
                                    float score = classifier.score(features, "sh", false);
                                    ArcEager.shift(newConfig.state);
                                    newConfig.addAction("sh");
                                    newConfig.addScore(score);
                                    newOracles.put(newConfig, (float) 0);

                                    if (newConfig.getScore(true) > bestScore) {
                                        bestScore = newConfig.getScore(true);
                                        bestScoringOracle = newConfig;
                                    }
                                    accepted++;
                                }
                                if (ArcEager.canDo(2, currentState)) {
                                    for (String dependency : dependencyRelations) {
                                        if (goldConfiguration.actionCost(2, dependency, currentState) == 0) {
                                            Configuration newConfig = configuration.clone();
                                            float score = classifier.score(features, "ra_" + dependency, false);
                                            ArcEager.rightArc(newConfig.state, dependency);
                                            newConfig.addAction("ra_" + dependency);
                                            newConfig.addScore(score);
                                            newOracles.put(newConfig, (float) 0);

                                            if (newConfig.getScore(true) > bestScore) {
                                                bestScore = newConfig.getScore(true);
                                                bestScoringOracle = newConfig;
                                            }
                                            accepted++;
                                        }
                                    }
                                }
                                if (ArcEager.canDo(3, currentState)) {
                                    for (String dependency : dependencyRelations) {
                                        if (goldConfiguration.actionCost(3, dependency, currentState) == 0) {
                                            Configuration newConfig = configuration.clone();
                                            float score = classifier.score(features, "la_" + dependency, false);
                                            ArcEager.leftArc(newConfig.state, dependency);
                                            newConfig.addAction("la_" + dependency);
                                            newConfig.addScore(score);
                                            newOracles.put(newConfig, (float) 0);

                                            if (newConfig.getScore(true) > bestScore) {
                                                bestScore = newConfig.getScore(true);
                                                bestScoringOracle = newConfig;
                                            }
                                            accepted++;
                                        }
                                    }
                                }
                                if (goldConfiguration.actionCost(1, "_", currentState) == 0) {
                                    Configuration newConfig = configuration.clone();
                                    float score = classifier.score(features, "rd", false);
                                    ArcEager.reduce(newConfig.state);
                                    newConfig.addAction("rd");
                                    newConfig.addScore(score);
                                    newOracles.put(newConfig, (float) 0);

                                    if (newConfig.getScore(true) > bestScore) {
                                        bestScore = newConfig.getScore(true);
                                        bestScoringOracle = newConfig;
                                    }
                                    accepted++;
                                }
                                if (accepted > 4) {
                                    System.out.println("why?");
                                }
                            } else {
                                newOracles.put(configuration, oracles.get(configuration));
                            }
                        }
                    } else {
                        int top = -1;
                        int first = -1;
                        HashMap<Integer, Pair<Integer, String>> goldDependencies = goldConfiguration.getGoldDependencies();
                        HashMap<Integer, HashSet<Integer>> reversedDependencies = goldConfiguration.getReversedDependencies();

                        for (Configuration configuration : oracles.keySet()) {
                            State state = configuration.state;
                            String[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                            if (!state.stackEmpty())
                                top = state.peek();
                            if (!state.bufferEmpty())
                                first = state.bufferHead();

                            if (!configuration.state.isTerminalState()) {
                                Configuration newConfig = configuration.clone();

                                if (first > 0 && goldDependencies.containsKey(first) && goldDependencies.get(first).first == top) {
                                    String dependency = goldDependencies.get(first).second;
                                    float score = classifier.score(features, "ra_" + dependency, false);
                                    ArcEager.rightArc(newConfig.state, dependency);
                                    newConfig.addAction("ra_" + dependency);
                                    newConfig.addScore(score);
                                } else if (top > 0 && goldDependencies.containsKey(top) && goldDependencies.get(top).first == first) {
                                    String dependency = goldDependencies.get(top).second;
                                    float score = classifier.score(features, "la_" + dependency, false);
                                    ArcEager.leftArc(newConfig.state, dependency);
                                    newConfig.addAction("la_" + dependency);
                                    newConfig.addScore(score);
                                } else if (state.hasHead(top)) {
                                    if (reversedDependencies.containsKey(top)) {
                                        if (reversedDependencies.get(top).size() == state.valence(top)) {
                                            float score = classifier.score(features, "rd", false);
                                            ArcEager.reduce(newConfig.state);
                                            newConfig.addAction("rd");
                                            newConfig.addScore(score);
                                        } else {
                                            float score = classifier.score(features, "sh", false);
                                            ArcEager.shift(newConfig.state);
                                            newConfig.addAction("sh");
                                            newConfig.addScore(score);
                                        }
                                    } else {
                                        float score = classifier.score(features, "rd", false);
                                        ArcEager.reduce(newConfig.state);
                                        newConfig.addAction("rd");
                                        newConfig.addScore(score);
                                    }

                                } else if (state.bufferEmpty() && state.stackSize() == 1 && state.peek() == state.rootIndex) {
                                    float score = classifier.score(features, "rd", false);
                                    ArcEager.reduce(newConfig.state);
                                    newConfig.addAction("rd");
                                    newConfig.addScore(score);
                                } else {
                                    float score = classifier.score(features, "sh", false);
                                    ArcEager.shift(newConfig.state);
                                    newConfig.addAction("sh");
                                    newConfig.addScore(score);
                                }
                                bestScoringOracle = newConfig;
                                newOracles.put(newConfig, (float) 0);
                            } else {
                                newOracles.put(configuration, oracles.get(configuration));
                            }
                        }
                    }
                    if (newOracles.size() == 0) {
                        System.err.println("no oracle");
                    }
                    oracles = newOracles;

                    TreeSet<BeamElement> beamPreserver = new TreeSet<BeamElement>();

                    for (int b = 0; b < beam.size(); b++) {
                        Configuration configuration = beam.get(b);
                        State currentState = configuration.state;
                        float prevScore = configuration.score;
                        boolean canShift = ArcEager.canDo(0, currentState);
                        boolean canReduce = ArcEager.canDo(1, currentState);
                        boolean canRightArc = ArcEager.canDo(2, currentState);
                        boolean canLeftArc = ArcEager.canDo(3, currentState);
                        String[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);

                        if (canShift) {
                            float score = classifier.score(features, "sh", false);
                            float addedScore = score + prevScore;
                            beamPreserver.add(new BeamElement(addedScore,b,0,""));

                            if (beamPreserver.size() > beamWidth)
                                beamPreserver.pollFirst();
                        }
                        if (canReduce) {
                            float score = classifier.score(features, "rd", false);
                            float addedScore = score + prevScore;
                            beamPreserver.add(new BeamElement(addedScore,b,1,""));

                            if (beamPreserver.size() > beamWidth)
                                beamPreserver.pollFirst();
                        }

                        if (canRightArc) {
                            int headPos = sentence.posAt(configuration.state.peek());
                            int depPos = sentence.posAt(configuration.state.bufferHead());
                            for (String dependency : dependencyRelations) {
                                if ((!canLeftArc && !canShift && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                        && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                    float score = classifier.score(features, "ra_" + dependency, false);
                                    float addedScore = score + prevScore;
                                    beamPreserver.add(new BeamElement(addedScore,b,2,dependency));

                                    if (beamPreserver.size() > beamWidth)
                                        beamPreserver.pollFirst();
                                }
                            }
                        }
                        if (canLeftArc) {
                            int headPos = sentence.posAt(configuration.state.bufferHead());
                            int depPos = sentence.posAt(configuration.state.peek());
                            for (String dependency : dependencyRelations) {
                                if ((!canShift && !canRightArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                        && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                    float score = classifier.score(features, "la_" + dependency, false);
                                    float addedScore = score + prevScore;
                                    beamPreserver.add(new BeamElement(addedScore,b,3,dependency));

                                    if (beamPreserver.size() > beamWidth)
                                        beamPreserver.pollFirst();
                                }
                            }
                        }

                    }

                    if (beamPreserver.size() == 0 || beam.size() == 0) {
                        break;
                    } else {
                        oracleInBeam = false;

                        ArrayList<Configuration> repBeam = new ArrayList<Configuration>(beamWidth);
                        for (BeamElement beamElement: beamPreserver.descendingSet()) {
                            if (repBeam.size() >= beamWidth)
                                break;
                                int b = beamElement.number;
                                int action = beamElement.action;
                            String label=beamElement.label;
                            float sc=beamElement.score;

                                Configuration newConfig = beam.get(b).clone();

                                if (action==0) {
                                    ArcEager.shift(newConfig.state);
                                    newConfig.addAction("sh");
                                } else if (action==1) {
                                    ArcEager.reduce(newConfig.state);
                                    newConfig.addAction("rd");
                                } else if (action==2) {
                                    ArcEager.rightArc(newConfig.state, label);
                                    newConfig.addAction("ra_"+label);
                                } else if (action==3) {
                                    ArcEager.leftArc(newConfig.state, label);
                                    newConfig.addAction("la_"+label);
                                } else if (action==4) {
                                    ArcEager.unShift(newConfig.state);
                                    newConfig.addAction("us");
                                }
                                newConfig.setScore(sc);
                                repBeam.add(newConfig);

                                if (oracles.containsKey(newConfig))
                                    oracleInBeam = true;

                        }
                        beam = repBeam;


                        if (beam.size() > 0) {
                            Configuration bestConfig = beam.get(0);
                            if (oracles.containsKey(bestConfig)) {
                                oracles = new HashMap<Configuration, Float>();
                                oracles.put(bestConfig, 0.0f);
                            } else {

                                oracles = new HashMap<Configuration, Float>();
                                if (isRandomDynamicSelection) { // choosing randomly, otherwise using latent structured Perceptron
                                    List<Configuration> keys = new ArrayList<Configuration>(oracles.keySet());
                                    Configuration randomKey = keys.get(randGen.nextInt(keys.size()));
                                    oracles.put(randomKey, 0.0f);
                                    bestScoringOracle = randomKey;
                                }
                                oracles.put(bestScoringOracle, 0.0f);
                            }


                            // do early update
                            if (!oracleInBeam && updateMode.equals("early"))
                                break;

                            // keep violations
                            if (beam.size() > 0 && !oracleInBeam && updateMode.equals("max_violation")) {
                                float violation = beam.get(0).getScore(true) - bestScoringOracle.getScore(true);//Math.abs(beam.get(0).getScore(true) - bestScoringOracle.getScore(true));
                                violations.put(new Pair<Configuration, Configuration>(beam.get(0), bestScoringOracle), violation);
                            }
                        } else
                            break;
                    }

                }

                // updating weights
                if (!oracleInBeam || !bestScoringOracle.equals(beam.get(0))) {
                    Configuration predicted = null;
                    Configuration finalOracle = null;
                    if (!updateMode.equals("max_violation")) {
                        finalOracle = bestScoringOracle;
                        predicted = beam.get(0);
                    } else {
                        float maxViolation = Float.NEGATIVE_INFINITY;
                        float violation = beam.get(0).getScore(true) - bestScoringOracle.getScore(true); //Math.abs(beam.get(0).getScore(true) - bestScoringOracle.getScore(true));
                        violations.put(new Pair<Configuration, Configuration>(beam.get(0), bestScoringOracle), violation);

                        for (Pair<Configuration, Configuration> pair : violations.keySet()) {
                            if (violations.get(pair) > maxViolation) {
                                maxViolation = violations.get(pair);
                                predicted = pair.first;
                                finalOracle = pair.second;
                            }
                        }
                    }

                    Object[] predictedFeatures = new Object[featureLength];
                    Object[] oracleFeatures = new Object[featureLength];
                    for (int f = 0; f < predictedFeatures.length; f++) {
                        oracleFeatures[f] = new HashMap<String, Float>();
                        predictedFeatures[f] = new HashMap<String, Float>();
                    }

                    Configuration predictedConfiguration = initialConfiguration.clone();
                    Configuration oracleConfiguration = initialConfiguration.clone();

                    for (String action : finalOracle.actionHistory) {
                        Object[] feats = FeatureExtractor.extractAllParseFeatures(oracleConfiguration, featureLength);
                        for (int f = 0; f < feats.length; f++) {
                            String featName = action + ":" + feats[f];
                            HashMap<String, Float> map = (HashMap<String, Float>) oracleFeatures[f];
                            Float value = map.get(featName);
                            if (value == null)
                                map.put(featName, 1.0f);
                            else
                                map.put(featName, value + 1);
                        }


                        if (action.equals("sh")) {
                            ArcEager.shift(oracleConfiguration.state);
                        } else if (action.equals("rd")) {
                            ArcEager.reduce(oracleConfiguration.state);
                        } else if (action.startsWith("la")) {
                            String dependency = action.substring(3);
                            ArcEager.leftArc(oracleConfiguration.state, dependency);
                        } else if (action.startsWith("ra")) {
                            String dependency = action.substring(3);
                            ArcEager.rightArc(oracleConfiguration.state, dependency);
                        }
                    }


                    for (String action : predicted.actionHistory) {
                        Object[] feats = FeatureExtractor.extractAllParseFeatures(predictedConfiguration, featureLength);
                        if (!action.equals("us")) // do not take into account for unshift
                            for (int f = 0; f < feats.length; f++) {
                                String featName = action + ":" + feats[f];
                                HashMap<String, Float> map = (HashMap<String, Float>) predictedFeatures[f];
                                Float value = map.get(featName);
                                if (value == null)
                                    map.put(featName, 1.f);
                                else
                                    map.put(featName, map.get(featName) + 1);
                            }
                        State state = predictedConfiguration.state;
                        if (action.equals("sh")) {
                            ArcEager.shift(state);
                        } else if (action.equals("rd")) {
                            ArcEager.reduce(state);
                        } else if (action.startsWith("la")) {
                            String dependency = action.substring(3);
                            ArcEager.leftArc(state, dependency);
                        } else if (action.startsWith("ra")) {
                            String dependency = action.substring(3);
                            ArcEager.rightArc(state, dependency);
                        } else if (action.equals("us")) {
                            ArcEager.unShift(state);
                        }
                    }

                    for (int f = 0; f < predictedFeatures.length; f++) {
                        HashMap<String, Float> map = (HashMap<String, Float>) predictedFeatures[f];
                        HashMap<String, Float> map2 = (HashMap<String, Float>) oracleFeatures[f];
                        for (String feat : map.keySet()) {
                            int labIndex = feat.indexOf(":");
                            String action = feat.substring(0, labIndex);
                            String feature = feat.substring(labIndex + 1);
                            if (!(map2.containsKey(feat) && map2.get(feat).equals(map.get(feat))))
                                classifier.changeWeight(f, feature, action, -map.get(feat));
                        }

                        for (String feat : map2.keySet()) {

                            int labIndex = feat.indexOf(":");
                            String action = feat.substring(0, labIndex);
                            String feature = feat.substring(labIndex + 1);

                            if (!(map.containsKey(feat) && map.get(feat).equals(map2.get(feat))))
                                classifier.changeWeight(f, feature, action, map2.get(feat));
                        }
                    }
                }
                classifier.incrementIteration();
            }
            System.out.print("\n");
            long end = System.currentTimeMillis();
            long timeSec = (end - start) / 1000;
            System.out.println("this iteration took " + timeSec + " seconds\n");
            classifier.saveModel(modelPath + "_iter" + i);

            System.out.println("\nsaved iteration " + i + " with " + classifier.size() + " features\n");

            if (!devPath.equals("")) {
                AveragedPerceptron averagedPerceptron = AveragedPerceptron.loadModel(modelPath + "_iter" + i, 1);
                KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyRelations, headDepSet, featureLength, maps);

                parser.parseConllFile(devPath, devPath+".tmp",
                        rootFirst, beamWidth, lowerCased);
                Evaluator.evaluate(devPath,devPath+".tmp");
            }


        }
    }
}
