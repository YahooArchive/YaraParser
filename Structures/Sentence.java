/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Structures;

import java.util.ArrayList;

public class Sentence implements Comparable {
    /**
     * shows the tokens of a specific sentence
     */
    private int[] words;
    private int[] tags;

    public Sentence(ArrayList<Integer> tokens, ArrayList<Integer> pos) {
        words = new int[tokens.size()];
        tags = new int[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            words[i] = tokens.get(i);
            tags[i] = pos.get(i);
        }
    }

    public Sentence(int[] words, int[] tags) {
        this.words = words;
        this.tags = tags;
    }

    public int size() {
        return words.length;
    }

    public int posAt(int position) {
        if (position == 0)
            return 0;

        return tags[position - 1];
    }

    public int[] getWords() {
        return words;
    }

    public int[] getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sentence) {
            Sentence sentence = (Sentence) obj;
            if (sentence.words.length != words.length)
                return false;
            for (int i = 0; i < sentence.words.length; i++) {
                if (sentence.words[i] != words[i])
                    return false;
                if (sentence.tags[i] != tags[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(Object o) {
        if (equals(o))
            return 0;
        return hashCode() - o.hashCode();
    }

    @Override
    public int hashCode() {
        StringBuilder builder = new StringBuilder();
        for (int tokenId = 0; tokenId < words.length; tokenId++) {
            builder.append(words[tokenId] + "/" + tags[tokenId] + " ");
        }
        return builder.toString().hashCode();
    }

}
