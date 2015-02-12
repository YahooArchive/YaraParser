/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package YaraParser.Parser;

import YaraParser.Accessories.CoNLLReader;
import YaraParser.Accessories.Evaluator;
import YaraParser.Accessories.Options;
import YaraParser.Learning.AveragedPerceptron;
import YaraParser.Structures.IndexMaps;
import YaraParser.Structures.InfStruct;
import YaraParser.TransitionBasedSystem.Configuration.GoldConfiguration;
import YaraParser.TransitionBasedSystem.Parser.KBeamArcEagerParser;
import YaraParser.TransitionBasedSystem.Trainer.ArcEagerBeamTrainer;

import java.util.ArrayList;
import java.util.HashMap;

public class YaraParser {
    public static void main(String[] args) throws Exception {
        Options options = Options.processArgs(args);

        if (options.showHelp) {
            Options.showHelp();
        } else {
            System.out.println(options);
            if (options.train) {
                train(options);
            } else if (options.parseTaggedFile || options.parseConllFile || options.parsePartialConll) {
                parse(options);
            } else if (options.evaluate) {
                evaluate(options);
            } else {
                Options.showHelp();
            }
        }
        System.exit(0);
    }

    private static void evaluate(Options options) throws Exception {
        if (options.goldFile.equals("") || options.predFile.equals(""))
            Options.showHelp();
        else {
            Evaluator.evaluate(options.goldFile, options.predFile, options.punctuations);
        }
    }

    private static void parse(Options options) throws Exception {
        if (options.outputFile.equals("") || options.inputFile.equals("")
                || options.modelFile.equals("")) {
            Options.showHelp();

        } else {
            InfStruct infStruct = new InfStruct(options.modelFile);
            ArrayList<Integer> dependencyLabels = infStruct.dependencyLabels;
            IndexMaps maps = infStruct.maps;


            Options inf_options = infStruct.options;
            AveragedPerceptron averagedPerceptron = new AveragedPerceptron(infStruct);

            int featureSize = averagedPerceptron.featureSize();
            KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, featureSize, maps, options.numOfThreads);

            if (options.parseTaggedFile)
                parser.parseTaggedFile(options.inputFile,
                        options.outputFile, inf_options.rootFirst, inf_options.beamWidth, inf_options.lowercase, options.separator, options.numOfThreads);
            else if (options.parseConllFile)
                parser.parseConllFile(options.inputFile,
                        options.outputFile, inf_options.rootFirst, inf_options.beamWidth, true, inf_options.lowercase, options.numOfThreads, false, options.scorePath);
            else if (options.parsePartialConll)
                parser.parseConllFile(options.inputFile,
                        options.outputFile, inf_options.rootFirst, inf_options.beamWidth, options.labeled, inf_options.lowercase, options.numOfThreads, true, options.scorePath);
            parser.shutDownLiveThreads();
        }
    }

    public static void train(Options options) throws Exception {
        if (options.inputFile.equals("") || options.modelFile.equals("")) {
            Options.showHelp();
        } else {
            IndexMaps maps = CoNLLReader.createIndices(options.inputFile, options.labeled, options.lowercase, options.clusterFile);
            CoNLLReader reader = new CoNLLReader(options.inputFile);
            ArrayList<GoldConfiguration> dataSet = reader.readData(Integer.MAX_VALUE, false, options.labeled, options.rootFirst, options.lowercase, maps);
            System.out.println("CoNLL data reading done!");

            ArrayList<Integer> dependencyLabels = new ArrayList<Integer>();
            for (int lab : maps.getLabels().keySet())
                dependencyLabels.add(lab);

            int featureLength = options.useExtendedFeatures ? 72 : 26;
            if (options.useExtendedWithBrownClusterFeatures || maps.hasClusters())
                featureLength = 153;

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

            ArcEagerBeamTrainer trainer = new ArcEagerBeamTrainer(options.useMaxViol ? "max_violation" : "early", new AveragedPerceptron(featureLength, dependencyLabels.size()),
                    options, dependencyLabels, featureLength, maps);
            trainer.train(dataSet, options.devPath, options.trainingIter, options.modelFile, options.lowercase, options.punctuations, options.partialTrainingStartingIteration);
        }
    }
}
