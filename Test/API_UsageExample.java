/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package Test;

import Accessories.Options;
import Learning.AveragedPerceptron;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.Configuration;
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

        // loading inf file
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(infFile));
        ArrayList<String> dependencyLabels=(ArrayList<String>)reader.readObject();
        HashMap<String, HashMap<String, HashSet<String>>> headDepSet=(HashMap<String, HashMap<String, HashSet<String>>>)reader.readObject();
        Options options=(Options)reader.readObject();

        // loading model to an averaged Perceptron classifier
        AveragedPerceptron averagedPerceptron = (AveragedPerceptron) AveragedPerceptron.loadModel(modelFile, 1);

        // loading the parser
        KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, averagedPerceptron.featureSize());


        if(options.rootFirst){
            String[] words={"I","am","here","."};
            String[] tags={"PRON","VERB","ADP","."};
        Configuration bestParse= parser.parse(new Sentence(words, tags), options.rootFirst, options.beamWidth);

            for(int i=0;i<words.length;i++){
              System.out.println(words[i]+"\t"+tags[i]+"\t"+bestParse.state.getHead(i+1)+"\t"+bestParse.state.getDependency(i+1));
            }
        }else{
            String[] words={"I","am","here",".","ROOT"};
            String[] tags={"PRON","VERB","ADP",".","ROOT"};
            Configuration bestParse= parser.parse(new Sentence(words, tags), options.rootFirst, options.beamWidth);
            for(int i=0;i<words.length-1;i++){
                int head=bestParse.state.getHead(i+1);
                if(head==words.length)
                    head=0;
                System.out.println(words[i]+"\t"+tags[i]+"\t"+head+"\t"+bestParse.state.getDependency(i+1));
            }
        }

    }
}
