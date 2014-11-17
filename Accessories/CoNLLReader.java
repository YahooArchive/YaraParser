/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/

package Accessories;

import Structures.Sentence;
import Structures.SentenceToken;
import TransitionBasedSystem.Configuration.GoldConfiguration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class CoNLLReader {
    /**
     * An object for reading the CoNLL file
     */
    BufferedReader fileReader;

    /**
     * Initializes the file reader
     *
     * @param filePath Path to the file
     * @throws Exception If the file path is not correct or there are not enough permission to read the file
     */
    public CoNLLReader(String filePath) throws Exception {
        fileReader = new BufferedReader(new FileReader(filePath));
    }

    /**
     * @param limit it is used if we want to read part of the data
     * @return
     */
    public ArrayList<GoldConfiguration> readData(int limit, boolean keepNonProjective, boolean labeled, boolean rootFirst, boolean lowerCased) throws Exception {
        int illegalHeadCount = 0;

        ArrayList<GoldConfiguration> configurationSet = new ArrayList<GoldConfiguration>();

        String line;
        ArrayList<SentenceToken> tokens = new ArrayList<SentenceToken>();

        HashMap<Integer, Pair<Integer, String>> goldDependencies = new HashMap<Integer, Pair<Integer, String>>();
        int sentenceCounter = 0;
        while ((line = fileReader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) {
                if (tokens.size() >= 1) {
                    sentenceCounter++;
                    if (!rootFirst) {
                        for (int gold : goldDependencies.keySet()) {
                            if (goldDependencies.get(gold).first.equals(0))
                                goldDependencies.get(gold).setFirst(goldDependencies.size() + 1);
                        }
                        tokens.add(new SentenceToken("ROOT", "ROOT"));
                    }
                    Sentence currentSentence = new Sentence(tokens);
                    GoldConfiguration goldConfiguration = new GoldConfiguration(currentSentence, goldDependencies);
                    if (keepNonProjective || !goldConfiguration.isNonprojective())
                        configurationSet.add(goldConfiguration);
                    goldDependencies = new HashMap<Integer, Pair<Integer, String>>();
                    tokens = new ArrayList<SentenceToken>();
                } else{
                    goldDependencies = new HashMap<Integer, Pair<Integer, String>>();
                    tokens = new ArrayList<SentenceToken>();
                }
                if (sentenceCounter >= limit) {
                    System.out.println("buffer full..."+configurationSet.size());
                    break;
                }
            } else {
                String[] splitLine = line.split("\t");
                if (splitLine.length < 8)
                    throw new Exception("wrong file format");
                int wordIndex = Integer.parseInt(splitLine[0]);
                String word = splitLine[1].trim();
                word = word.replace("``", "\"").replace("-LRB-", "(").replace("-RRB-", ")").replace("''", "\"")
                        .replace("-RSB-", "]").replace("-LSB-", "[").replace("-LCB-", "{").replace("-RCB-", "}");
                if (lowerCased)
                    word = word.toLowerCase();
                String pos = splitLine[3].trim();

                SentenceToken token = new SentenceToken(word, pos);

                int headIndex = Integer.parseInt(splitLine[6]);
                if (headIndex < 0) {
                 //   illegal = true;
                    illegalHeadCount++;
                }
                String relation = splitLine[7];
                if(relation.equals("_"))
                    relation="-";
                if (!labeled)
                    relation = "~";
                goldDependencies.put(wordIndex, new Pair<Integer, String>(headIndex, relation));
                tokens.add(token);
            }
        }
        if (tokens.size() > 0) {
            if (!rootFirst) {
                for (int gold : goldDependencies.keySet()) {
                    if (goldDependencies.get(gold).first.equals(0))
                        goldDependencies.get(gold).setFirst(goldDependencies.size() + 1);
                }
                tokens.add(new SentenceToken("ROOT", "ROOT"));
            }
            sentenceCounter++;
            Sentence currentSentence = new Sentence(tokens);
            configurationSet.add( new GoldConfiguration(currentSentence, goldDependencies));
        }

        System.out.println(illegalHeadCount);
        return configurationSet;
    }
}
