/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Learning;

import Structures.InfStruct;
import TransitionBasedSystem.Parser.Actions;

import java.util.HashMap;

public class AveragedPerceptron {
    /**
     * This class tries to implement averaged Perceptron algorithm
     * Collins, Michael. "Discriminative training methods for hidden Markov models: Theory and experiments with Perceptron algorithms."
     * In Proceedings of the ACL-02 conference on Empirical methods in natural language processing-Volume 10, pp. 1-8.
     * Association for Computational Linguistics, 2002.
     * <p/>
     * The averaging update is also optimized by using the trick introduced in Hal Daume's dissertation.
     * For more information see the second chapter of his thesis:
     * Harold Charles Daume' III. "Practical Structured Learning Techniques for Natural Language Processing", PhD thesis, ISI USC, 2006.
     * http://www.umiacs.umd.edu/~hal/docs/daume06thesis.pdf
     */
    /**
     * For the weights for all features
     */
    public HashMap<Long,Float>[] shiftFeatureWeights;
    public HashMap<Long,Float>[] reduceFeatureWeights;
    public HashMap<Long,float[]>[] leftArcFeatureWeights;
    public HashMap<Long,float[]>[] rightArcFeatureWeights;

    public int iteration;
    public  int dependencySize;
    /**
     * This is the main part of the extension to the original perceptron algorithm which the averaging over all the history
     */
    public HashMap<Long,Float>[] shiftFeatureAveragedWeights;
    public HashMap<Long,Float>[] reduceFeatureAveragedWeights;
    public HashMap<Long,float[]>[] leftArcFeatureAveragedWeights;
    public HashMap<Long,float[]>[] rightArcFeatureAveragedWeights;


    public AveragedPerceptron(int featSize, int dependencySize) {
        shiftFeatureWeights=new HashMap[featSize];
        reduceFeatureWeights=new HashMap[featSize];
        leftArcFeatureWeights=new HashMap[featSize];
        rightArcFeatureWeights=new HashMap[featSize];

        shiftFeatureAveragedWeights=new HashMap[featSize];
        reduceFeatureAveragedWeights=new HashMap[featSize];
        leftArcFeatureAveragedWeights=new HashMap[featSize];
        rightArcFeatureAveragedWeights=new HashMap[featSize];
        for(int i=0;i<featSize;i++){
            shiftFeatureWeights[i]=new HashMap<Long, Float>();
            reduceFeatureWeights[i]=new HashMap<Long, Float>();
            leftArcFeatureWeights[i]=new HashMap<Long, float[]>();
            rightArcFeatureWeights[i]=new HashMap<Long, float[]>();


            shiftFeatureAveragedWeights[i]=new HashMap<Long, Float>();
            reduceFeatureAveragedWeights[i]=new HashMap<Long, Float>();
            leftArcFeatureAveragedWeights[i]=new HashMap<Long, float[]>();
            rightArcFeatureAveragedWeights[i]=new HashMap<Long, float[]>();
        }

        iteration = 1;
        this.dependencySize=dependencySize;
    }

    private AveragedPerceptron( HashMap<Long,Float>[]  shiftFeatureAveragedWeights,  HashMap<Long,Float>[]  reduceFeatureAveragedWeights,
                              HashMap<Long,float[]>[] leftArcFeatureAveragedWeights,HashMap<Long,float[]>[] rightArcFeatureAveragedWeights,
                              int dependencySize) {
        this.shiftFeatureAveragedWeights=shiftFeatureAveragedWeights;
        this.reduceFeatureAveragedWeights=reduceFeatureAveragedWeights;
        this.leftArcFeatureAveragedWeights=leftArcFeatureAveragedWeights;
        this.rightArcFeatureAveragedWeights=rightArcFeatureAveragedWeights;
        this.dependencySize = dependencySize;
    }
    
    public AveragedPerceptron(InfStruct infStruct){
        this(infStruct.shiftFeatureAveragedWeights,infStruct.reduceFeatureAveragedWeights,infStruct.leftArcFeatureAveragedWeights,infStruct.rightArcFeatureAveragedWeights,infStruct.dependencySize);
    }

    public float changeWeight(Actions actionType, int slotNum, Long featureName, int labelIndex, float change) {
        if (featureName == null)
            return 0;
        if(actionType==Actions.Shift){
            if(!shiftFeatureWeights[slotNum].containsKey(featureName))
                shiftFeatureWeights[slotNum].put(featureName,change);
            else
                shiftFeatureWeights[slotNum].put(featureName, shiftFeatureWeights[slotNum].get(featureName)+ change);

            if(!shiftFeatureAveragedWeights[slotNum].containsKey(featureName))
                shiftFeatureAveragedWeights[slotNum].put(featureName,iteration*change);
            else
                shiftFeatureAveragedWeights[slotNum].put(featureName, shiftFeatureAveragedWeights[slotNum].get(featureName)+ iteration*change);
        } else if(actionType==Actions.Reduce){
            if(!reduceFeatureWeights[slotNum].containsKey(featureName))
                reduceFeatureWeights[slotNum].put(featureName,change);
            else
                reduceFeatureWeights[slotNum].put(featureName, reduceFeatureWeights[slotNum].get(featureName)+ change);

            if(!reduceFeatureAveragedWeights[slotNum].containsKey(featureName))
                reduceFeatureAveragedWeights[slotNum].put(featureName,iteration*change);
            else
                reduceFeatureAveragedWeights[slotNum].put(featureName, reduceFeatureAveragedWeights[slotNum].get(featureName)+ iteration*change);
        }   else if(actionType==Actions.RightArc){
            changeFeatureWeight(rightArcFeatureWeights[slotNum],featureName,labelIndex,change,dependencySize);
            changeFeatureAveragedWeight(rightArcFeatureAveragedWeights[slotNum], featureName, labelIndex, change, dependencySize);
        }   else if(actionType==Actions.LeftArc){
            changeFeatureWeight(leftArcFeatureWeights[slotNum],featureName,labelIndex,change,dependencySize);
            changeFeatureAveragedWeight(leftArcFeatureAveragedWeights[slotNum], featureName, labelIndex, change, dependencySize);
        }

        return change;
    }
    
    public void changeFeatureWeight( HashMap<Long, float[]> map,  Long featureName, int labelIndex, float change,int size) {
        float[] values = map.get(featureName);
        if (values != null) {
            values[labelIndex]+=change;
        }
        else {
            values=new float[size];
            values[labelIndex] =change;
            map.put(featureName,values);
        }
    }

    public void changeFeatureAveragedWeight( HashMap<Long, float[]> map,  Long featureName, int labelIndex, float change,int size) {
        float[] values = map.get(featureName);
        if (values != null)
            values[labelIndex]+=iteration * change;
        else {
            values=new float[size];
            values[labelIndex] = iteration * change;
            map.put(featureName, values);
        }
    }
    

    /**
     * Adds to the iterations
     */
    public void incrementIteration() {
        iteration++;
    }

    public float shiftScore(final Long[] features, boolean decode){
        float score=0.0f;

        HashMap<Long,Float>[] map=decode?shiftFeatureAveragedWeights:shiftFeatureWeights;
        
        for(int i=0;i<features.length;i++){
            if (features[i] == null || (i >= 26 && i < 32))
                continue;
           Float values=map[i].get(features[i]);
            if(values!=null){
                score+=values;
            }
        }
        
        return score;
    }

    public float reduceScore(final Long[] features, boolean decode){
        float score=0.0f;

        HashMap<Long,Float>[] map=decode?reduceFeatureAveragedWeights:reduceFeatureWeights;

        for(int i=0;i<features.length;i++){
            if (features[i] == null || (i >= 26 && i < 32))
                continue;
            Float values=map[i].get(features[i]);
            if(values!=null){
                score+=values;
            }
        }

        return score;
    }

    public float[] leftArcScores(final Long[] features, boolean decode){
        float scores[]=new float[dependencySize];

        HashMap<Long,float[]>[] map=decode?leftArcFeatureAveragedWeights:leftArcFeatureWeights;

        for(int i=0;i<features.length;i++){
            if (features[i] == null)
                continue;
            float[] values=map[i].get(features[i]);
            if(values!=null){
               for(int d=0;d<dependencySize;d++){
                   scores[d]+=values[d];
               }
            }
        }

        return scores;
    }

    public float[] rightArcScores(final Long[] features, boolean decode){
        float scores[]=new float[dependencySize];

        HashMap<Long,float[]>[] map=decode?rightArcFeatureAveragedWeights:rightArcFeatureWeights;

        for(int i=0;i<features.length;i++){
            if (features[i] == null)
                continue;
            float[] values=map[i].get(features[i]);
            if(values!=null){
                for(int d=0;d<dependencySize;d++){
                    scores[d]+=values[d];
                }
            }
        }

        return scores;
    }

    public int featureSize() {
        return shiftFeatureAveragedWeights.length;
    }
}
