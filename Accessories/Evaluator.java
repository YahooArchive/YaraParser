/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Accessories;

import Structures.IndexMaps;
import TransitionBasedSystem.Configuration.GoldConfiguration;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class Evaluator {
    public static void evaluate(String testPath, String predictedPath) throws  Exception{
        CoNLLReader goldReader=new CoNLLReader(testPath);
        CoNLLReader predictedReader=new CoNLLReader(predictedPath);

        ArrayList<GoldConfiguration> goldConfiguration=goldReader.readData(Integer.MAX_VALUE, true, true, false, false, new IndexMaps(new HashMap<String, Integer>(),new HashMap<String, Integer>()));
        ArrayList<GoldConfiguration> predConfiguration=predictedReader.readData(Integer.MAX_VALUE,true,true,false,false,new IndexMaps(new HashMap<String, Integer>(),new HashMap<String, Integer>()));


        float unlabMatch=0f;
        float labMatch=0f;
        int all=0;

        float fullULabMatch=0f;
        float fullLabMatch=0f;
        int numTree=0;

        for(int i=0;i<goldConfiguration.size();i++){
            HashMap<Integer, Pair<Integer, String>> goldDeps= goldConfiguration.get(i).getGoldDependencies();
            HashMap<Integer, Pair<Integer, String>> predDeps= predConfiguration.get(i).getGoldDependencies();

            numTree++;
            boolean fullMatch=true;
            boolean fullUnlabMatch=true;
            for(int dep:goldDeps.keySet()){
                all++;
                if(predDeps.containsKey(dep)){
                    int gh= goldDeps.get(dep).first;
                    int ph=   predDeps.get(dep).first;
                    String gl=    goldDeps.get(dep).second;
                    String pl=    predDeps.get(dep).second;

                    if(ph==gh){
                        unlabMatch++;

                        if(pl.equals(gl))
                            labMatch++;
                        else
                            fullMatch=false;
                    }else{
                        fullMatch=false;
                        fullUnlabMatch=false;
                    }
                }
            }

            if(fullMatch)
                fullLabMatch++;
            if(fullUnlabMatch)
                fullULabMatch++;
        }

        DecimalFormat format=new DecimalFormat("##.00");
        double labeledAccuracy = 100.0 * labMatch / all;
        double unlabaledAccuracy = 100.0 * unlabMatch / all;
        System.err.println("Labeled accuracy: " + format.format(labeledAccuracy));
        System.err.println("Unlabeled accuracy:  " + format.format(unlabaledAccuracy));
        double labExact = 100.0 * fullLabMatch / numTree;
        double ulabExact = 100.0 * fullULabMatch / numTree;
        System.err.println("Labeled exact match:  " + format.format(labExact));
        System.err.println("Unlabeled exact match:  " + format.format(ulabExact)+" \n");
    }
}
