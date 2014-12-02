/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Structures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class IndexMaps implements Serializable {
    public HashSet<Integer> punctuations;
    private HashMap<String, Integer> wordMap;
    private HashMap<String, Integer> labels;
    public String[] revWords;

    public IndexMaps(HashMap<String, Integer> wordMap, HashMap<String, Integer> labels) {
        this.wordMap = wordMap;
        this.labels = labels;

        HashSet<String> punctuations = new HashSet<String>();
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

        revWords = new String[wordMap.size() + 1];
        revWords[0] = "ROOT";
        this.punctuations = new HashSet<Integer>();

        for (String word : wordMap.keySet()) {
            revWords[wordMap.get(word)] = word;
            if (punctuations.contains(word))
                this.punctuations.add(wordMap.get(word));
        }
    }

    public Sentence makeSentence(String[] words,String[] posTags,boolean rootFirst,boolean lowerCased){
        ArrayList<Integer> tokens = new ArrayList<Integer>();
        ArrayList<Integer> tags = new ArrayList<Integer>();


        int i=0;
        for(String word:words){
            if (word.length()==0)
                continue;
            if(lowerCased)
                word=word.toLowerCase();
            String pos=posTags[i];

            int wi=-1;
            if(wordMap.containsKey(word))
                wi=wordMap.get(word);

            int pi=-1;
            if(wordMap.containsKey(pos))
                pi=wordMap.get(pos);

            tokens.add(wi);
            tags.add(pi);

            i++;
        }

        if(!rootFirst) {
            tokens.add(0);
            tags.add(0);
        }

        return new Sentence(tokens,tags);
    }

    public HashMap<String, Integer> getWordMap() {
        return wordMap;
    }

    public HashSet<Integer> getPunctuations() {
        return punctuations;
    }


    public String[] getRevWords() {
        return revWords;
    }

    public HashMap<String, Integer> getLabels() {
        return labels;
    }
}
