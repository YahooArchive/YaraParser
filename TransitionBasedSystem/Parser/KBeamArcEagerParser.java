/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package TransitionBasedSystem.Parser;

import Accessories.CoNLLReader;
import Accessories.Pair;
import Learning.AveragedPerceptron;
import Structures.IndexMaps;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.BeamElement;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.GoldConfiguration;
import TransitionBasedSystem.Configuration.State;
import TransitionBasedSystem.Features.FeatureExtractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.*;

public class KBeamArcEagerParser extends TransitionBasedParser {
    /**
     * Any kind of classifier that can give us scores
     */
    AveragedPerceptron classifier;

    ArrayList<String> dependencyRelations;

    int featureLength;

    // for pruning irrelevant search space
    HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet;

    IndexMaps maps;

    public KBeamArcEagerParser(AveragedPerceptron classifier, ArrayList<String> dependencyRelations,
                               HashMap<Integer, HashMap<Integer, HashSet<String>>> headDepSet, int featureLength, IndexMaps maps) {
        this.classifier = classifier;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        this.headDepSet = headDepSet;
        this.maps = maps;
    }

    public Configuration parse(Sentence sentence, boolean rootFirst, int beamWidth) throws Exception {
        Configuration initialConfiguration = new Configuration(sentence, rootFirst);

        ArrayList<Configuration> beam = new ArrayList<Configuration>(beamWidth);
        beam.add(initialConfiguration);

        while (!ArcEager.isTerminal(beam)) {
            if (beamWidth != 1) {
                TreeSet<BeamElement> beamPreserver = new TreeSet<BeamElement>();
                for (int b = 0; b < beam.size(); b++) {
                    Configuration configuration = beam.get(b);
                    State currentState = configuration.state;
                    float prevScore = configuration.score;
                    boolean canShift = ArcEager.canDo(0, currentState);
                    boolean canReduce = ArcEager.canDo(1, currentState);
                    boolean canRightArc = ArcEager.canDo(2, currentState);
                    boolean canLeftArc = ArcEager.canDo(3, currentState);
                    String[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                    if (!canShift
                            && !canReduce
                            && !canRightArc
                            && !canLeftArc) {
                        float addedScore = prevScore;
                        beamPreserver.add(new BeamElement(addedScore,b,4,""));

                        if (beamPreserver.size() > beamWidth)
                            beamPreserver.pollFirst();
                    }

                    if (canShift) {
                        float score = classifier.score(features, "sh", true);
                        float addedScore = score + prevScore;
                        beamPreserver.add(new BeamElement(addedScore,b,0,""));

                        if (beamPreserver.size() > beamWidth)
                            beamPreserver.pollFirst();
                    }

                    if (canReduce) {
                        float score = classifier.score(features, "rd", true);
                        float addedScore = score + prevScore;
                        beamPreserver.add(new BeamElement(addedScore,b,1,""));

                        if (beamPreserver.size() > beamWidth)
                            beamPreserver.pollFirst();
                    }

                    if (canRightArc) {
                        int headPos = sentence.posAt(configuration.state.peek());
                        int depPos = sentence.posAt(configuration.state.bufferHead());
                        for (String dependency : dependencyRelations) {
                            if ((!canLeftArc && !canShift && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                    && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                float score = classifier.score(features, "ra_" + dependency, true);
                                float addedScore = score + prevScore;
                                beamPreserver.add(new BeamElement(addedScore,b,2,dependency));

                                if (beamPreserver.size() > beamWidth)
                                    beamPreserver.pollFirst();
                            }
                        }
                    }

                    if (canLeftArc) {
                        int headPos = sentence.posAt(configuration.state.bufferHead());
                        int depPos = sentence.posAt(configuration.state.peek());
                        for (String dependency : dependencyRelations) {
                            if ((!canShift && !canRightArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                    && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                float score = classifier.score(features, "la_" + dependency, true);
                                float addedScore = score + prevScore;
                                beamPreserver.add(new BeamElement(addedScore,b,3,dependency));

                                if (beamPreserver.size() > beamWidth)
                                    beamPreserver.pollFirst();
                            }
                        }
                    }
                }

                ArrayList<Configuration> repBeam = new ArrayList<Configuration>(beamWidth);
                for (BeamElement beamElement : beamPreserver.descendingSet()) {
                    if (repBeam.size() >= beamWidth)
                        break;
                        int b = beamElement.number;
                        int action =beamElement.action;
                        String label=beamElement.label;
                        float score=beamElement.score;

                        Configuration newConfig = beam.get(b).clone();

                        if (action==0) {
                            ArcEager.shift(newConfig.state);
                            newConfig.addAction("sh");
                        } else if (action==1) {
                            ArcEager.reduce(newConfig.state);
                            newConfig.addAction("rd");
                        } else if (action==2) {
                            ArcEager.rightArc(newConfig.state, label);
                            newConfig.addAction("ra_"+label);
                        } else if (action==3) {
                            ArcEager.leftArc(newConfig.state, label);
                            newConfig.addAction("la_"+label);
                        } else if (action==4) {
                            ArcEager.unShift(newConfig.state);
                            newConfig.addAction("us");
                        }
                        newConfig.setScore(score);
                        repBeam.add(newConfig);
                }
                beam = repBeam;
            } else {
                Configuration configuration = beam.get(0);
                State currentState = configuration.state;
                String[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                float bestScore = Float.NEGATIVE_INFINITY;
                String bestAction = null;

                boolean canShift = ArcEager.canDo(0, currentState);
                boolean canReduce = ArcEager.canDo(1, currentState);
                boolean canRightArc = ArcEager.canDo(2, currentState);
                boolean canLeftArc = ArcEager.canDo(3, currentState);

                if (!canShift
                        && !canReduce
                        && !canRightArc
                        && !canLeftArc) {

                    if (!currentState.stackEmpty()) {
                        ArcEager.unShift(currentState);
                        configuration.addAction("us");
                    } else if (!currentState.bufferEmpty() && currentState.stackEmpty()) {
                        ArcEager.shift(currentState);
                        configuration.addAction("sh");
                    }
                }

                if (canShift) {
                    float score = classifier.score(features, "sh", true);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAction = "sh";
                    }
                }
                if (canReduce) {
                    float score = classifier.score(features, "rd", true);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAction = "rd";
                    }
                }
                if (canRightArc) {
                    int headPos = sentence.posAt(configuration.state.peek());
                    int depPos = sentence.posAt(configuration.state.bufferHead());
                    for (String dependency : dependencyRelations) {
                        if ((!canShift && !canLeftArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                            float score = classifier.score(features, "ra_" + dependency, true);
                            if (score > bestScore) {
                                bestScore = score;
                                bestAction = "ra_" + dependency;
                            }
                        }
                    }
                }
                if (ArcEager.canDo(3, currentState)) {
                    int headPos = sentence.posAt(configuration.state.bufferHead());
                    int depPos = sentence.posAt(configuration.state.peek());
                    for (String dependency : dependencyRelations) {
                        if ((!canShift && !canRightArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                            float score = classifier.score(features, "la_" + dependency, true);
                            if (score > bestScore) {
                                bestScore = score;
                                bestAction = "la_" + dependency;
                            }
                        }
                    }
                }

                if (bestAction != null) {
                    if (bestAction.equals("sh")) {
                        ArcEager.shift(configuration.state);
                    } else if (bestAction.equals("rd")) {
                        ArcEager.reduce(configuration.state);
                    } else {
                        String act = bestAction.substring(0, 2);
                        String label = bestAction.substring(3);

                        if (act.equals("la")) {
                            ArcEager.leftArc(configuration.state, label);
                        } else {
                            ArcEager.rightArc(configuration.state, label);
                        }
                    }
                    configuration.addScore(bestScore);
                    configuration.addAction(bestAction);
                }
                if (beam.size() == 0) {
                    System.out.println("WHY?");
                }
            }
        }

        Configuration bestConfiguration = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Configuration configuration : beam) {
            if (configuration.getScore(true) > bestScore) {
                bestScore = configuration.getScore(true);
                bestConfiguration = configuration;
            }
        }
        return bestConfiguration;
    }

    /**
     * Needs Conll 2006 format
     *
     * @param inputFile
     * @param outputFile
     * @param rootFirst
     * @param beamWidth
     * @throws Exception
     */
    public void parseConllFile(String inputFile, String outputFile, boolean rootFirst, int beamWidth, boolean lowerCased) throws Exception {
        CoNLLReader reader = new CoNLLReader(inputFile);
        long start = System.currentTimeMillis();
        int allArcs = 0;
        int size = 0;
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile+".tmp"));
        int dataCount = 0;

        ArrayList<String> outputs=new ArrayList<String>(2000);

        while (true) {
            ArrayList<GoldConfiguration> data = reader.readData(5000, true, true, rootFirst, lowerCased, maps);
            size += data.size();
            if (data.size() == 0)
                break;

            for (GoldConfiguration goldConfiguration : data) {
                dataCount++;
                if (dataCount % 100 == 0)
                    System.err.print(dataCount + " ... ");
                Configuration bestParse = parse(goldConfiguration.getSentence(), rootFirst, beamWidth);


                int[] words = goldConfiguration.getSentence().getWords();
                allArcs+=words.length-1;

                StringBuilder finalOutput = new StringBuilder();
                for (int i = 0; i < words.length; i++) {
                    int w = i + 1;
                    int head = bestParse.state.getHead(w);
                    String dep = bestParse.state.getDependency(w);

                    if(w==bestParse.state.rootIndex && !rootFirst)
                        continue;

                    if (head == bestParse.state.rootIndex)
                        head = 0;

                    String output = head + "\t" + dep + "\n";
                    finalOutput.append(output);
                }
                finalOutput.append("\n");
                if(outputs.size()<2000){
                    outputs.add(finalOutput.toString());
                } else{
                    for(String o:outputs)
                        writer.write(o);
                    outputs=new ArrayList<String>(2000);
                }
            }
        }

        if(outputs.size()>0){
            for(String o:outputs)
                writer.write(o);
        }

        System.err.print("\n");
        long end = System.currentTimeMillis();
        float each = (1.0f * (end - start)) / size;
        float eacharc = (1.0f * (end - start)) / allArcs;

        writer.flush();
        writer.close();

        DecimalFormat format = new DecimalFormat("##.00");

        System.err.print(format.format(eacharc) + " ms for each arc!\n");
        System.err.print(format.format(each) + " ms for each sentence!\n\n");

        BufferedReader gReader=new BufferedReader(new FileReader(inputFile));
        BufferedReader pReader=new BufferedReader(new FileReader(outputFile+".tmp"));
        BufferedWriter pwriter = new BufferedWriter(new FileWriter(outputFile));

        String line;
        while((line=pReader.readLine())!=null){
            String gLine=gReader.readLine();
            if(line.trim().length()>0){
                while(gLine.trim().length()==0)
                    gLine=gReader.readLine();
                String[] ps=line.split("\t");
                String[] gs=gLine.split("\t");
                    gs[6] = ps[0];
                    gs[7] = ps[1];
                StringBuilder output=new StringBuilder();
                for(int i=0;i<gs.length;i++){
                    output.append(gs[i]+"\t");
                }
                pwriter.write(output.toString().trim()+"\n");
            } else{
                pwriter.write("\n");
            }
        }
        pwriter.flush();
        pwriter.close();
        System.err.print("done!\n");

    }


    public void parseTaggedFile(String inputFile, String outputFile, boolean rootFirst, int beamWidth, boolean lowerCased,String separator) throws Exception {
         BufferedReader reader=new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer=new BufferedWriter(new FileWriter(outputFile));
      HashMap<String,Integer> wordMap= maps.getWordMap();

        String line;
        int count=0;
        while((line=reader.readLine())!=null){
            count++;
            if(count%100==0)
                System.err.print(count+"...");
            line=line.trim();
            String[] wrds=line.split(" ");
            String[] words =new String[wrds.length];
            String[] posTags = new String[wrds.length];

            ArrayList<Integer> tokens = new ArrayList<Integer>();
            ArrayList<Integer> tags = new ArrayList<Integer>();


            int i=0;
            for(String w:wrds){
                if (w.length()==0)
                    continue;
                int index=w.lastIndexOf(separator);
                String word= w.substring(0, index);
                if(lowerCased)
                    word=word.toLowerCase();
                String pos=w.substring(index+1);
                words[i]=word;
                posTags[i++]=pos;

                int wi=-1;
                if(wordMap.containsKey(word))
                    wi=wordMap.get(word);

                int pi=-1;
                if(wordMap.containsKey(pos))
                    pi=wordMap.get(pos);

                tokens.add(wi);
                tags.add(pi);
            }

            if(!rootFirst) {
                tokens.add(0);
                tags.add(0);
            }
            Sentence sentence=new Sentence(tokens,tags);
            Configuration bestParse=parse(sentence,rootFirst,beamWidth);


            StringBuilder finalOutput = new StringBuilder();
            for (i = 0; i < words.length; i++) {

                String word = words[i];
                String pos = posTags[i];

                int w = i + 1;
                int head = bestParse.state.getHead(w);
                String dep = bestParse.state.getDependency(w);


                String lemma = "_";

                String fpos = "_";

                if (head == bestParse.state.rootIndex)
                    head = 0;
                String output = w + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + fpos + "\t_\t" + head + "\t" + dep + "\t_\t_\n";
                finalOutput.append(output);
            }
            if(words.length>0)
            finalOutput.append("\n");
            writer.write(finalOutput.toString());

        }
        writer.flush();
        writer.close();
        System.err.println("done!");

    }
}
