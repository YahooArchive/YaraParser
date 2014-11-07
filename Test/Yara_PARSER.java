package Test;

import Accessories.CoNLLReader;
import Accessories.Options;
import Accessories.Pair;
import Learning.AveragedPerceptron;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.GoldConfiguration;
import TransitionBasedSystem.Parser.KBeamArcEagerParser;
import TransitionBasedSystem.Trainer.ArcEagerBeamTrainer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
Copyright 2014, Yahoo! Inc.
Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
**/
public class Yara_PARSER {
    public static Options processArgs(String[] args){
        Options options=new Options();

        for(int i=0;i<args.length;i++) {
            if (args[i].equals("--help") || args[i].equals("-h") || args[i].equals("-help"))
                options.showHelp = true;
            else if (args[i].equals("train"))
                options.train = true;
            else if (args[i].equals("parse_conll"))
                options.parseConllFile = true;
            else if (args[i].equals("parse_tagged"))
                options.parseTaggedFile = true;
            else if (args[i].startsWith("train-file:") || args[i].startsWith("test-file:"))
                options.inputFile = args[i].substring(args[i].lastIndexOf(":") + 1).trim();
            else if(args[i].startsWith("model-file:"))
                options.modelFile = args[i].substring(args[i].lastIndexOf(":") + 1).trim();
            else if(args[i].startsWith("dev-file:"))
                options.devPath = args[i].substring(args[i].lastIndexOf(":") + 1).trim();
            else if(args[i].startsWith("out:"))
                options.outputFile = args[i].substring(args[i].lastIndexOf(":") + 1).trim();
            else if(args[i].startsWith("inf-file:"))
                options.infFile = args[i].substring(args[i].lastIndexOf(":") + 1).trim();
            else if(args[i].startsWith("beam:"))
                options.beamWidth = Integer.parseInt(args[i].substring(args[i].lastIndexOf(":") + 1));
            else if(args[i].equals("unlabeled"))
                options.labeled =Boolean.parseBoolean(args[i]);
            else if(args[i].equals("lowercase"))
                options.lowercase =Boolean.parseBoolean(args[i]);
            else if(args[i].equals("basic"))
                options.useExtendedFeatures =false;
            else if(args[i].equals("early"))
                options.useMaxViol =false;
            else if(args[i].equals("static"))
                options.useDynamicOracle =false;
            else if(args[i].equals("random"))
                options.useRandomOracleSelection =true;
            else if(args[i].equals("root_first"))
                options.rootFirst =true;
            else if(args[i].startsWith("iter:"))
                options.trainingIter =Integer.parseInt(args[i].substring(args[i].lastIndexOf(":") + 1));

        }

        if(options.train || options.parseTaggedFile || options.parseConllFile)
            options.showHelp=false;

        return options;
    }

    public  static void showHelp(){
        StringBuilder output=new StringBuilder();
        output.append("© Yara Parser \n");
        output.append("\u00a9 Copyright 2014, Yahoo! Inc.\n");
        output.append("\u00a9 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.");
        output.append("http://www.apache.org/licenses/LICENSE-2.0\n");
        output.append("\n");

        output.append("Usage:\n");

        output.append("* Train a parser:\n");
        output.append("\tjava Yara_PARSER train train-file:[train-file] dev-file:[dev-file] model-file:[model-file]\n");
        output.append("\t** The model for each iteration is with the pattern [model-file]_iter[iter#]; e.g. mode_iter2\n");
        output.append("\t** The inf file is [model-file] for parsing\n");
        output.append("\t** Other options\n");
        output.append("\t \t beam:[beam-width] (default:1)\n");
        output.append("\t \t iter:[training-iterations] (default:50)\n");
        output.append("\t \t unlabeled (default: labeled parsing, unless explicitly put `unlabeled')\n");
        output.append("\t \t lowercase (default: case-sensitive words, unless explicitly put 'lowercase')\n");
        output.append("\t \t basic (default: use extended feature set, unless explicitly put 'basic')\n");
        output.append("\t \t early (default: use max violation update, unless explicitly put `early' for early update)\n");
        output.append("\t \t static (default: use dynamic oracles, unless explicitly put `static' for static oracles)\n");
        output.append("\t \t random (default: choose maximum scoring oracle, unless explicitly put `random' for randomly choosing an oracle)\n");
        output.append("\t \t root_first (default: put ROOT in the last position, unless explicitly put 'root_first')\n\n");

        output.append("* Parse a CoNLL'2006 file:\n");
        output.append("\tjava Yara_PARSER parse_conll test-file:[test-file] out:[output-file] inf-file:[inf-file] model-file:[model-file]\n");
        output.append("\t** The test file should have the conll 2006 format\n\n");

        output.append("* Parse a tagged file:\n");
        output.append("\tjava Yara_PARSER parse_tagged test-file:[test-file] out:[output-file] inf-file:[inf-file] model-file:[model-file]\n");
        output.append("\t** The test file should have each sentence in line and word_tag pairs are space-delimited\n");
        output.append("\t \t Example: He_PRP is_VBZ nice_AJ ._.");
        System.out.println(output.toString());
    }

    public static void main(String[] args) throws  Exception{
        Options options=processArgs(args);

        if(options.showHelp){
            showHelp();
        } else if(options.train){
            if(options.devPath.equals("") || options.inputFile.equals("") || options.modelFile.equals(""))
            {
                showHelp();

            }else {
                System.out.println(options);
                CoNLLReader reader = new CoNLLReader(options.inputFile);


                ArrayList<GoldConfiguration> dataSet = reader.readData(Integer.MAX_VALUE, false, options.labeled, options.rootFirst, options.lowercase);
                System.out.println("conll data reading done!");

                ArrayList<String> dependencyLabels = new ArrayList<String>();
                HashMap<String, HashMap<String, HashSet<String>>> headDepSet = new HashMap<String, HashMap<String, HashSet<String>>>();

                for (GoldConfiguration configuration : dataSet) {
                    Sentence sentence = configuration.getSentence();

                    for (int dep : configuration.getGoldDependencies().keySet()) {
                        Pair<Integer, String> headDepPair = configuration.getGoldDependencies().get(dep);
                        if (!dependencyLabels.contains(headDepPair.second))
                            dependencyLabels.add(headDepPair.second);
                        String relation = headDepPair.second;
                        String dependent = sentence.tokenAt(dep).getPOS();
                        String head = sentence.tokenAt(headDepPair.first).getPOS();

                        if (!headDepSet.containsKey(head))
                            headDepSet.put(head, new HashMap<String, HashSet<String>>());
                        if (!headDepSet.get(head).containsKey(dependent))
                            headDepSet.get(head).put(dependent, new HashSet<String>());
                        headDepSet.get(head).get(dependent).add(relation);
                    }
                }

                int featureLength = options.useExtendedFeatures ?72 : 26;

                System.out.println(dataSet.size());

                HashMap<String, Integer> labels = new HashMap<String, Integer>();
                int labIndex = 0;
                labels.put("sh", labIndex++);
                labels.put("rd", labIndex++);
                for (String label : dependencyLabels) {
                    labels.put("ra_" + label, labIndex++);
                    labels.put("la_" + label, labIndex++);
                }

                System.out.print("writing objects....");

                ObjectOutput writer = new ObjectOutputStream(new FileOutputStream(options.modelFile));
                writer.writeObject(dependencyLabels);
                writer.writeObject(headDepSet);
                writer.writeObject(options);
                writer.flush();
                writer.close();
                System.out.println("done!");

                ArcEagerBeamTrainer trainer = new ArcEagerBeamTrainer(options.useMaxViol ? "max_violation" : "early", new AveragedPerceptron(featureLength, labels, 1),
                        options.rootFirst, options.beamWidth, dependencyLabels, headDepSet, featureLength, options.useDynamicOracle, options.useRandomOracleSelection);
                trainer.train(dataSet, options.devPath, options.trainingIter, options.modelFile, options.lowercase);
            }
        } else if (options.parseTaggedFile){
            if(options.outputFile.equals("") || options.inputFile.equals("")
                    || options.infFile.equals("") || options.modelFile.equals(""))
            {
                showHelp();

            }else {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(options.infFile));
                ArrayList<String> dependencyLabels=(ArrayList<String>)reader.readObject();
                HashMap<String, HashMap<String, HashSet<String>>> headDepSet=(HashMap<String, HashMap<String, HashSet<String>>>)reader.readObject();
                Options inf_options=(Options)reader.readObject();
                AveragedPerceptron averagedPerceptron = (AveragedPerceptron) AveragedPerceptron.loadModel(options.modelFile, 1);

                int templates = averagedPerceptron.featureSize();
                KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, templates);

                parser.parseTaggedFile(options.inputFile,
                        options.outputFile, inf_options.rootFirst, inf_options.beamWidth, inf_options.lowercase);
            }
        }else if(options.parseConllFile){
            if(options.outputFile.equals("") || options.inputFile.equals("")
                    || options.infFile.equals("") || options.modelFile.equals(""))
            {
                showHelp();

            }else {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(options.infFile));
                ArrayList<String> dependencyLabels=(ArrayList<String>)reader.readObject();
                HashMap<String, HashMap<String, HashSet<String>>> headDepSet=(HashMap<String, HashMap<String, HashSet<String>>>)reader.readObject();
               Options inf_options=(Options)reader.readObject();
                AveragedPerceptron averagedPerceptron = (AveragedPerceptron) AveragedPerceptron.loadModel(options.modelFile, 1);

               int featureLength = averagedPerceptron.featureSize();
                KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, featureLength);

                parser.parseConllFile(options.inputFile,
                        options.outputFile, inf_options.rootFirst, inf_options.beamWidth, inf_options.lowercase);
            }
        } else{
            showHelp();
        }
    }
}
