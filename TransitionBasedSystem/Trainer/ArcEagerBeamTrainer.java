/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package TransitionBasedSystem.Trainer;

import Accessories.Pair;
import Learning.AveragedPerceptron;
import Learning.OnlineClassifier;
import Structures.Sentence;
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

    OnlineClassifier classifier;

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
    HashMap<String, HashMap<String, HashSet<String>>> headDepSet;


    Random randGen;

    public ArcEagerBeamTrainer(String updateMode, OnlineClassifier classifier, boolean rootFirst,
                               int beamWidth, ArrayList<String> dependencyRelations,
                               HashMap<String, HashMap<String, HashSet<String>>> headDepSet, int featureLength
            , boolean dynamicOracle, boolean isRandomDynamicSelection) {
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
    }

    public void train(ArrayList<GoldConfiguration> trainData, String devPath, int maxIteration, String modelPath, boolean lowerCased) throws Exception {
        for (int i = 1; i <= maxIteration; i++) {
            long start = System.currentTimeMillis();
            int all = 0;
            int allMatch = 0;
            int allArcs = 0;
            int correctUnlabeled = 0;
            int correctLabeled = 0;

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
                 * The double is the oracle's cost
                 * For more information see:
                 * Yoav Goldberg and Joakim Nivre. "Training Deterministic Parsers with Non-Deterministic Oracles."
                 * TACL 1 (2013): 403-414.
                 * for the mean while we just use zero-cost oracles
                 */
                HashMap<Configuration, Double> oracles = new HashMap<Configuration, Double>();

                oracles.put(firstOracle, 0.0);

                /**
                 * For keeping track of the violations
                 * For more information see:
                 * Liang Huang, Suphan Fayong and Yang Guo. "Structured perceptron with inexact search."
                 * In Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies,
                 * pp. 142-151. Association for Computational Linguistics, 2012.
                 */
                HashMap<Pair<Configuration, Configuration>, Double> violations = new HashMap<Pair<Configuration, Configuration>, Double>();
                Configuration bestScoringOracle = null;
                boolean oracleInBeam = false;

                while (!ArcEager.isTerminal(beam) && beam.size() > 0) {
                    /**
                     *  generating new oracles
                     *  it keeps the oracles which are in the terminal state
                     */
                    HashMap<Configuration, Double> newOracles = new HashMap<Configuration, Double>();
                    double bestScore = Double.NEGATIVE_INFINITY;

                    if (dynamicOracle) {
                        for (Configuration configuration : oracles.keySet()) {
                            if (!configuration.state.isTerminalState()) {
                                State currentState = configuration.state;
                                Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                                int accepted = 0;
                                // I only assumed that we need zero cost ones
                                if (goldConfiguration.actionCost("sh", "_", currentState) == 0) {
                                    Configuration newConfig = configuration.clone();
                                    double score = classifier.score(features, "sh", false);
                                    ArcEager.shift(newConfig.state);
                                    newConfig.addAction("sh");
                                    newConfig.addScore(score);
                                    newOracles.put(newConfig, (double) 0);

                                    if (newConfig.getScore(true) > bestScore) {
                                        bestScore = newConfig.getScore(true);
                                        bestScoringOracle = newConfig;
                                    }
                                    accepted++;
                                }
                                if (ArcEager.canDo("ra", currentState)) {
                                    for (String dependency : dependencyRelations) {
                                        if (goldConfiguration.actionCost("ra", dependency, currentState) == 0) {
                                            Configuration newConfig = configuration.clone();
                                            double score = classifier.score(features, "ra_" + dependency, false);
                                            ArcEager.rightArc(newConfig.state, dependency);
                                            newConfig.addAction("ra_" + dependency);
                                            newConfig.addScore(score);
                                            newOracles.put(newConfig, (double) 0);

                                            if (newConfig.getScore(true) > bestScore) {
                                                bestScore = newConfig.getScore(true);
                                                bestScoringOracle = newConfig;
                                            }
                                            accepted++;
                                        }
                                    }
                                }
                                if (ArcEager.canDo("la", currentState)) {
                                    for (String dependency : dependencyRelations) {
                                        if (goldConfiguration.actionCost("la", dependency, currentState) == 0) {
                                            Configuration newConfig = configuration.clone();
                                            double score = classifier.score(features, "la_" + dependency, false);
                                            ArcEager.leftArc(newConfig.state, dependency);
                                            newConfig.addAction("la_" + dependency);
                                            newConfig.addScore(score);
                                            newOracles.put(newConfig, (double) 0);

                                            if (newConfig.getScore(true) > bestScore) {
                                                bestScore = newConfig.getScore(true);
                                                bestScoringOracle = newConfig;
                                            }
                                            accepted++;
                                        }
                                    }
                                }
                                if (goldConfiguration.actionCost("rd", "_", currentState) == 0) {
                                    Configuration newConfig = configuration.clone();
                                    double score = classifier.score(features, "rd", false);
                                    ArcEager.reduce(newConfig.state);
                                    newConfig.addAction("rd");
                                    newConfig.addScore(score);
                                    newOracles.put(newConfig, (double) 0);

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
                            Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                            if (!state.stackEmpty())
                                top = state.peek();
                            if (!state.bufferEmpty())
                                first = state.bufferHead();

                            if (!configuration.state.isTerminalState()) {
                                Configuration newConfig = configuration.clone();

                                if (first > 0 && goldDependencies.containsKey(first) && goldDependencies.get(first).first == top) {
                                    String dependency = goldDependencies.get(first).second;
                                    double score = classifier.score(features, "ra_" + dependency, false);
                                    ArcEager.rightArc(newConfig.state, dependency);
                                    newConfig.addAction("ra_" + dependency);
                                    newConfig.addScore(score);
                                } else if (top > 0 && goldDependencies.containsKey(top) && goldDependencies.get(top).first == first) {
                                    String dependency = goldDependencies.get(top).second;
                                    double score = classifier.score(features, "la_" + dependency, false);
                                    ArcEager.leftArc(newConfig.state, dependency);
                                    newConfig.addAction("la_" + dependency);
                                    newConfig.addScore(score);
                                } else if (state.hasHead(top)) {
                                    if (reversedDependencies.containsKey(top)) {
                                        if (reversedDependencies.get(top).size() == state.valence(top)) {
                                            double score = classifier.score(features, "rd", false);
                                            ArcEager.reduce(newConfig.state);
                                            newConfig.addAction("rd");
                                            newConfig.addScore(score);
                                        } else {
                                            double score = classifier.score(features, "sh", false);
                                            ArcEager.shift(newConfig.state);
                                            newConfig.addAction("sh");
                                            newConfig.addScore(score);
                                        }
                                    } else {
                                        double score = classifier.score(features, "rd", false);
                                        ArcEager.reduce(newConfig.state);
                                        newConfig.addAction("rd");
                                        newConfig.addScore(score);
                                    }

                                } else if (state.bufferEmpty() && state.stackSize() == 1 && state.peek() == state.rootIndex) {
                                    double score = classifier.score(features, "rd", false);
                                    ArcEager.reduce(newConfig.state);
                                    newConfig.addAction("rd");
                                    newConfig.addScore(score);
                                } else {
                                    double score = classifier.score(features, "sh", false);
                                    ArcEager.shift(newConfig.state);
                                    newConfig.addAction("sh");
                                    newConfig.addScore(score);
                                }
                                bestScoringOracle = newConfig;
                                newOracles.put(newConfig, (double) 0);
                            } else {
                                newOracles.put(configuration, oracles.get(configuration));
                            }
                        }
                    }
                    if (newOracles.size() == 0) {
                        System.err.println("no oracle");
                    }
                    oracles = newOracles;

                    TreeMap<Double, ArrayList<Pair<Integer, Pair<String, Double>>>> beamPreserver = new TreeMap<Double, ArrayList<Pair<Integer, Pair<String, Double>>>>();

                    for (int b = 0; b < beam.size(); b++) {
                        Configuration configuration = beam.get(b);
                        State currentState = configuration.state;
                        double prevScore = configuration.score;
                        boolean canShift = ArcEager.canDo("sh", currentState);
                        boolean canReduce = ArcEager.canDo("rd", currentState);
                        boolean canRightArc = ArcEager.canDo("ra", currentState);
                        boolean canLeftArc = ArcEager.canDo("la", currentState);
                        Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);

                        if (canShift) {
                            double score = classifier.score(features, "sh", false);
                            double addedScore = score + prevScore;
                            if (!beamPreserver.containsKey(addedScore))
                                beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                            beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("sh", score)));
                        }
                        if (canReduce) {
                            double score = classifier.score(features, "rd", false);
                            double addedScore = score + prevScore;
                            if (!beamPreserver.containsKey(addedScore))
                                beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                            beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("rd", score)));
                        }

                        if (canRightArc) {
                            String headPos = sentence.posAt(configuration.state.peek());
                            String depPos = sentence.posAt(configuration.state.bufferHead());
                            for (String dependency : dependencyRelations) {
                                if ((!canLeftArc && !canShift && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                        && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                    double score = classifier.score(features, "ra_" + dependency, false);
                                    double addedScore = score + prevScore;
                                    if (!beamPreserver.containsKey(addedScore))
                                        beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                                    beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("ra_" + dependency, score)));
                                }
                            }
                        }
                        if (canLeftArc) {
                            String headPos = sentence.posAt(configuration.state.bufferHead());
                            String depPos = sentence.posAt(configuration.state.peek());
                            for (String dependency : dependencyRelations) {
                                if ((!canShift && !canRightArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                        && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                    double score = classifier.score(features, "la_" + dependency, false);
                                    double addedScore = score + prevScore;
                                    if (!beamPreserver.containsKey(addedScore))
                                        beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                                    beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("la_" + dependency, score)));
                                }
                            }
                        }

                    }

                    if (beamPreserver.size() == 0 || beam.size() == 0) {
                        break;
                    } else {
                        oracleInBeam = false;

                        ArrayList<Configuration> repBeam = new ArrayList<Configuration>(beamWidth);
                        for (double sc : beamPreserver.descendingKeySet()) {
                            if (repBeam.size() >= beamWidth)
                                break;
                            ArrayList<Pair<Integer, Pair<String, Double>>> values = beamPreserver.get(sc);
                            for (Pair<Integer, Pair<String, Double>> val : values) {
                                if (repBeam.size() >= beamWidth)
                                    break;
                                int b = val.first;
                                String action = val.second.first;
                                double score = val.second.second;

                                Configuration newConfig = beam.get(b).clone();

                                if (action.startsWith("sh")) {
                                    ArcEager.shift(newConfig.state);
                                } else if (action.startsWith("rd")) {
                                    ArcEager.reduce(newConfig.state);
                                } else if (action.startsWith("ra")) {
                                    String label = action.split("_")[1];
                                    ArcEager.rightArc(newConfig.state, label);
                                } else if (action.startsWith("la")) {
                                    String label = action.split("_")[1];
                                    ArcEager.leftArc(newConfig.state, label);
                                } else if (action.equals("unshift")) {
                                    ArcEager.unShift(newConfig.state);
                                }
                                newConfig.addAction(action);
                                newConfig.addScore(score);
                                repBeam.add(newConfig);

                                if (oracles.containsKey(newConfig))
                                    oracleInBeam = true;
                            }

                        }
                        beam = repBeam;


                        if (beam.size() > 0) {
                            Configuration bestConfig = beam.get(0);
                            if (oracles.containsKey(bestConfig)) {
                                oracles = new HashMap<Configuration, Double>();
                                oracles.put(bestConfig, 0.0);
                            } else {

                                oracles = new HashMap<Configuration, Double>();
                                if (isRandomDynamicSelection) { // choosing randomly, otherwise using latent structured Perceptron
                                    List<Configuration> keys = new ArrayList<Configuration>(oracles.keySet());
                                    Configuration randomKey = keys.get(randGen.nextInt(keys.size()));
                                    oracles.put(randomKey, 0.0);
                                    bestScoringOracle = randomKey;
                                }
                                oracles.put(bestScoringOracle, 0.0);
                            }


                            // do early update
                            if (!oracleInBeam && updateMode.equals("early"))
                                break;

                            // keep violations
                            if (beam.size() > 0 && !oracleInBeam && updateMode.equals("max_violation")) {
                                double violation = beam.get(0).getScore(true) - bestScoringOracle.getScore(true);//Math.abs(beam.get(0).getScore(true) - bestScoringOracle.getScore(true));
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
                        double maxViolation = Double.NEGATIVE_INFINITY;
                        double violation = beam.get(0).getScore(true) - bestScoringOracle.getScore(true); //Math.abs(beam.get(0).getScore(true) - bestScoringOracle.getScore(true));
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
                        oracleFeatures[f] = new HashMap<String, Double>();
                        predictedFeatures[f] = new HashMap<String, Double>();
                    }

                    Configuration predictedConfiguration = initialConfiguration.clone();
                    Configuration oracleConfiguration = initialConfiguration.clone();

                    for (String action : finalOracle.actionHistory) {
                        Object[] feats = FeatureExtractor.extractAllParseFeatures(oracleConfiguration, featureLength);
                        for (int f = 0; f < feats.length; f++) {
                            Pair<String, Double> pair = (Pair<String, Double>) feats[f];
                            String featName = action + ":" + pair.first;
                            HashMap<String, Double> map = (HashMap<String, Double>) oracleFeatures[f];
                            if (!map.containsKey(featName))
                                map.put(featName, pair.second);
                            else
                                map.put(featName, map.get(featName) + pair.second);
                        }


                        if (action.equals("sh")) {
                            ArcEager.shift(oracleConfiguration.state);
                        } else if (action.equals("rd")) {
                            ArcEager.reduce(oracleConfiguration.state);
                        } else if (action.startsWith("la")) {
                            String dependency = action.split("_")[1];
                            ArcEager.leftArc(oracleConfiguration.state, dependency);
                        } else if (action.startsWith("ra")) {
                            String dependency = action.split("_")[1];
                            ArcEager.rightArc(oracleConfiguration.state, dependency);
                        }
                    }


                    for (String action : predicted.actionHistory) {
                        Object[] feats = FeatureExtractor.extractAllParseFeatures(predictedConfiguration, featureLength);
                        if (!action.equals("unshift")) // do not take into account for unshift
                            for (int f = 0; f < feats.length; f++) {
                                Pair<String, Double> pair = (Pair<String, Double>) feats[f];
                                String featName = action + ":" + pair.first;
                                HashMap<String, Double> map = (HashMap<String, Double>) predictedFeatures[f];
                                if (!map.containsKey(featName))
                                    map.put(featName, pair.second);
                                else
                                    map.put(featName, map.get(featName) + pair.second);
                            }
                        State state = predictedConfiguration.state;
                        if (action.equals("sh")) {
                            ArcEager.shift(state);
                        } else if (action.equals("rd")) {
                            ArcEager.reduce(state);
                        } else if (action.startsWith("la")) {
                            String dependency = action.split("_")[1];
                            ArcEager.leftArc(state, dependency);
                        } else if (action.startsWith("ra")) {
                            String dependency = action.split("_")[1];
                            ArcEager.rightArc(state, dependency);
                        } else if (action.equals("unshift")) {
                            ArcEager.unShift(state);
                        }
                    }

                    for (int f = 0; f < predictedFeatures.length; f++) {
                        HashMap<String, Double> map = (HashMap<String, Double>) predictedFeatures[f];
                        HashMap<String, Double> map2 = (HashMap<String, Double>) oracleFeatures[f];
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
                    allArcs += goldConfiguration.getGoldDependencies().size();
                    for (int dependent : goldConfiguration.getGoldDependencies().keySet()) {
                        if (predicted.state.getHead(dependent) == goldConfiguration.getGoldDependencies().get(dependent).first) {
                            correctUnlabeled += 1;
                            if (predicted.state.getDependency(dependent).equals(goldConfiguration.getGoldDependencies().get(dependent).second))
                                correctLabeled += 1;
                        }
                    }
                } else {
                    allArcs += goldConfiguration.getGoldDependencies().size();
                    correctUnlabeled += goldConfiguration.getGoldDependencies().size();
                    correctLabeled += goldConfiguration.getGoldDependencies().size();
                    allMatch++;
                }

                classifier.incrementIteration();
                all++;

            }
            System.out.print("\n");
            long end = System.currentTimeMillis();
            long timeSec = (end - start) / 1000;
            System.out.println(timeSec);
            classifier.saveModel(modelPath + "_iter" + i);

            double labeledAccuracy = (double) correctLabeled / allArcs;
            double unlabeledAccuracy = (double) correctUnlabeled / allArcs;
            System.out.println("\nsaved iteration " + i + " with " + classifier.size() + " features with accuracy " + ((double) (100 * allMatch) / all));
            System.out.println(unlabeledAccuracy + "\t" + labeledAccuracy);

            if (!devPath.equals("")) {
                AveragedPerceptron averagedPerceptron = (AveragedPerceptron) AveragedPerceptron.loadModel(modelPath + "_iter" + i, 1);
                KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyRelations, headDepSet, featureLength);

                parser.parseConllFile(devPath,
                        rootFirst, beamWidth, lowerCased);
            }


        }
    }
}
