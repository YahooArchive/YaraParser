/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Accessories;

import Structures.IndexMaps;
import Structures.Sentence;
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

    public static IndexMaps createIndices(String filePath, boolean labeled, boolean lowercased) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        HashMap<String, Integer> wordMap = new HashMap<String, Integer>();
        HashMap<String, Integer> labels = new HashMap<String, Integer>();
        labels.put("ROOT", 1);
        int labelCount = 2;

        int wi = 1;
        wordMap.put("ROOT", 0);

        String line;
        while ((line = reader.readLine()) != null) {
            String[] spl = line.trim().split("\t");
            if (spl.length > 7) {
                String word = spl[1];
                if (lowercased)
                    word = word.toLowerCase();
                if (!wordMap.containsKey(word)) {
                    wordMap.put(word, wi++);
                }

                String pos = spl[3];
                if (!wordMap.containsKey(pos)) {
                    wordMap.put(pos, wi++);
                }
                String label = spl[7];
                if (label.equals("_"))
                    label = "-";
                if (!labeled)
                    label = "~";
                if (!wordMap.containsKey(label)) {
                    labels.put(label, labelCount);
                    labelCount *= 2;
                    wordMap.put(label, wi++);
                }
            }
        }

        return new IndexMaps(wordMap, labels);
    }

    /**
     * @param limit it is used if we want to read part of the data
     * @return
     */
    public ArrayList<GoldConfiguration> readData(int limit, boolean keepNonProjective, boolean labeled, boolean rootFirst, boolean lowerCased, IndexMaps maps) throws Exception {
        HashMap<String, Integer> wordmap = maps.getWordMap();
        ArrayList<GoldConfiguration> configurationSet = new ArrayList<GoldConfiguration>();

        String line;
        ArrayList<Integer> tokens = new ArrayList<Integer>();
        ArrayList<Integer> tags = new ArrayList<Integer>();

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
                        tokens.add(0);
                        tags.add(0);
                    }
                    Sentence currentSentence = new Sentence(tokens, tags);
                    GoldConfiguration goldConfiguration = new GoldConfiguration(currentSentence, goldDependencies);
                    if (keepNonProjective || !goldConfiguration.isNonprojective())
                        configurationSet.add(goldConfiguration);
                    goldDependencies = new HashMap<Integer, Pair<Integer, String>>();
                    tokens = new ArrayList<Integer>();
                    tags = new ArrayList<Integer>();
                } else {
                    goldDependencies = new HashMap<Integer, Pair<Integer, String>>();
                    tokens = new ArrayList<Integer>();
                    tags = new ArrayList<Integer>();
                }
                if (sentenceCounter >= limit) {
                    System.out.println("buffer full..." + configurationSet.size());
                    break;
                }
            } else {
                String[] splitLine = line.split("\t");
                if (splitLine.length < 8)
                    throw new Exception("wrong file format");
                int wordIndex = Integer.parseInt(splitLine[0]);
                String word = splitLine[1].trim();
                if (lowerCased)
                    word = word.toLowerCase();
                String pos = splitLine[3].trim();

                int wi = -1;
                if (wordmap.containsKey(word))
                    wi = wordmap.get(word);

                int pi = -1;
                if (wordmap.containsKey(pos))
                    pi = wordmap.get(pos);

                tags.add(pi);
                tokens.add(wi);

                int headIndex = Integer.parseInt(splitLine[6]);
                String relation = splitLine[7];
                if (relation.equals("_"))
                    relation = "-";
                if (!labeled)
                    relation = "~";
                goldDependencies.put(wordIndex, new Pair<Integer, String>(headIndex, relation));
            }
        }
        if (tokens.size() > 0) {
            if (!rootFirst) {
                for (int gold : goldDependencies.keySet()) {
                    if (goldDependencies.get(gold).first.equals(0))
                        goldDependencies.get(gold).setFirst(goldDependencies.size() + 1);
                }
                tokens.add(0);
                tags.add(0);
            }
            sentenceCounter++;
            Sentence currentSentence = new Sentence(tokens, tags);
            configurationSet.add(new GoldConfiguration(currentSentence, goldDependencies));
        }

        return configurationSet;
    }
}
