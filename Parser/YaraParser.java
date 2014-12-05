/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Parser;

import Accessories.CoNLLReader;
import Accessories.Evaluator;
import Accessories.Options;
import Accessories.Pair;
import Learning.AveragedPerceptron;
import Structures.IndexMaps;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.GoldConfiguration;
import TransitionBasedSystem.Parser.KBeamArcEagerParser;
import TransitionBasedSystem.Trainer.ArcEagerBeamTrainer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class YaraParser {

    public static void main(String[] args) throws Exception {
        Options options = Options.processArgs(args);

        if (options.showHelp) {
            Options.showHelp();

        } else {
            System.out.println(options);
            if (options.train) {
                if (options.inputFile.equals("") || options.modelFile.equals("")) {
                    Options.showHelp();
                } else {
                    IndexMaps maps = CoNLLReader.createIndices(options.inputFile, options.labeled, options.lowercase);
                    CoNLLReader reader = new CoNLLReader(options.inputFile);
                    ArrayList<GoldConfiguration> dataSet = reader.readData(Integer.MAX_VALUE, false, options.labeled, options.rootFirst, options.lowercase, maps);
                    System.out.println("CoNLL data reading done!");

                    ArrayList<Integer> dependencyLabels = new ArrayList<Integer>();
                    for (int lab : maps.getLabels().keySet())
                        dependencyLabels.add(lab);

                    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();

                    for (GoldConfiguration configuration : dataSet) {
                        Sentence sentence = configuration.getSentence();

                        for (int dep : configuration.getGoldDependencies().keySet()) {
                            Pair<Integer, Integer> headDepPair = configuration.getGoldDependencies().get(dep);
                            int relation = headDepPair.second;
                            int dependent = sentence.posAt(dep);
                            int head = sentence.posAt(headDepPair.first);

                            if (!headDepSet.containsKey(head))
                                headDepSet.put(head, new HashMap<Integer, HashSet<Integer>>());
                            if (!headDepSet.get(head).containsKey(dependent))
                                headDepSet.get(head).put(dependent, new HashSet<Integer>());
                            headDepSet.get(head).get(dependent).add(relation);
                        }
                    }

                    int featureLength = options.useExtendedFeatures ? 72 : 26;

                    System.out.println("size of training data (#sens): " + dataSet.size());

                    HashMap<String, Integer> labels = new HashMap<String, Integer>();
                    int labIndex = 0;
                    labels.put("sh", labIndex++);
                    labels.put("rd", labIndex++);
                    labels.put("us", labIndex++);
                    for (int label : dependencyLabels) {
                        if (options.labeled) {
                            labels.put("ra_" + label, 3 + label);
                            labels.put("la_" + label, 3 + dependencyLabels.size() + label);
                        } else {
                            labels.put("ra_" + label, 3);
                            labels.put("la_" + label, 4);
                        }
                    }

                    System.out.print("writing objects....");

                    ObjectOutput writer = new ObjectOutputStream(new FileOutputStream(options.modelFile));
                    writer.writeObject(dependencyLabels);
                    writer.writeObject(maps);
                    writer.writeObject(headDepSet);
                    writer.writeObject(options);
                    writer.flush();
                    writer.close();
                    System.out.println("done!");

                    ArcEagerBeamTrainer trainer = new ArcEagerBeamTrainer(options.useMaxViol ? "max_violation" : "early", new AveragedPerceptron(featureLength, 4 + 2 * dependencyLabels.size(), options.numOfThreads),
                            options.rootFirst, options.beamWidth, dependencyLabels, headDepSet, featureLength, options.useDynamicOracle, options.useRandomOracleSelection, maps, options.numOfThreads);
                    trainer.train(dataSet, options.devPath, options.trainingIter, options.modelFile, options.lowercase);
                }
            } else if (options.parseTaggedFile) {
                if (options.outputFile.equals("") || options.inputFile.equals("")
                        || options.infFile.equals("") || options.modelFile.equals("")) {
                    Options.showHelp();

                } else {
                    ObjectInputStream reader = new ObjectInputStream(new FileInputStream(options.infFile));
                    ArrayList<Integer> dependencyLabels = (ArrayList<Integer>) reader.readObject();
                    IndexMaps maps = (IndexMaps) reader.readObject();

                    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet = (HashMap<Integer, HashMap<Integer, HashSet<Integer>>>) reader.readObject();

                    Options inf_options = (Options) reader.readObject();
                    AveragedPerceptron averagedPerceptron = AveragedPerceptron.loadModel(options.modelFile, options.numOfThreads);

                    int templates = averagedPerceptron.featureSize();
                    KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, templates, maps, options.numOfThreads);

                    parser.parseTaggedFile(options.inputFile,
                            options.outputFile, inf_options.rootFirst, inf_options.beamWidth, inf_options.lowercase, options.separator, options.numOfThreads);
                    parser.shutDownLiveThreads();
                }
            } else if (options.parseConllFile) {
                if (options.outputFile.equals("") || options.inputFile.equals("")
                        || options.infFile.equals("") || options.modelFile.equals("")) {
                    Options.showHelp();
                } else {
                    ObjectInputStream reader = new ObjectInputStream(new FileInputStream(options.infFile));
                    ArrayList<Integer> dependencyLabels = (ArrayList<Integer>) reader.readObject();
                    IndexMaps maps = (IndexMaps) reader.readObject();

                    HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet = (HashMap<Integer, HashMap<Integer, HashSet<Integer>>>) reader.readObject();

                    Options inf_options = (Options) reader.readObject();
                    AveragedPerceptron averagedPerceptron = AveragedPerceptron.loadModel(options.modelFile, options.numOfThreads);

                    int templates = averagedPerceptron.featureSize();
                    KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, templates, maps, options.numOfThreads);

                    parser.parseConllFile(options.inputFile,
                            options.outputFile, inf_options.rootFirst, inf_options.beamWidth, inf_options.lowercase, options.numOfThreads);
                    parser.shutDownLiveThreads();
                }
            } else if (options.evaluate) {
                if (options.goldFile.equals("") || options.predFile.equals("") || options.infFile.equals(""))
                    Options.showHelp();
                else {
                    ObjectInputStream reader = new ObjectInputStream(new FileInputStream(options.infFile));
                    ArrayList<Integer> dependencyLabels = (ArrayList<Integer>) reader.readObject();
                    IndexMaps maps = (IndexMaps) reader.readObject();
                    reader.close();
                    Evaluator.evaluate(options.goldFile, options.predFile, maps);
                }
            } else {
                Options.showHelp();
            }
        }
        System.exit(0);
    }
}
