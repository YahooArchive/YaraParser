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
    private String[] wordStr;
    private String[] tagStr;

    public Sentence(ArrayList<Integer> tokens, ArrayList<Integer> pos) {
        words = new int[tokens.size()];
        tags = new int[tokens.size()];
        wordStr = new String[tokens.size()];
        tagStr = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            words[i] = tokens.get(i);
            tags[i] = pos.get(i);
            wordStr[i] = (words[i] + "").intern();
            tagStr[i] = (tags[i] + "").intern();
        }
    }

    public Sentence(int[] words, int[] tags) {
        this.words = words;
        this.tags = tags;
        wordStr = new String[words.length];
        tagStr = new String[words.length];
        for (int i = 0; i < words.length; i++) {
            wordStr[i] = (words[i] + "").intern();
            tagStr[i] = (tags[i] + "").intern();
        }
    }

    public int size() {
        return words.length;
    }

    public int posAt(int position) throws Exception {
        if (position < 0)
            throw new ArrayIndexOutOfBoundsException("The position is outside the boundary of this sentence: " + position);
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

    public String[] getWordStr() {
        return wordStr;
    }

    public String[] getTagStr() {
        return tagStr;
    }
}
