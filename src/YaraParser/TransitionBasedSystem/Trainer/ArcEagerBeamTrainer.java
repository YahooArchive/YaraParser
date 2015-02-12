/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package YaraParser.TransitionBasedSystem.Trainer;

import YaraParser.Accessories.Evaluator;
import YaraParser.Accessories.Options;
import YaraParser.Accessories.Pair;
import YaraParser.Learning.AveragedPerceptron;
import YaraParser.Structures.IndexMaps;
import YaraParser.Structures.InfStruct;
import YaraParser.Structures.Sentence;
import YaraParser.TransitionBasedSystem.Configuration.BeamElement;
import YaraParser.TransitionBasedSystem.Configuration.Configuration;
import YaraParser.TransitionBasedSystem.Configuration.GoldConfiguration;
import YaraParser.TransitionBasedSystem.Configuration.State;
import YaraParser.TransitionBasedSystem.Features.FeatureExtractor;
import YaraParser.TransitionBasedSystem.Parser.Actions;
import YaraParser.TransitionBasedSystem.Parser.ArcEager;
import YaraParser.TransitionBasedSystem.Parser.BeamScorerThread;
import YaraParser.TransitionBasedSystem.Parser.KBeamArcEagerParser;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArcEagerBeamTrainer {
    Options options;
    /**
     * Can be either "early" or "max_violation"
     * For more information read:
     * Liang Huang, Suphan Fayong and Yang Guo. "Structured perceptron with inexact search."
     * In Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies,
     * pp. 142-151. Association for Computational Linguistics, 2012.
     */
    private String updateMode;
    private AveragedPerceptron classifier;
   
    private ArrayList<Integer> dependencyRelations;
    private int featureLength;

    private Random randGen;
    private IndexMaps maps;

    public ArcEagerBeamTrainer(String updateMode, AveragedPerceptron classifier, Options options,
                               ArrayList<Integer> dependencyRelations,
                               int featureLength
            , IndexMaps maps) {
        this.updateMode = updateMode;
        this.classifier = classifier;
        this.options = options;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        randGen = new Random();
        this.maps = maps;
    }

    public void train(ArrayList<GoldConfiguration> trainData, String devPath, int maxIteration, String modelPath, boolean lowerCased, HashSet<String> punctuations, int partialTreeIter) throws Exception {
        /**
         * Actions: 0=shift, 1=reduce, 2=unshift, ra_dep=3+dep, la_dep=3+dependencyRelations.size()+dep
         */
        ExecutorService executor = Executors.newFixedThreadPool(options.numOfThreads);
        CompletionService<ArrayList<BeamElement>> pool = new ExecutorCompletionService<ArrayList<BeamElement>>(executor);


        for (int i = 1; i <= maxIteration; i++) {
            long start = System.currentTimeMillis();

            int dataCount = 0;

            for (GoldConfiguration goldConfiguration : trainData) {
                dataCount++;
                if (dataCount % 1000 == 0)
                    System.out.print(dataCount + "...");
                trainOnOneSample(goldConfiguration, partialTreeIter, i, dataCount, pool);

                classifier.incrementIteration();
            }
            System.out.print("\n");
            long end = System.currentTimeMillis();
            long timeSec = (end - start) / 1000;
            System.out.println("iteration "+i+" took " + timeSec + " seconds\n");

            System.out.print("saving the model...");
            InfStruct infStruct = new InfStruct(classifier, maps, dependencyRelations, options);
            infStruct.saveModel(modelPath + "_iter" + i);

            System.out.println("done\n");

            if (!devPath.equals("")) {
                AveragedPerceptron averagedPerceptron = new AveragedPerceptron(infStruct);

                int raSize = averagedPerceptron.raSize();
                int effectiveRaSize = averagedPerceptron.effectiveRaSize();
                float raRatio = 100.0f * effectiveRaSize / raSize;

                int laSize = averagedPerceptron.laSize();
                int effectiveLaSize = averagedPerceptron.effectiveLaSize();
                float laRatio = 100.0f * effectiveLaSize / laSize;

                DecimalFormat format = new DecimalFormat("##.00");
                System.out.println("size of RA features in memory:" + effectiveRaSize + "/" + raSize + "->" + format.format(raRatio) + "%");
                System.out.println("size of LA features in memory:" + effectiveLaSize + "/" + laSize + "->" + format.format(laRatio) + "%");
                KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyRelations, featureLength, maps, options.numOfThreads);

                parser.parseConllFile(devPath, modelPath + ".__tmp__",
                        options.rootFirst, options.beamWidth, true, lowerCased, options.numOfThreads, false, "");
                Evaluator.evaluate(devPath, modelPath + ".__tmp__", punctuations);
                parser.shutDownLiveThreads();
            }
        }
        boolean isTerminated = executor.isTerminated();
        while (!isTerminated) {
            executor.shutdownNow();
            isTerminated = executor.isTerminated();
        }
    }

    private void trainOnOneSample(GoldConfiguration goldConfiguration, int partialTreeIter, int i, int dataCount, CompletionService<ArrayList<BeamElement>> pool) throws Exception {
        boolean isPartial = goldConfiguration.isPartial(options.rootFirst);

        if (partialTreeIter > i && isPartial)
            return;

        Sentence sentence = goldConfiguration.getSentence();

        Configuration initialConfiguration = new Configuration(goldConfiguration.getSentence(), options.rootFirst);
        Configuration firstOracle = initialConfiguration.clone();
        ArrayList<Configuration> beam = new ArrayList<Configuration>(options.beamWidth);
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
        float maxViol = Float.NEGATIVE_INFINITY;
        Pair<Configuration, Configuration> maxViolPair = null;

        Configuration bestScoringOracle = null;
        boolean oracleInBeam = false;

        while (!ArcEager.isTerminal(beam) && beam.size() > 0) {
            /**
             *  generating new oracles
             *  it keeps the oracles which are in the terminal state
             */
            HashMap<Configuration, Float> newOracles = new HashMap<Configuration, Float>();

            if (options.useDynamicOracle || isPartial) {
                bestScoringOracle = zeroCostDynamicOracle(goldConfiguration, oracles, newOracles);
            } else {
                bestScoringOracle = staticOracle(goldConfiguration, oracles, newOracles);
            }

            if (newOracles.size() == 0) {
                System.err.print("...no oracle(" + dataCount + ")...");
            }
            oracles = newOracles;

            TreeSet<BeamElement> beamPreserver = new TreeSet<BeamElement>();

            if (options.numOfThreads == 1 || beam.size() == 1) {
                beamSortOneThread(beam, beamPreserver, sentence);
            } else {
                for (int b = 0; b < beam.size(); b++) {
                    pool.submit(new BeamScorerThread(false, classifier, beam.get(b),
                            dependencyRelations, featureLength, b, options.rootFirst));
                }
                for (int b = 0; b < beam.size(); b++) {
                    for (BeamElement element : pool.take().get()) {
                        beamPreserver.add(element);
                        if (beamPreserver.size() > options.beamWidth)
                            beamPreserver.pollFirst();
                    }
                }
            }

            if (beamPreserver.size() == 0 || beam.size() == 0) {
                break;
            } else {
                oracleInBeam = false;

                ArrayList<Configuration> repBeam = new ArrayList<Configuration>(options.beamWidth);
                for (BeamElement beamElement : beamPreserver.descendingSet()) {
                    if (repBeam.size() >= options.beamWidth)
                        break;
                    int b = beamElement.number;
                    int action = beamElement.action;
                    int label = beamElement.label;
                    float sc = beamElement.score;

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
                    newConfig.setScore(sc);
                    repBeam.add(newConfig);

                    if (oracles.containsKey(newConfig))
                        oracleInBeam = true;
                }
                beam = repBeam;

                if (beam.size() > 0 && oracles.size() > 0) {
                    Configuration bestConfig = beam.get(0);
                    if (oracles.containsKey(bestConfig)) {
                        oracles = new HashMap<Configuration, Float>();
                        oracles.put(bestConfig, 0.0f);
                    } else {
                        if (options.useRandomOracleSelection) { // choosing randomly, otherwise using latent structured Perceptron
                            List<Configuration> keys = new ArrayList<Configuration>(oracles.keySet());
                            Configuration randomKey = keys.get(randGen.nextInt(keys.size()));
                            oracles = new HashMap<Configuration, Float>();
                            oracles.put(randomKey, 0.0f);
                            bestScoringOracle = randomKey;
                        } else {
                            oracles = new HashMap<Configuration, Float>();
                            oracles.put(bestScoringOracle, 0.0f);
                        }
                    }

                    // do early update
                    if (!oracleInBeam && updateMode.equals("early"))
                        break;

                    // keep violations
                    if (beam.size() > 0 && !oracleInBeam && updateMode.equals("max_violation")) {
                        float violation = beam.get(0).getScore(true) - bestScoringOracle.getScore(true);//Math.abs(beam.get(0).getScore(true) - bestScoringOracle.getScore(true));
                        if (violation > maxViol) {
                            maxViol = violation;
                            maxViolPair = new Pair<Configuration, Configuration>(beam.get(0), bestScoringOracle);
                        }
                    }
                } else
                    break;
            }
        }

        // updating weights
        if (!oracleInBeam || !bestScoringOracle.equals(beam.get(0))) {
            updateWeights(initialConfiguration, maxViol, isPartial, bestScoringOracle, maxViolPair, beam);
        }
    }

    private Configuration staticOracle(GoldConfiguration goldConfiguration, HashMap<Configuration, Float> oracles, HashMap<Configuration, Float> newOracles) throws Exception {
        Configuration bestScoringOracle = null;
        int top = -1;
        int first = -1;
        HashMap<Integer, Pair<Integer, Integer>> goldDependencies = goldConfiguration.getGoldDependencies();
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
                    int dependency = goldDependencies.get(first).second;
                    float[] scores = classifier.rightArcScores(features, false);
                    float score = scores[dependency];
                    ArcEager.rightArc(newConfig.state, dependency);
                    newConfig.addAction(3 + dependency);
                    newConfig.addScore(score);
                } else if (top > 0 && goldDependencies.containsKey(top) && goldDependencies.get(top).first == first) {
                    int dependency = goldDependencies.get(top).second;
                    float[] scores = classifier.leftArcScores(features, false);
                    float score = scores[dependency];
                    ArcEager.leftArc(newConfig.state, dependency);
                    newConfig.addAction(3 + dependencyRelations.size() + dependency);
                    newConfig.addScore(score);
                } else if (top >= 0 && state.hasHead(top)) {

                    if (reversedDependencies.containsKey(top)) {
                        if (reversedDependencies.get(top).size() == state.valence(top)) {
                            float score = classifier.reduceScore(features, false);
                            ArcEager.reduce(newConfig.state);
                            newConfig.addAction(1);
                            newConfig.addScore(score);
                        } else {
                            float score = classifier.shiftScore(features, false);
                            ArcEager.shift(newConfig.state);
                            newConfig.addAction(0);
                            newConfig.addScore(score);
                        }
                    } else {
                        float score = classifier.reduceScore(features, false);
                        ArcEager.reduce(newConfig.state);
                        newConfig.addAction(1);
                        newConfig.addScore(score);
                    }

                } else if (state.bufferEmpty() && state.stackSize() == 1 && state.peek() == state.rootIndex) {
                    float score = classifier.reduceScore(features, false);
                    ArcEager.reduce(newConfig.state);
                    newConfig.addAction(1);
                    newConfig.addScore(score);
                } else {
                    float score = classifier.shiftScore(features, true);
                    ArcEager.shift(newConfig.state);
                    newConfig.addAction(0);
                    newConfig.addScore(score);
                }
                bestScoringOracle = newConfig;
                newOracles.put(newConfig, (float) 0);
            } else {
                newOracles.put(configuration, oracles.get(configuration));
            }
        }
        return bestScoringOracle;
    }

    private Configuration zeroCostDynamicOracle(GoldConfiguration goldConfiguration, HashMap<Configuration, Float> oracles, HashMap<Configuration, Float> newOracles) throws Exception {
        float bestScore = Float.NEGATIVE_INFINITY;
        Configuration bestScoringOracle = null;

        for (Configuration configuration : oracles.keySet()) {
            if (!configuration.state.isTerminalState()) {
                State currentState = configuration.state;
                Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                int accepted = 0;
                // I only assumed that we need zero cost ones
                if (goldConfiguration.actionCost(Actions.Shift, -1, currentState) == 0) {
                    Configuration newConfig = configuration.clone();
                    float score = classifier.shiftScore(features, false);
                    ArcEager.shift(newConfig.state);
                    newConfig.addAction(0);
                    newConfig.addScore(score);
                    newOracles.put(newConfig, (float) 0);

                    if (newConfig.getScore(true) > bestScore) {
                        bestScore = newConfig.getScore(true);
                        bestScoringOracle = newConfig;
                    }
                    accepted++;
                }
                if (ArcEager.canDo(Actions.RightArc, currentState)) {
                    float[] rightArcScores = classifier.rightArcScores(features, false);
                    for (int dependency : dependencyRelations) {
                        if (goldConfiguration.actionCost(Actions.RightArc, dependency, currentState) == 0) {
                            Configuration newConfig = configuration.clone();
                            float score = rightArcScores[dependency];
                            ArcEager.rightArc(newConfig.state, dependency);
                            newConfig.addAction(3 + dependency);
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
                if (ArcEager.canDo(Actions.LeftArc, currentState)) {
                    float[] leftArcScores = classifier.leftArcScores(features, false);

                    for (int dependency : dependencyRelations) {
                        if (goldConfiguration.actionCost(Actions.LeftArc, dependency, currentState) == 0) {
                            Configuration newConfig = configuration.clone();
                            float score = leftArcScores[dependency];
                            ArcEager.leftArc(newConfig.state, dependency);
                            newConfig.addAction(3 + dependencyRelations.size() + dependency);
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
                if (goldConfiguration.actionCost(Actions.Reduce, -1, currentState) == 0) {
                    Configuration newConfig = configuration.clone();
                    float score = classifier.reduceScore(features, false);
                    ArcEager.reduce(newConfig.state);
                    newConfig.addAction(1);
                    newConfig.addScore(score);
                    newOracles.put(newConfig, (float) 0);

                    if (newConfig.getScore(true) > bestScore) {
                        bestScore = newConfig.getScore(true);
                        bestScoringOracle = newConfig;
                    }
                    accepted++;
                }
            } else {
                newOracles.put(configuration, oracles.get(configuration));
            }
        }

        return bestScoringOracle;
    }

    private void beamSortOneThread(ArrayList<Configuration> beam, TreeSet<BeamElement> beamPreserver, Sentence sentence) throws Exception {
        for (int b = 0; b < beam.size(); b++) {
            Configuration configuration = beam.get(b);
            State currentState = configuration.state;
            float prevScore = configuration.score;
            boolean canShift = ArcEager.canDo(Actions.Shift, currentState);
            boolean canReduce = ArcEager.canDo(Actions.Reduce, currentState);
            boolean canRightArc = ArcEager.canDo(Actions.RightArc, currentState);
            boolean canLeftArc = ArcEager.canDo(Actions.LeftArc, currentState);
            Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);

            if (canShift) {
                float score = classifier.shiftScore(features, false);
                float addedScore = score + prevScore;
                beamPreserver.add(new BeamElement(addedScore, b, 0, -1));

                if (beamPreserver.size() > options.beamWidth)
                    beamPreserver.pollFirst();
            }
            if (canReduce) {
                float score = classifier.reduceScore(features, false);
                float addedScore = score + prevScore;
                beamPreserver.add(new BeamElement(addedScore, b, 1, -1));

                if (beamPreserver.size() > options.beamWidth)
                    beamPreserver.pollFirst();
            }

            if (canRightArc) {
                float[] rightArcScores = classifier.rightArcScores(features, false);
                for (int dependency : dependencyRelations) {
                    float score = rightArcScores[dependency];
                    float addedScore = score + prevScore;
                    beamPreserver.add(new BeamElement(addedScore, b, 2, dependency));

                    if (beamPreserver.size() > options.beamWidth)
                        beamPreserver.pollFirst();
                }
            }
            if (canLeftArc) {
                float[] leftArcScores = classifier.leftArcScores(features, false);
                for (int dependency : dependencyRelations) {
                    float score = leftArcScores[dependency];
                    float addedScore = score + prevScore;
                    beamPreserver.add(new BeamElement(addedScore, b, 3, dependency));

                    if (beamPreserver.size() > options.beamWidth)
                        beamPreserver.pollFirst();
                }
            }
        }
    }

    private void updateWeights(Configuration initialConfiguration, float maxViol, boolean isPartial, Configuration bestScoringOracle, Pair<Configuration, Configuration> maxViolPair, ArrayList<Configuration> beam) throws Exception {
        Configuration predicted = null;
        Configuration finalOracle = null;
        if (!updateMode.equals("max_violation")) {
            finalOracle = bestScoringOracle;
            predicted = beam.get(0);
        } else {
            float violation = beam.get(0).getScore(true) - bestScoringOracle.getScore(true); //Math.abs(beam.get(0).getScore(true) - bestScoringOracle.getScore(true));
            if (violation > maxViol) {
                maxViolPair = new Pair<Configuration, Configuration>(beam.get(0), bestScoringOracle);
            }
            predicted = maxViolPair.first;
            finalOracle = maxViolPair.second;
        }

        Object[] predictedFeatures = new Object[featureLength];
        Object[] oracleFeatures = new Object[featureLength];
        for (int f = 0; f < predictedFeatures.length; f++) {
            oracleFeatures[f] = new HashMap<Pair<Integer, Long>, Float>();
            predictedFeatures[f] = new HashMap<Pair<Integer, Long>, Float>();
        }

        Configuration predictedConfiguration = initialConfiguration.clone();
        Configuration oracleConfiguration = initialConfiguration.clone();

        for (int action : finalOracle.actionHistory) {
            boolean isTrueFeature = true;
            if (isPartial && action >= 3) {
                if (!oracleConfiguration.state.hasHead(oracleConfiguration.state.peek()) || !oracleConfiguration.state.hasHead(oracleConfiguration.state.bufferHead()))
                    isTrueFeature = false;
            } else if (isPartial && action == 0) {
                if (!oracleConfiguration.state.hasHead(oracleConfiguration.state.bufferHead()))
                    isTrueFeature = false;
            } else if (isPartial && action == 1) {
                if (!oracleConfiguration.state.hasHead(oracleConfiguration.state.peek()))
                    isTrueFeature = false;
            }

            if (isTrueFeature) {   // if the made dependency is truely for the word
                Object[] feats = FeatureExtractor.extractAllParseFeatures(oracleConfiguration, featureLength);
                for (int f = 0; f < feats.length; f++) {
                    Pair<Integer, Object> featName = new Pair<Integer, Object>(action, feats[f]);
                    HashMap<Pair<Integer, Object>, Float> map = (HashMap<Pair<Integer, Object>, Float>) oracleFeatures[f];
                    Float value = map.get(featName);
                    if (value == null)
                        map.put(featName, 1.0f);
                    else
                        map.put(featName, value + 1);
                }
            }

            if (action == 0) {
                ArcEager.shift(oracleConfiguration.state);
            } else if (action == 1) {
                ArcEager.reduce(oracleConfiguration.state);
            } else if (action >= (3 + dependencyRelations.size())) {
                int dependency = action - (3 + dependencyRelations.size());
                ArcEager.leftArc(oracleConfiguration.state, dependency);
            } else if (action >= 3) {
                int dependency = action - 3;
                ArcEager.rightArc(oracleConfiguration.state, dependency);
            }
        }

        for (int action : predicted.actionHistory) {
            boolean isTrueFeature = true;
            if (isPartial && action >= 3) {
                if (!predictedConfiguration.state.hasHead(predictedConfiguration.state.peek()) || !predictedConfiguration.state.hasHead(predictedConfiguration.state.bufferHead()))
                    isTrueFeature = false;
            } else if (isPartial && action == 0) {
                if (!predictedConfiguration.state.hasHead(predictedConfiguration.state.bufferHead()))
                    isTrueFeature = false;
            } else if (isPartial && action == 1) {
                if (!predictedConfiguration.state.hasHead(predictedConfiguration.state.peek()))
                    isTrueFeature = false;
            }

            if (isTrueFeature) {   // if the made dependency is truely for the word
                Object[] feats = FeatureExtractor.extractAllParseFeatures(predictedConfiguration, featureLength);
                if (action != 2) // do not take into account for unshift
                    for (int f = 0; f < feats.length; f++) {
                        Pair<Integer, Object> featName = new Pair<Integer, Object>(action, feats[f]);
                        HashMap<Pair<Integer, Object>, Float> map = (HashMap<Pair<Integer, Object>, Float>) predictedFeatures[f];
                        Float value = map.get(featName);
                        if (value == null)
                            map.put(featName, 1.f);
                        else
                            map.put(featName, map.get(featName) + 1);
                    }
            }

            State state = predictedConfiguration.state;
            if (action == 0) {
                ArcEager.shift(state);
            } else if (action == 1) {
                ArcEager.reduce(state);
            } else if (action >= 3 + dependencyRelations.size()) {
                int dependency = action - (3 + dependencyRelations.size());
                ArcEager.leftArc(state, dependency);
            } else if (action >= 3) {
                int dependency = action - 3;
                ArcEager.rightArc(state, dependency);
            } else if (action == 2) {
                ArcEager.unShift(state);
            }
        }

        for (int f = 0; f < predictedFeatures.length; f++) {
            HashMap<Pair<Integer, Object>, Float> map = (HashMap<Pair<Integer, Object>, Float>) predictedFeatures[f];
            HashMap<Pair<Integer, Object>, Float> map2 = (HashMap<Pair<Integer, Object>, Float>) oracleFeatures[f];
            for (Pair<Integer, Object> feat : map.keySet()) {
                int action = feat.first;
                Actions actionType = Actions.Shift;
                int dependency = 0;
                if (action == 0) {
                    actionType = Actions.Shift;
                } else if (action == 1) {
                    actionType = Actions.Reduce;
                } else if (action >= 3 + dependencyRelations.size()) {
                    dependency = action - (3 + dependencyRelations.size());
                    actionType = Actions.LeftArc;
                } else if (action >= 3) {
                    dependency = action - 3;
                    actionType = Actions.RightArc;
                } else if (action == 2) {
                    actionType = Actions.Unshift;
                }
                if (feat.second != null) {
                    Object feature = feat.second;
                    if (!(map2.containsKey(feat) && map2.get(feat).equals(map.get(feat))))
                        classifier.changeWeight(actionType, f, feature, dependency, -map.get(feat));
                }
            }

            for (Pair<Integer, Object> feat : map2.keySet()) {
                int action = feat.first;
                Actions actionType = Actions.Shift;
                int dependency = 0;
                if (action == 0) {
                    actionType = Actions.Shift;
                } else if (action == 1) {
                    actionType = Actions.Reduce;
                } else if (action >= 3 + dependencyRelations.size()) {
                    dependency = action - (3 + dependencyRelations.size());
                    actionType = Actions.LeftArc;
                } else if (action >= 3) {
                    dependency = action - 3;
                    actionType = Actions.RightArc;
                } else if (action == 2) {
                    actionType = Actions.Unshift;
                }
                if (feat.second != null) {
                    Object feature = feat.second;
                    if (!(map.containsKey(feat) && map.get(feat).equals(map2.get(feat))))
                        classifier.changeWeight(actionType, f, feature, dependency, map2.get(feat));
                }
            }
        }
    }

}