package Accessories;

import java.io.Serializable;

/**
 Copyright 2014, Yahoo! Inc.
 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 **/
public class Options implements Serializable {
    public boolean train;
    public boolean parseTaggedFile;
    public boolean parseConllFile;
    public int beamWidth;
    public boolean rootFirst;
    public boolean showHelp;
    public boolean labeled;
    public  String inputFile;
    public  String outputFile;
    public String devPath;
    public int trainingIter;

    public String infFile;
    public String modelFile;
    public boolean lowercase;
    public boolean useExtendedFeatures;
    public boolean useMaxViol;
    public boolean useDynamicOracle;
    public boolean useRandomOracleSelection;

    public Options(){
        showHelp=true;
        train=false;
        parseConllFile=false;
        parseTaggedFile=false;
        beamWidth=1;
        rootFirst=false;
        infFile ="";
        modelFile ="";
        outputFile="";
        inputFile="";
        devPath="";
        labeled=true;
        lowercase=false;
        useExtendedFeatures=true;
        useMaxViol=true;
        useDynamicOracle=true;
        useRandomOracleSelection=false;
        trainingIter=20;
    }

    public String toString(){
        if(train){
            StringBuilder builder=new StringBuilder();
            builder.append("train file: "+inputFile+"\n");
            builder.append("dev file: "+devPath+"\n");
            builder.append("model/inf file: "+modelFile+"\n");
            builder.append("beam width: "+beamWidth+"\n");
            builder.append("rootFirst: "+rootFirst+"\n");
            builder.append("labeled: "+labeled+"\n");
            builder.append("lower-case: "+lowercase+"\n");
            builder.append("extended features: "+useExtendedFeatures+"\n");
            builder.append("updateModel: "+(useMaxViol?"max violation":"early")+"\n");
            builder.append("oracle: "+(useDynamicOracle?"dynamic":"static")+"\n");
            if(useDynamicOracle)
                builder.append("oracle selection: "+(!useRandomOracleSelection?"latent max":"random")+"\n");

            builder.append("training-iterations: "+trainingIter+"\n");
            return builder.toString();
        }else if(parseConllFile){
            StringBuilder builder=new StringBuilder();
            builder.append("parse model: conll"+"\n");
            builder.append("input file: "+inputFile+"\n");
            builder.append("output file: "+outputFile+"\n");
            builder.append("model file: "+modelFile+"\n");
            builder.append("inf file: "+infFile+"\n");
            return builder.toString();
        } else if(parseTaggedFile){
            StringBuilder builder=new StringBuilder();
            builder.append("parse model: tag file"+"\n");
            builder.append("input file: "+inputFile+"\n");
            builder.append("output file: "+outputFile+"\n");
            builder.append("model file: "+modelFile+"\n");
            builder.append("inf file: "+infFile+"\n");
            return builder.toString();
        }
        return  "";
    }

}
