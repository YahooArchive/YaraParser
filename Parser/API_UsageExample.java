/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package Parser;

import Learning.AveragedPerceptron;
import Structures.IndexMaps;
import Structures.InfStruct;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Parser.KBeamArcEagerParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class API_UsageExample {
    public static void main(String[] args) throws Exception {
        String modelFile = args[0];
        int numOfThreads = 8;

        InfStruct infStruct = new InfStruct(modelFile);

        ArrayList<Integer> dependencyLabels = infStruct.dependencyLabels;
        IndexMaps maps = infStruct.maps;
        HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet = infStruct.headDepSet;
        AveragedPerceptron averagedPerceptron = new AveragedPerceptron(infStruct.avg.length, infStruct.avg, infStruct.avg[0].length);

        int templates = averagedPerceptron.featureSize();
        KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, templates, maps, numOfThreads);

        String[] words = {"I", "am", "here", "."};
        String[] tags = {"PRP", "VBP", "RB", "."};

        Configuration bestParse = parser.parse(maps.makeSentence(words, tags, infStruct.options.rootFirst, infStruct.options.lowercase), infStruct.options.rootFirst, infStruct.options.beamWidth, numOfThreads);
        if (infStruct.options.rootFirst) {
            for (int i = 0; i < words.length; i++) {
                System.out.println(words[i] + "\t" + tags[i] + "\t" + bestParse.state.getHead(i + 1) + "\t" + maps.revWords[bestParse.state.getDependency(i + 1)]);
            }
        } else {
            for (int i = 0; i < words.length; i++) {
                int head = bestParse.state.getHead(i + 1);
                if (head == words.length + 1)
                    head = 0;
                System.out.println(words[i] + "\t" + tags[i] + "\t" + head + "\t" + maps.revWords[bestParse.state.getDependency(i + 1)]);
            }
        }
        parser.shutDownLiveThreads();
        System.exit(0);
    }
}
