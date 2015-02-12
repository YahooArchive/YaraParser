/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package YaraParser.Parser;

import YaraParser.Learning.AveragedPerceptron;
import YaraParser.Structures.IndexMaps;
import YaraParser.Structures.InfStruct;
import YaraParser.TransitionBasedSystem.Configuration.Configuration;
import YaraParser.TransitionBasedSystem.Parser.KBeamArcEagerParser;

import java.util.ArrayList;

public class API_UsageExample {
    public static void main(String[] args) throws Exception {
        String modelFile = args[0];
        int numOfThreads = 8;

        InfStruct infStruct = new InfStruct(modelFile);

        ArrayList<Integer> dependencyLabels = infStruct.dependencyLabels;
        IndexMaps maps = infStruct.maps;
        AveragedPerceptron averagedPerceptron = new AveragedPerceptron(infStruct);

        int featureSize = averagedPerceptron.featureSize();
        KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, featureSize, maps, numOfThreads);

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
                if (head == words.length+1)
                    head = 0;
                System.out.println(words[i] + "\t" + tags[i] + "\t" + head + "\t" + maps.revWords[bestParse.state.getDependency(i + 1)]);
            }
        }
        parser.shutDownLiveThreads();
        System.exit(0);
    }
}
