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
import TransitionBasedSystem.Configuration.State;
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

        } else if (options.train) {
            if (options.inputFile.equals("") || options.modelFile.equals("")) {
                Options.showHelp();
            } else {
                System.out.println(options);
                IndexMaps maps = CoNLLReader.createIndices(options.inputFile, options.labeled, options.lowercase);
                State.labelMap = maps.getLabels();
                CoNLLReader reader = new CoNLLReader(options.inputFile);


                ArrayList<GoldConfiguration> dataSet = reader.readData(Integer.MAX_VALUE, false, options.labeled, options.rootFirst, options.lowercase, maps);
                System.out.println("CoNLL data reading done!");

                ArrayList<String> dependencyLabels = new ArrayList<String>();
                HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet = new HashMap<Integer, HashMap<Integer, HashSet<String>>>();

                for (GoldConfiguration configuration : dataSet) {
                    Sentence sentence = configuration.getSentence();

                    for (int dep : configuration.getGoldDependencies().keySet()) {
                        Pair<Integer, String> headDepPair = configuration.getGoldDependencies().get(dep);
                        if (!dependencyLabels.contains(headDepPair.second))
                            dependencyLabels.add(headDepPair.second);
                        String relation = headDepPair.second;
                        int dependent = sentence.posAt(dep);
                        int head = sentence.posAt(headDepPair.first);

                        if (!headDepSet.containsKey(head))
                            headDepSet.put(head, new HashMap<Integer, HashSet<String>>());
                        if (!headDepSet.get(head).containsKey(dependent))
                            headDepSet.get(head).put(dependent, new HashSet<String>());
                        headDepSet.get(head).get(dependent).add(relation);
                    }
                }

                int featureLength = options.useExtendedFeatures ? 72 : 26;

                System.out.println("size of training data (#sens): " + dataSet.size());

                HashMap<String, Integer> labels = new HashMap<String, Integer>();
                int labIndex = 0;
                labels.put("sh", labIndex++);
                labels.put("rd", labIndex++);
                for (String label : dependencyLabels) {
                    labels.put("ra_" + label, labIndex++);
                    labels.put("la_" + label, labIndex++);
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

                ArcEagerBeamTrainer trainer = new ArcEagerBeamTrainer(options.useMaxViol ? "max_violation" : "early", new AveragedPerceptron(featureLength, labels, 1),
                        options.rootFirst, options.beamWidth, dependencyLabels, headDepSet, featureLength, options.useDynamicOracle, options.useRandomOracleSelection, maps);
                trainer.train(dataSet, options.devPath, options.trainingIter, options.modelFile, options.lowercase);
            }
        } else if (options.parseTaggedFile) {
            if (options.outputFile.equals("") || options.inputFile.equals("")
                    || options.infFile.equals("") || options.modelFile.equals("")) {
                Options.showHelp();

            } else {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(options.infFile));
                ArrayList<String> dependencyLabels = (ArrayList<String>) reader.readObject();
                IndexMaps maps = (IndexMaps) reader.readObject();
                State.labelMap = maps.getLabels();

                HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet = (HashMap<Integer, HashMap<Integer, HashSet<String>>>) reader.readObject();

                Options inf_options = (Options) reader.readObject();
                AveragedPerceptron averagedPerceptron = AveragedPerceptron.loadModel(options.modelFile, 1);

                int templates = averagedPerceptron.featureSize();
                KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, templates, maps);

                parser.parseTaggedFile(options.inputFile,
                        options.outputFile, inf_options.rootFirst, inf_options.beamWidth, inf_options.lowercase,options.separator);
            }
        } else if (options.parseConllFile) {
            if (options.outputFile.equals("") || options.inputFile.equals("")
                    || options.infFile.equals("") || options.modelFile.equals("")) {
                Options.showHelp();

            } else {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(options.infFile));
                ArrayList<String> dependencyLabels = (ArrayList<String>) reader.readObject();
                IndexMaps maps = (IndexMaps) reader.readObject();
                State.labelMap = maps.getLabels();

                HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet = (HashMap<Integer, HashMap<Integer, HashSet<String>>>) reader.readObject();

                Options inf_options = (Options) reader.readObject();
                AveragedPerceptron averagedPerceptron = AveragedPerceptron.loadModel(options.modelFile, 1);

                int templates = averagedPerceptron.featureSize();
                KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, templates, maps);

                parser.parseConllFile(options.inputFile,
                        options.outputFile, inf_options.rootFirst, inf_options.beamWidth, inf_options.lowercase);
            }
        } else if (options.evaluate){
            if (options.goldFile.equals("") || options.predFile.equals(""))
                Options.showHelp();
            else
            Evaluator.evaluate(options.goldFile,options.predFile);
        } else {
            Options.showHelp();
        }
    }
}
