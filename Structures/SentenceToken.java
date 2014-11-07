package Structures;

/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/
public class SentenceToken implements Comparable, Cloneable {
    /**
     * Shows the word of a token
     */
    private String word;

    /**
     * Shows the part of speech tag of a token
     */
    private String POS;

    /**
     * Shows other properties of a word, such as lemma, word cluster, morphological features, etc.
     */
    // private  HashMap<String,String> features;


    /**
     * Default constructor of a sentence token
     *
     * @param Word word of a token
     * @param POS  Part of speech tag of a token
     */
    public SentenceToken(String Word, String POS) {
        this.word = Word;
        this.POS = POS;
        // features =new HashMap<String, String>();
    }

    /**
     * Default constructor of a sentence token
     *
     * @param word word of a token
     */
    public SentenceToken(String word, int initialPosition) {
        this(word, "_");
    }

    public String getWord() {
        return word;
    }

    public String getPOS() {
        return POS;
    }

    /**
     * Gets the value for the feature
     * @param feature
     * @return
     */
    /*
    public String getFeature(String feature){
        if(features.containsKey(feature))
            return features.get(feature);
        return null;
    }
    */

    /**
     * Adds a new feature to the feature sets
     *
     * @param feature feature type
     * @param value   feature value
     */
   /* public void addFeature(String feature, String value){
        features.put(feature,value);
    }
    */
    public void changeContent(SentenceToken sentenceToken) {
        this.word = sentenceToken.getWord();
        this.POS = sentenceToken.getPOS();
    }

    @Override
    public SentenceToken clone() {
        SentenceToken token = new SentenceToken(word, POS);
        //  for(String feat:features.keySet()){
        //      token.features.put(feat,features.get(feat));
        //  }
        return token;
    }

    @Override
    public int hashCode() {
        int hashCode = (word + ":" + POS).hashCode();

        //   for(String feat:features.keySet()){
        //      hashCode+=feat.hashCode()+features.get(feat).hashCode();
        //  }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SentenceToken) {
            SentenceToken sentenceToken = (SentenceToken) obj;
            if (word.equals(sentenceToken.word) && POS.equals(sentenceToken.POS)
                    ) {
                /*
                if(features.size()!= sentenceToken.features.size())
                    return false;
                for(String feat:features.keySet())
                    if(!sentenceToken.features.containsKey(feat) || !sentenceToken.features.get(feat).equals(features.get(feat)))
                        return false;
                        */
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int compareTo(Object obj) {
        if (equals(obj))
            return 0;
        return hashCode() - obj.hashCode();
    }
}
