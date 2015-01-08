package Structures;

import Accessories.Options;
import Learning.AveragedPerceptron;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Mohammad Sadegh Rasooli.
 * ML-NLP Lab, Department of Computer Science, Columbia University
 * Date Created: 1/8/15
 * Time: 11:41 AM
 * To report any bugs or problems contact rasooli@cs.columbia.edu
 */

public class InfStruct {
   public  HashMap<Long, Float>[][] avg;
    public   IndexMaps maps;
    public   ArrayList<Integer> dependencyLabels;
    public   HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet;
    public    Options options;

    public InfStruct(HashMap<Long, Float>[][] avg, IndexMaps maps, ArrayList<Integer> dependencyLabels, HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet, Options options) {
        this.avg = avg;
        this.maps = maps;
        this.dependencyLabels = dependencyLabels;
        this.headDepSet = headDepSet;
        this.options = options;
    }

    public InfStruct(AveragedPerceptron perceptron, IndexMaps maps, ArrayList<Integer> dependencyLabels, HashMap<Integer, HashMap<Integer, HashSet<Integer>>> headDepSet, Options options) {
        avg = new HashMap[perceptron.featureWeights.length][perceptron.featureWeights[0].length];
        for (int i = 0; i < avg.length; i++) {
            for (int j = 0; j < avg[i].length; j++) {
                avg[i][j] = new HashMap<Long, Float>();
                HashMap<Long, Float> map = perceptron.featureWeights[i][j];
                HashMap<Long, Float> avgMap = perceptron.averagedWeights[i][j];
                for (long feat : map.keySet()) {
                    float weight = map.get(feat) - (avgMap.get(feat) / perceptron.iteration);
                    if (weight != 0)
                        (avg[i][j]).put(feat, weight);
                }
            }
        }
        this.maps = maps;
        this.dependencyLabels = dependencyLabels;
        this.headDepSet = headDepSet;
        this.options = options;
    }

    public InfStruct(String modelPath) throws Exception{
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(modelPath));
        dependencyLabels=(ArrayList<Integer>) reader.readObject();
        headDepSet=( HashMap<Integer, HashMap<Integer, HashSet<Integer>>>)    reader.readObject();
        maps=(IndexMaps)reader.readObject();
        options=(Options)reader.readObject();
        avg= (HashMap<Long, Float>[][]) reader.readObject();
    }

    public void saveModel(String modelPath) throws Exception {
        ObjectOutput writer = new ObjectOutputStream(new FileOutputStream(modelPath));
        writer.writeObject(dependencyLabels);
        writer.writeObject(headDepSet);
        writer.writeObject(maps);
        writer.writeObject(options);
        writer.writeObject(avg);
        writer.flush();
        writer.close();
    }
}
