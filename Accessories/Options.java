/**
 * Copyright 2014, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package Accessories;

import java.io.Serializable;

public class Options implements Serializable {
    public boolean train;
    public boolean parseTaggedFile;
    public boolean parseConllFile;
    public int beamWidth;
    public boolean rootFirst;
    public boolean showHelp;
    public boolean labeled;
    public String inputFile;
    public String outputFile;
    public String devPath;
    public int trainingIter;
    public boolean evaluate;

    public String infFile;
    public String modelFile;
    public boolean lowercase;
    public boolean useExtendedFeatures;
    public boolean useMaxViol;
    public boolean useDynamicOracle;
    public boolean useRandomOracleSelection;
    public String separator;

    public String goldFile;
    public String predFile;

    public Options() {
        showHelp = false;
        train = false;
        parseConllFile = false;
        parseTaggedFile = false;
        beamWidth = 1;
        rootFirst = false;
        infFile = "";
        modelFile = "";
        outputFile = "";
        inputFile = "";
        devPath = "";
        separator="_";
        labeled = true;
        lowercase = false;
        useExtendedFeatures = true;
        useMaxViol = true;
        useDynamicOracle = true;
        useRandomOracleSelection = false;
        trainingIter = 20;
        evaluate=false;
    }

    public String toString() {
        if (train) {
            StringBuilder builder = new StringBuilder();
            builder.append("train file: " + inputFile + "\n");
            builder.append("dev file: " + devPath + "\n");
            builder.append("model/inf file: " + modelFile + "\n");
            builder.append("beam width: " + beamWidth + "\n");
            builder.append("rootFirst: " + rootFirst + "\n");
            builder.append("labeled: " + labeled + "\n");
            builder.append("lower-case: " + lowercase + "\n");
            builder.append("extended features: " + useExtendedFeatures + "\n");
            builder.append("updateModel: " + (useMaxViol ? "max violation" : "early") + "\n");
            builder.append("oracle: " + (useDynamicOracle ? "dynamic" : "static") + "\n");
            if (useDynamicOracle)
                builder.append("oracle selection: " + (!useRandomOracleSelection ? "latent max" : "random") + "\n");

            builder.append("training-iterations: " + trainingIter + "\n");
            return builder.toString();
        } else if (parseConllFile) {
            StringBuilder builder = new StringBuilder();
            builder.append("parse conll" + "\n");
            builder.append("input file: " + inputFile + "\n");
            builder.append("output file: " + outputFile + "\n");
            builder.append("model file: " + modelFile + "\n");
            builder.append("inf file: " + infFile + "\n");
            return builder.toString();
        } else if (parseTaggedFile) {
            StringBuilder builder = new StringBuilder();
            builder.append("parse  tag file" + "\n");
            builder.append("input file: " + inputFile + "\n");
            builder.append("output file: " + outputFile + "\n");
            builder.append("model file: " + modelFile + "\n");
            builder.append("inf file: " + infFile + "\n");
            return builder.toString();
        } else if(evaluate){
            StringBuilder builder = new StringBuilder();
            builder.append("Evaluate" + "\n");
            builder.append("gold file: " + goldFile + "\n");
            builder.append("parsed file: " + predFile + "\n");
            return builder.toString();
        }
        return "";
    }

    public static void showHelp() {
        StringBuilder output = new StringBuilder();
        output.append("© Yara Parser \n");
        output.append("\u00a9 Copyright 2014, Yahoo! Inc.\n");
        output.append("\u00a9 Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.");
        output.append("http://www.apache.org/licenses/LICENSE-2.0\n");
        output.append("\n");

        output.append("Usage:\n");

        output.append("* Train a parser:\n");
        output.append("\tjava -jar YaraParser.jar train --train-file [train-file] --dev-file [dev-file] --model-file [model-file]\n");
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
        output.append("\tjava -jar YaraParser.jar parse_conll --test-file [test-file] --out [output-file] --inf-file [inf-file] --model-file [model-file]\n");
        output.append("\t** The inf file is [model-file] for parsing (used in the testing phase)\n");
        output.append("\t** The test file should have the conll 2006 format\n\n");

        output.append("* Parse a tagged file:\n");
        output.append("\tjava -jar YaraParser.jar parse_tagged --test-file [test-file] --out [output-file] --inf-file [inf-file] --model-file [model-file]\n");
        output.append("\t** The test file should have each sentence in line and word_tag pairs are space-delimited\n");
        output.append("\t** Optional:  --delim [delim] (default is _)\n");
        output.append("\t** The inf file is [model-file] for parsing (used in the testing phase)\n");
        output.append("\t \t Example: He_PRP is_VBZ nice_AJ ._.\n\n");

        output.append("* Evaluate a Conll file:\n");
        output.append("\tjava -jar YaraParser.jar eval --gold-file [gold-file] --parsed-file [parsed-file]\n");
        output.append("\t** Both files should have conll 2006 format\n");
        System.out.println(output.toString());
    }

    public static Options processArgs(String[] args) {
        Options options = new Options();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help") || args[i].equals("-h") || args[i].equals("-help"))
                options.showHelp = true;
            else if (args[i].equals("train"))
                options.train = true;
            else if (args[i].equals("parse_conll"))
                options.parseConllFile = true;
            else if (args[i].equals("eval"))
                options.evaluate = true;
            else if (args[i].equals("parse_tagged"))
                options.parseTaggedFile = true;
            else if (args[i].equals("--train-file") || args[i].equals("--test-file"))
                options.inputFile = args[i+1];
            else if (args[i].equals("--model-file"))
                options.modelFile = args[i+1];
            else if (args[i].startsWith("--dev-file"))
                options.devPath  = args[i+1];
            else if (args[i].equals("--gold-file"))
                options.goldFile = args[i+1];
            else if (args[i].startsWith("--parsed-file"))
                options.predFile  = args[i+1];
            else if (args[i].startsWith("--out"))
                options.outputFile =  args[i+1];
            else if (args[i].startsWith("--inf-file"))
                options.infFile =   args[i+1];
            else if (args[i].startsWith("--delim"))
                options.separator =   args[i+1];
            else if (args[i].startsWith("beam:"))
                options.beamWidth = Integer.parseInt(args[i].substring(args[i].lastIndexOf(":") + 1));
            else if (args[i].equals("unlabeled"))
                options.labeled = Boolean.parseBoolean(args[i]);
            else if (args[i].equals("lowercase"))
                options.lowercase = Boolean.parseBoolean(args[i]);
            else if (args[i].equals("basic"))
                options.useExtendedFeatures = false;
            else if (args[i].equals("early"))
                options.useMaxViol = false;
            else if (args[i].equals("static"))
                options.useDynamicOracle = false;
            else if (args[i].equals("random"))
                options.useRandomOracleSelection = true;
            else if (args[i].equals("root_first"))
                options.rootFirst = true;
            else if (args[i].startsWith("iter:"))
                options.trainingIter = Integer.parseInt(args[i].substring(args[i].lastIndexOf(":") + 1));
        }

        if (options.train || options.parseTaggedFile || options.parseConllFile)
            options.showHelp = false;

        return options;
    }


}
