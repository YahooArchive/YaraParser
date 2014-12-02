/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package Parser;

import Accessories.Options;
import Learning.AveragedPerceptron;
import Structures.IndexMaps;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.State;
import TransitionBasedSystem.Parser.KBeamArcEagerParser;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class API_UsageExample {
    public static void main(String[] args) throws  Exception{
        String infFile=args[0];
        String modelFile=args[1];


        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(infFile));
        ArrayList<String> dependencyLabels = (ArrayList<String>) reader.readObject();
        IndexMaps maps = (IndexMaps) reader.readObject();
        State.labelMap = maps.getLabels();

        HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet = (HashMap<Integer, HashMap<Integer, HashSet<String>>>) reader.readObject();

        Options inf_options = (Options) reader.readObject();
        AveragedPerceptron averagedPerceptron = AveragedPerceptron.loadModel(modelFile, 1);

        int templates = averagedPerceptron.featureSize();
        KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, templates, maps);

        String[] words={"I","am","here","."};
        String[] tags={"PRP","VBP","RB","."};

        Configuration bestParse= parser.parse(maps.makeSentence(words,tags,inf_options.rootFirst,inf_options.lowercase), inf_options.rootFirst, inf_options.beamWidth);
        if(inf_options.rootFirst){
            for(int i=0;i<words.length;i++){
                System.out.println(words[i]+"\t"+tags[i]+"\t"+bestParse.state.getHead(i+1)+"\t"+bestParse.state.getDependency(i+1));
            }
        }else{
            for(int i=0;i<words.length;i++){
                int head=bestParse.state.getHead(i+1);
                if(head==words.length+1)
                    head=0;
                System.out.println(words[i]+"\t"+tags[i]+"\t"+head+"\t"+bestParse.state.getDependency(i+1));
            }
        }

    }
}
