/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package UnitTest;

import YaraParser.Accessories.CoNLLReader;
import YaraParser.Accessories.Options;
import YaraParser.Accessories.Pair;
import YaraParser.Learning.AveragedPerceptron;
import YaraParser.Structures.IndexMaps;
import YaraParser.Structures.Sentence;
import YaraParser.TransitionBasedSystem.Configuration.GoldConfiguration;
import YaraParser.TransitionBasedSystem.Trainer.ArcEagerBeamTrainer;

import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class UnitTest {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.inputFile = args[0];
        options.devPath = args[1];
        options.modelFile = args[2];
        options.changePunc(args[3]);
        options.trainingIter = 3;
        options.train = true;
        options.beamWidth = 4;
        options.rootFirst = true;
        options.useDynamicOracle = false;
        options.labeled = true;
        options.useMaxViol = false;
        options.numOfThreads = 2;

        ArrayList<Options> optionList = Options.getAllPossibleOptions(options);
        options.numOfThreads = 2;
        for (Options o : optionList)
            testOption(o);

        System.exit(0);
    }

    public static void testOption(Options options) throws Exception {
        System.out.println("**********************************************");
        System.out.print(options);
        System.out.println("**********************************************");
        IndexMaps maps = CoNLLReader.createIndices(options.inputFile, options.labeled, options.lowercase, options.clusterFile);
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

        System.out.print("writing objects....");

        ObjectOutput writer = new ObjectOutputStream(new FileOutputStream(options.modelFile));
        writer.writeObject(dependencyLabels);
        writer.writeObject(maps);
        writer.writeObject(headDepSet);
        writer.writeObject(options);
        writer.flush();
        writer.close();
        System.out.println("done!");

        ArcEagerBeamTrainer trainer = new ArcEagerBeamTrainer(options.useMaxViol ? "max_violation" : "early", new AveragedPerceptron(featureLength, dependencyLabels.size()),
                options, dependencyLabels, featureLength, maps);
        trainer.train(dataSet, options.devPath, options.trainingIter, options.modelFile, options.lowercase, options.punctuations, options.partialTrainingStartingIteration);
        trainer = null;
    }
}
