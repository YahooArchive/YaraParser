/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package TransitionBasedSystem.Parser;

import Accessories.CoNLLReader;
import Accessories.Pair;
import Learning.OnlineClassifier;
import Structures.Sentence;
import Structures.SentenceToken;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Configuration.GoldConfiguration;
import TransitionBasedSystem.Configuration.State;
import TransitionBasedSystem.Features.FeatureExtractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

public class KBeamArcEagerParser extends TransitionBasedParser {
    /**
     * Any kind of classifier that can give us scores
     */
    OnlineClassifier classifier;

    ArrayList<String> dependencyRelations;

   int  featureLength;
    HashSet<String> punctuations;

    // for pruning irrelevant search space
    HashMap<String, HashMap<String, HashSet<String>>> headDepSet;

    public KBeamArcEagerParser(OnlineClassifier classifier, ArrayList<String> dependencyRelations,
                               HashMap<String, HashMap<String, HashSet<String>>> headDepSet, int featureLength) {
        this.classifier = classifier;
        this.dependencyRelations = dependencyRelations;
        this.featureLength = featureLength;
        this.headDepSet = headDepSet;

        punctuations = new HashSet<String>();
        punctuations.add("#");
        punctuations.add("$");
        punctuations.add("''");
        punctuations.add("(");
        punctuations.add(")");
        punctuations.add("[");
        punctuations.add("]");
        punctuations.add("{");
        punctuations.add("}");
        punctuations.add("\"");
        punctuations.add(",");
        punctuations.add(".");
        punctuations.add(":");
        punctuations.add("``");
        punctuations.add("-LRB-");
        punctuations.add("-RRB-");
        punctuations.add("-LSB-");
        punctuations.add("-RSB-");
        punctuations.add("-LCB-");
        punctuations.add("-RCB-");
    }

    public Configuration parse(Sentence sentence, boolean rootFirst, int beamWidth) throws Exception {
        Configuration initialConfiguration = new Configuration(sentence, rootFirst);

        ArrayList<Configuration> beam = new ArrayList<Configuration>(beamWidth);
        beam.add(initialConfiguration);

        while (!ArcEager.isTerminal(beam)) {
            if (beamWidth != 1) {
                TreeMap<Double, ArrayList<Pair<Integer, Pair<String, Double>>>> beamPreserver = new TreeMap<Double, ArrayList<Pair<Integer, Pair<String, Double>>>>();
                for (int b = 0; b < beam.size(); b++) {
                    Configuration configuration = beam.get(b);
                    State currentState = configuration.state;
                    double prevScore = configuration.score;
                    boolean canShift = ArcEager.canDo("sh", currentState);
                    boolean canReduce = ArcEager.canDo("rd", currentState);
                    boolean canRightArc = ArcEager.canDo("ra", currentState);
                    boolean canLeftArc = ArcEager.canDo("la", currentState);
                    Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                    if (!canShift
                            && !canReduce
                            && !canRightArc
                            && !canLeftArc) {
                        double addedScore = prevScore;
                        if (!beamPreserver.containsKey(addedScore))
                            beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                        beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("unshift", 0.0)));

                    }

                    if (canShift) {
                        double score = classifier.score(features, "sh", true);
                        double addedScore = score + prevScore;
                        if (!beamPreserver.containsKey(addedScore))
                            beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                        beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("sh", score)));
                    }

                    if (canReduce) {
                        double score = classifier.score(features, "rd", true);
                        double addedScore = score + prevScore;
                        if (!beamPreserver.containsKey(addedScore))
                            beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                        beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("rd", score)));
                    }

                    if (canRightArc) {
                        String headPos = sentence.posAt(configuration.state.peek());
                        String depPos = sentence.posAt(configuration.state.bufferHead());
                        for (String dependency : dependencyRelations) {
                            if ((!canLeftArc && !canShift && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                    && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                double score = classifier.score(features, "ra_" + dependency, true);
                                double addedScore = score + prevScore;
                                if (!beamPreserver.containsKey(addedScore))
                                    beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                                beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("ra_" + dependency, score)));
                            }
                        }
                    }

                    if (canLeftArc) {
                        String headPos = sentence.posAt(configuration.state.bufferHead());
                        String depPos = sentence.posAt(configuration.state.peek());
                        for (String dependency : dependencyRelations) {
                            if ((!canShift && !canRightArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                    && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                                double score = classifier.score(features, "la_" + dependency, true);
                                double addedScore = score + prevScore;
                                if (!beamPreserver.containsKey(addedScore))
                                    beamPreserver.put(addedScore, new ArrayList<Pair<Integer, Pair<String, Double>>>());
                                beamPreserver.get(addedScore).add(new Pair<Integer, Pair<String, Double>>(b, new Pair<String, Double>("la_" + dependency, score)));
                            }
                        }
                    }
                }

                ArrayList<Configuration> repBeam = new ArrayList<Configuration>(beamWidth);
                for (double sc : beamPreserver.descendingKeySet()) {
                    if (repBeam.size() >= beamWidth)
                        break;
                    ArrayList<Pair<Integer, Pair<String, Double>>> values = beamPreserver.get(sc);
                    for (Pair<Integer, Pair<String, Double>> val : values) {
                        if (repBeam.size() >= beamWidth)
                            break;
                        int b = val.first;
                        String action = val.second.first;
                        double score = val.second.second;

                        Configuration newConfig = beam.get(b).clone();

                        if (action.startsWith("sh")) {
                            ArcEager.shift(newConfig.state);
                        } else if (action.startsWith("rd")) {
                            ArcEager.reduce(newConfig.state);
                        } else if (action.startsWith("ra")) {
                            String label = action.split("_")[1];
                            ArcEager.rightArc(newConfig.state, label);
                        } else if (action.startsWith("la")) {
                            String label = action.split("_")[1];
                            ArcEager.leftArc(newConfig.state, label);
                        } else if (action.equals("unshift")) {
                            ArcEager.unShift(newConfig.state);
                        }
                        // newConfig.addAction(action);
                        newConfig.addScore(score);
                        repBeam.add(newConfig);
                    }
                }
                beam = repBeam;
            } else {
                Configuration configuration = beam.get(0);
                State currentState = configuration.state;
                Object[] features = FeatureExtractor.extractAllParseFeatures(configuration, featureLength);
                double bestScore = Double.NEGATIVE_INFINITY;
                String bestAction = null;

                boolean canShift = ArcEager.canDo("sh", currentState);
                boolean canReduce = ArcEager.canDo("rd", currentState);
                boolean canRightArc = ArcEager.canDo("ra", currentState);
                boolean canLeftArc = ArcEager.canDo("la", currentState);

                if (!canShift
                        && !canReduce
                        && !canRightArc
                        && !canLeftArc) {

                    if (!currentState.stackEmpty()) {
                        ArcEager.unShift(currentState);
                        configuration.addAction("unshift");
                    } else if (!currentState.bufferEmpty() && currentState.stackEmpty()) {
                        ArcEager.shift(currentState);
                        configuration.addAction("sh");
                    }
                }

                if (canShift) {
                    double score = classifier.score(features, "sh", true);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAction = "sh";
                    }
                }
                if (canReduce) {
                    double score = classifier.score(features, "rd", true);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAction = "rd";
                    }
                }
                if (canRightArc) {
                    String headPos = sentence.posAt(configuration.state.peek());
                    String depPos = sentence.posAt(configuration.state.bufferHead());
                    for (String dependency : dependencyRelations) {
                        if ((!canShift && !canLeftArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                            double score = classifier.score(features, "ra_" + dependency, true);
                            if (score > bestScore) {
                                bestScore = score;
                                bestAction = "ra_" + dependency;
                            }
                        }
                    }
                }
                if (ArcEager.canDo("la", currentState)) {
                    String headPos = sentence.posAt(configuration.state.bufferHead());
                    String depPos = sentence.posAt(configuration.state.peek());
                    for (String dependency : dependencyRelations) {
                        if ((!canShift && !canRightArc && !canReduce) || (headDepSet.containsKey(headPos) && headDepSet.get(headPos).containsKey(depPos)
                                && headDepSet.get(headPos).get(depPos).contains(dependency))) {
                            double score = classifier.score(features, "la_" + dependency, true);
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
                        String act = bestAction.split("_")[0];
                        String label = bestAction.split("_")[1];

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
        double bestScore = Double.NEGATIVE_INFINITY;
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
        double labeledCorrect = 0;
        double unlabaledCorrect = 0;
        int size=0;
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        int dataCount=0;

        while(true) {
            ArrayList< GoldConfiguration> data = reader.readData(5000, true, true, rootFirst, lowerCased);
            size+=data.size();
            if(data.size()==0)
                break;


            System.err.println(data.size());

            for (GoldConfiguration goldConfiguration : data) {
                dataCount++;
                if (dataCount % 100 == 0)
                    System.err.print(dataCount + " ... ");
                Configuration bestParse = parse(goldConfiguration.getSentence(), rootFirst, beamWidth);


                String[] words = goldConfiguration.getSentence().getWords();
                String[] tags = goldConfiguration.getSentence().getTags();
                HashMap<Integer, Pair<Integer, String>> gold = goldConfiguration.getGoldDependencies();

                StringBuilder finalOutput = new StringBuilder();
                for (int i = 0; i < words.length; i++) {

                    String word = words[i];
                    String pos = tags[i];

                    int w = i + 1;
                    int head = bestParse.state.getHead(w);
                    String dep = bestParse.state.getDependency(w);

                    if (gold.containsKey(w)) {

                        double goldHead = gold.get(w).first;
                        String goldDep = gold.get(w).second;

                        if (!punctuations.contains(pos)) {
                            if (head == goldHead) {
                                unlabaledCorrect += 1;
                                if (goldDep.equals(dep)) {
                                    labeledCorrect += 1;
                                }
                            }
                            allArcs++;
                        }


                        String lemma = "_";

                        String fpos = "_";

                        if(head==bestParse.state.rootIndex)
                            head=0;
                        String output = w + "\t" + word + "\t" + lemma + "\t" + pos + "\t" + fpos + "\t_\t" + head + "\t" + dep + "\t_\t_\n";
                        finalOutput.append(output);
                    }
                }
                finalOutput.append("\n");
                writer.write(finalOutput.toString());

            }
        }
        System.err.print("\n");
        long end = System.currentTimeMillis();
        double each = (1.0 * (end - start)) / size;
        double eacharc = (1.0 * (end - start)) / allArcs;

        writer.flush();
        writer.close();

        double labeledAccuracy = 100.0 * labeledCorrect / allArcs;
        double unlabaledAccuracy = 100.0 * unlabaledCorrect / allArcs;
        System.err.println("\nLabeled accuracy is  " + labeledAccuracy);
        System.err.println("Unlabeled accuracy is  " + unlabaledAccuracy);
        System.err.print(eacharc + " for each arc!\n");
        System.err.print(each + " for each sentence!\n");
        System.err.print("done!\n");
    }


    /**
     * The input file should be each sentence in one line and word/pos are separated by under-score (e.g. does_VBZ)
     * @param inputFile
     * @param outputFile
     * @param rootFirst
     * @param beamWidth
     * @param lowerCased
     * @throws Exception
     */
    public void parseTaggedFile(String inputFile, String outputFile, boolean rootFirst, int beamWidth, boolean lowerCased) throws Exception{
        BufferedReader reader=new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer=new BufferedWriter(new FileWriter(outputFile));

        String line;
        int count=0;
        while((line=reader.readLine())!=null){
            count++;
            if(count%100==0)
                System.err.print(count+"...");
            line=line.trim();
            String[] wrds=line.split(" ");
            ArrayList<SentenceToken> tokens=new ArrayList<SentenceToken>();
            for(String w:wrds){
                int index=w.lastIndexOf("_");
                String word=w.substring(0,index);
                if(lowerCased)
                    word=word.toLowerCase();
                String pos=w.substring(index+1);
                tokens.add(new SentenceToken(word,pos));
            }

            if(!rootFirst)
                tokens.add(new SentenceToken("ROOT","ROOT"));
            Sentence sentence=new Sentence(tokens);
            Configuration bestParse=parse(sentence,rootFirst,beamWidth);


            String[] words = bestParse.sentence.getWords();
            String[] tags =  bestParse.sentence.getTags();

            StringBuilder finalOutput = new StringBuilder();
            for (int i = 0; i < words.length; i++) {

                String word = words[i];
                String pos = tags[i];

                if(word.equals("ROOT") && pos.equals("ROOT"))
                    continue;

                int w = i + 1;
                double head = bestParse.state.getHead(w);
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

    /**
     * Needs Conll 2006 format
     *
     * @param inputFile
     * @param rootFirst
     * @param beamWidth
     * @throws Exception
     */
    public void parseConllFile(String inputFile, boolean rootFirst, int beamWidth, boolean lowerCased) throws Exception {
        CoNLLReader reader = new CoNLLReader(inputFile);
        ArrayList<GoldConfiguration> data = reader.readData(Integer.MAX_VALUE, true, true, rootFirst, lowerCased);
        long start = System.currentTimeMillis();
        int allArcs = 0;
        double labeledCorrect = 0;
        double unlabaledCorrect = 0;


        System.err.println(data.size());

        int ind=0;
        for (GoldConfiguration goldConfiguration : data) {
            ind++;
            if (ind % 100 == 0)
                System.err.print(ind + " ... ");
            Configuration bestParse = parse(goldConfiguration.getSentence(), rootFirst, beamWidth);


            String[] tags = goldConfiguration.getSentence().getTags();
            HashMap<Integer, Pair<Integer, String>> gold =goldConfiguration.getGoldDependencies();

            for (int i = 0; i < tags.length; i++) {
                int w = i + 1;
                String pos = tags[i];


                double head = bestParse.state.getHead(w);
                String dep = bestParse.state.getDependency(w);

                if (gold.containsKey(w)) {

                    double goldHead = gold.get(w).first;
                    String goldDep = gold.get(w).second;

                    if (!punctuations.contains(pos)) {
                        if (head == goldHead) {
                            unlabaledCorrect += 1;
                            if (goldDep.equals(dep)) {
                                labeledCorrect += 1;
                            }
                        }
                        allArcs++;
                    }
                }
            }
        }
        System.err.print("\n");
        long end = System.currentTimeMillis();
        long timeSec = (end - start) / 1000;
        double each = (1.0 * (end - start)) / data.size();
        double eacharc = (1.0 * (end - start)) / allArcs;


        double labeledAccuracy = 100.0 * labeledCorrect / allArcs;
        double unlabaledAccuracy = 100.0 * unlabaledCorrect / allArcs;
        System.err.println("\nLabeled accuracy is  " + labeledAccuracy);
        System.err.println("Unlabeled accuracy is  " + unlabaledAccuracy);
        System.err.print(eacharc + " for each arc!\n");
        System.err.print(each + " for each sentence!\n");
        System.err.print("done!\n");
    }
}
