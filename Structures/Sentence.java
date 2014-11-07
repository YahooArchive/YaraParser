package Structures;

import java.util.ArrayList;

/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/
public class Sentence implements Comparable{
    /**
     * shows the tokens of a specific sentence
     */
    private String[] words;
    private String[] tags;

    public Sentence(ArrayList<SentenceToken> tokens) {
       words = new String[tokens.size()];
        tags = new String[tokens.size()];

        for (int i = 0; i < tokens.size(); i++) {
           SentenceToken tok = tokens.get(i);
            words[i]=tok.getWord();
            tags[i]=tok.getPOS();
        }
    }

    public Sentence(String[] words, String[] tags){
        this.words=words;
        this.tags=tags;
    }

    public int size() {
        return words.length;
    }

    public SentenceToken tokenAt(int position) throws Exception {
        if (position < 0)
            throw new ArrayIndexOutOfBoundsException("The position is outside the boundary of this sentence: " + position);
        if (position == 0)
            return new SentenceToken("ROOT", "ROOT");

         return new SentenceToken(words[position - 1],tags[position-1]);
    }

    public String posAt(int position) throws Exception {
        if (position < 0)
            throw new ArrayIndexOutOfBoundsException("The position is outside the boundary of this sentence: " + position);
        if (position == 0)
            return "ROOT";

        return tags[position - 1];
    }

    public String[] getWords() {
        return words;
    }

    public String[] getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sentence) {
            Sentence sentence = (Sentence) obj;
            if (sentence.words.length != words.length)
                return false;
            for (int i = 0; i < sentence.words.length; i++) {
                if (!sentence.words[i].equals(words[i]))
                    return false;
                if (!sentence.tags[i].equals(tags[i]))
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
        for (int tokenId =0;tokenId<words.length;tokenId++) {
            builder.append(words[tokenId] + "/" + tags[tokenId] + " ");
        }
        return builder.toString().hashCode();
    }

}
