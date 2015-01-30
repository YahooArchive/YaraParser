package Structures;

import Accessories.Options;
import Learning.AveragedPerceptron;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Mohammad Sadegh Rasooli.
 * ML-NLP Lab, Department of Computer Science, Columbia University
 * Date Created: 1/8/15
 * Time: 11:41 AM
 * To report any bugs or problems contact rasooli@cs.columbia.edu
 */

public class InfStruct {
    public HashMap<Long,Float>[] shiftFeatureAveragedWeights;
    public HashMap<Long,Float>[] reduceFeatureAveragedWeights;
    public HashMap<Long,float[]>[] leftArcFeatureAveragedWeights;
    public HashMap<Long,float[]>[] rightArcFeatureAveragedWeights;
    public int dependencySize;
    
    public IndexMaps maps;
    public ArrayList<Integer> dependencyLabels;
    public Options options;

    public InfStruct(HashMap<Long,Float>[] shiftFeatureAveragedWeights,HashMap<Long,Float>[] reduceFeatureAveragedWeights,HashMap<Long,float[]>[] leftArcFeatureAveragedWeights,HashMap<Long,float[]>[] rightArcFeatureAveragedWeights,
                     IndexMaps maps, ArrayList<Integer> dependencyLabels, Options options,int dependencySize) {
        this.shiftFeatureAveragedWeights = shiftFeatureAveragedWeights;
        this.reduceFeatureAveragedWeights = reduceFeatureAveragedWeights;
        this.leftArcFeatureAveragedWeights = leftArcFeatureAveragedWeights;
        this.rightArcFeatureAveragedWeights = rightArcFeatureAveragedWeights;
        this.maps = maps;
        this.dependencyLabels = dependencyLabels;
        this.options = options;
        this.dependencySize=dependencySize;
    }

    public InfStruct(AveragedPerceptron perceptron, IndexMaps maps, ArrayList<Integer> dependencyLabels, Options options) {
        shiftFeatureAveragedWeights=new HashMap[perceptron.shiftFeatureAveragedWeights.length];
        reduceFeatureAveragedWeights=new HashMap[perceptron.reduceFeatureAveragedWeights.length];
       
        HashMap<Long,Float>[] map=perceptron.shiftFeatureWeights;
        HashMap<Long,Float>[] avgMap=perceptron.shiftFeatureAveragedWeights;
        this.dependencySize=perceptron.dependencySize;
        
        for(int i=0;i<shiftFeatureAveragedWeights.length;i++){
            shiftFeatureAveragedWeights[i]=new HashMap<Long, Float>();
            for(Long feat:map[i].keySet()){
                float vals=map[i].get(feat);
                float avgVals=avgMap[i].get(feat);
                float newVals=vals-(avgVals/perceptron.iteration);
                shiftFeatureAveragedWeights[i].put(feat,newVals);
            }
        }

        HashMap<Long,Float>[] map4=perceptron.reduceFeatureWeights;
        HashMap<Long,Float>[] avgMap4=perceptron.reduceFeatureAveragedWeights;
        this.dependencySize=perceptron.dependencySize;

        for(int i=0;i<reduceFeatureAveragedWeights.length;i++){
            reduceFeatureAveragedWeights[i]=new HashMap<Long, Float>();
            for(Long feat:map4[i].keySet()){
                float vals=map4[i].get(feat);
                float avgVals=avgMap4[i].get(feat);
                float newVals=vals-(avgVals/perceptron.iteration);
                reduceFeatureAveragedWeights[i].put(feat,newVals);
            }
        }

        leftArcFeatureAveragedWeights=new HashMap[perceptron.leftArcFeatureAveragedWeights.length];
        HashMap<Long,float[]>[] map2=perceptron.leftArcFeatureWeights;
        HashMap<Long,float[]>[] avgMap2=perceptron.leftArcFeatureAveragedWeights;

        for(int i=0;i<leftArcFeatureAveragedWeights.length;i++){
            leftArcFeatureAveragedWeights[i]=new HashMap<Long, float[]>();
            for(Long feat:map2[i].keySet()){
                float[] vals=map2[i].get(feat);
                float[] avgVals=avgMap2[i].get(feat);
                float[] newVals=new float[vals.length];
                for(int f=0;f<vals.length;f++){
                    newVals[f]=vals[f]-(avgVals[f]/perceptron.iteration);
                }
                leftArcFeatureAveragedWeights[i].put(feat,newVals);
            }
        }

        rightArcFeatureAveragedWeights=new HashMap[perceptron.rightArcFeatureAveragedWeights.length];
        HashMap<Long,float[]>[] map3=perceptron.rightArcFeatureWeights;
        HashMap<Long,float[]>[] avgMap3=perceptron.rightArcFeatureAveragedWeights;

        for(int i=0;i<rightArcFeatureAveragedWeights.length;i++){
            rightArcFeatureAveragedWeights[i]=new HashMap<Long, float[]>();
            for(Long feat:map3[i].keySet()){
                float[] vals=map3[i].get(feat);
                float[] avgVals=avgMap3[i].get(feat);
                float[] newVals=new float[vals.length];
                for(int f=0;f<vals.length;f++){
                    newVals[f]=vals[f]-(avgVals[f]/perceptron.iteration);
                }
                rightArcFeatureAveragedWeights[i].put(feat,newVals);
            }
        }
        
        this.maps = maps;
        this.dependencyLabels = dependencyLabels;
        this.options = options;
    }

    public InfStruct(String modelPath) throws Exception {
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(modelPath));
        dependencyLabels = (ArrayList<Integer>) reader.readObject();
        maps = (IndexMaps) reader.readObject();
        options = (Options) reader.readObject();
        shiftFeatureAveragedWeights = (HashMap<Long, Float>[]) reader.readObject();
        reduceFeatureAveragedWeights = (HashMap<Long, Float>[]) reader.readObject();
        leftArcFeatureAveragedWeights = (HashMap<Long, float[]>[]) reader.readObject();
        rightArcFeatureAveragedWeights = (HashMap<Long, float[]>[]) reader.readObject();
        dependencySize =  reader.readInt();
    }

    public void saveModel(String modelPath) throws Exception {
        ObjectOutput writer = new ObjectOutputStream(new FileOutputStream(modelPath));
        writer.writeObject(dependencyLabels);
        writer.writeObject(maps);
        writer.writeObject(options);
        writer.writeObject(shiftFeatureAveragedWeights);
        writer.writeObject(reduceFeatureAveragedWeights);
        writer.writeObject(leftArcFeatureAveragedWeights);
        writer.writeObject(rightArcFeatureAveragedWeights);
        writer.writeInt(dependencySize);
        writer.flush();
        writer.close();
    }
}
