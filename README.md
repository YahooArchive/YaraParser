Yara Parser
===================

&copy; Copyright 2014, Yahoo! Inc.

&copy; Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.


# Yara K-Beam Arc-Eager Dependency Parser

This project is implemented by [Mohammad Sadegh Rasooli](www.cs.columbia.edu:/~rasooli) during his internship in Yahoo! labs. For more details, see the technical details.

## Meaning of Yara
Yara (Yahoo-Rasooli) is a word that means __strength__ , __boldness__, __bravery__ or __something robust__ in Persian. In other languages it has other meanings, e.g. in Amazonian language it means __daughter of the forests__, in Malaysian it means  __the beams of the sun__ and in ancient Egyptian it means __pure__ and __beloved__.

## Compilation

Go to the root directory of the project and run the following command:
	
	javac Test/YaraParser.java

Then you can test the code via the following command:

    java Test.YaraParser
    
or
    
    java Test/YaraParser


## Command Line Options

__NOTE:__ All the examples bellow are using the jar file for running the application but you can also use the java class files after manullay compiling the code.

### Train a parser

__WARNING:__ The training code ignores non-projective trees in the training data. If you want to include them, try to projectivize them; e.g. by [tree projectivizer](http://www.cs.bgu.ac.il/~yoavg/software/projectivize/).

* java -jar YaraParser.jar train train-file:[train-file] dev-file:[dev-file] model-file:[model-file]

	    * In training mode, the parser takes as input CoNLL 2006 format 
	    
	    * If you do not use the dev-file option, the trainer just trains and it may be difficult to optimize the number of iterations.
	
	    * Other options
	
	 	    * beam:[beam-width] default:1
	 	 
	 	    * iter:[training-iterations] default:20
	 	 
	 	    * unlabeled (default: labeled parsing, unless explicitly put _unlabeled_)
	 	 
	 	    * lowercase (default: case-sensitive words, unless explicitly put _lowercase_)
	 	 
	 	    * basic (default: use extended feature set, unless explicitly put _basic_)
	 	 
	 	    * early (default: use max violation update, unless explicitly put _early_ for early update)
	 	 
	 	    * static (default: use dynamic oracles, unless explicitly put _static_ for static oracles)
	 	 
	 	    * random (default: choose maximum scoring oracle, unless explicitly put _random_ for randomly choosing an oracle)
	 	
	 	    * root_first (default: put ROOT in the last position, unless explicitly put _root_first_)

### Parse a CoNLL_2006 file
* java -jar YaraParser.jar parse_conll test-file:[test-file] out:[output-file] inf-file:[inf-file] model-file:[model-file]

	* The test file should have the conll 2006 format

	* The model for each iteration is with the pattern [model-file]_iter[iter#]; e.g. mode_iter2
	
	* The inf file is [model-file] for parsing (used in the testing phase)

### Parse a POS tagged file:
* java -jar YaraParser.jar parse_tagged test-file:[test-file] out:[output-file] inf-file:[inf-file] model-file:[model-file]
	* The test file should have each sentence in line and word_tag pairs are space-delimited
		
			Example: He_PRP is_VBZ nice_AJ ._.


### Using the JAR file
Use the following command to use the jar file (instead of class files):
    
    java -jar jar/YaraParser.jar

### Example Usage
There is small portion from Google Universal Treebank for the German language in the __sample\_data__ directory. 

     java -jar jar/YaraParser.jar train train-file:sample_data/train.conll dev-file:sample_data/dev.conll model-file:/tmp/model beam:64 iter:10

You can kill the process whenever you find that the model performance is converging on the dev data. The parser achieved an unlabeled accuracy __86.54__ and labeled accuracy __78.34__ on the dev set in the 3rd iteration. 

Performance numbers are produced after each iteration in the following format:  The first line shows labeled accuracy, second line is te unlabeled accuracy, third line is the time for processing each word (in milliseconds) and the forth line is the time for processing each sentence (in milliseconds).

    Labeled accuracy is  78.33537331701346
    Unlabeled accuracy is  86.53610771113831
    8.802937576499389 for each arc!
    101.29577464788733 for each sentence!

Next, you can run the developed model on the test data, in this case, we are using the default parser settings:

     java -jar jar/YaraParser.jar parse_conll test-file:sample_data/test.conll model-file:/tmp/model_iter3 inf-file:/tmp/model out:/tmp/test.output.conll

This will produce the following result (note that the sample data size is very small, hence the low performance numbers):

    Labeled accuracy is  70.20023557126031
    Unlabeled accuracy is  75.85394581861013
    8.759717314487633 for each arc!
    130.47368421052633 for each sentence!

# API USAGE

#### Importing libraries

```java
import Accessories.Options;
import Learning.AveragedPerceptron;
import Structures.Sentence;
import TransitionBasedSystem.Configuration.Configuration;
import TransitionBasedSystem.Parser.KBeamArcEagerParser;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
```
#### Loading the parser

```java
    // loading inf file
        ObjectInputStream reader = new ObjectInputStream(new FileInputStream(infFile));
        ArrayList<String> dependencyLabels=(ArrayList<String>)reader.readObject();
        HashMap<String, HashMap<String, HashSet<String>>> headDepSet=(HashMap<String, HashMap<String, HashSet<String>>>)reader.readObject();
        Options options=(Options)reader.readObject();

        // loading model to an averaged Perceptron classifier
        AveragedPerceptron averagedPerceptron = (AveragedPerceptron) AveragedPerceptron.loadModel(modelFile, 1);

        // loading the parser
        KBeamArcEagerParser parser = new KBeamArcEagerParser(averagedPerceptron, dependencyLabels, headDepSet, averagedPerceptron.featureSize());


```

#### Parsing a sentence

```java
 if(options.rootFirst){
            String[] words={"I","am","here","."};
            String[] tags={"PRON","VERB","ADP","."};
        Configuration bestParse= parser.parse(new Sentence(words, tags), options.rootFirst, options.beamWidth);

            for(int i=0;i<words.length;i++){
              System.out.println(words[i]+"\t"+tags[i]+"\t"+bestParse.state.getHead(i+1)+"\t"+bestParse.state.getDependency(i+1));
            }
        }else{
            String[] words={"I","am","here",".","ROOT"};
            String[] tags={"PRON","VERB","ADP",".","ROOT"};
            Configuration bestParse= parser.parse(new Sentence(words, tags), options.rootFirst, options.beamWidth);
            for(int i=0;i<words.length-1;i++){
                int head=bestParse.state.getHead(i+1);
                if(head==words.length)
                    head=0;
                System.out.println(words[i]+"\t"+tags[i]+"\t"+head+"\t"+bestParse.state.getDependency(i+1));
            }
        }
```

# OTHER USEFUL NOTES

## A Useful Trick to Improve Performance
It is shown that replacing all numbers (integers and floating points), with a dummy token (e.g. <num>) helps the accuracy in some treebanks. If you want to try that idea, simply replace the numbers in your training, development and test data with that dummy token.

## Memory size
For very large training sets, you may need to increase the java memory heap size. For training on the Ontonotes training corpus (105k sentences), we found setting the heap size to -Xmx10G was enough for training and -Xmx7G for parsing.  For training on the standard WSJ set, the default memory was sufficient.  

## Performance 

We tested the Yara Parser on a few datasets to serve as benchmarks.  Using the standard WSJ train / dev / test splits, with gold-standard tags and a beam size of 1 and 20 iterations, yielded a performance of 89.61 labeled accuracy (LAS) and 91.10 unlabeled accuracy (UAS).  Repeating the experiment but with tags from the Stanford tagger yielded a performance of 87.43 LAS and 89.74 UAS.  Finally, we used the train / dev / test splits from ontonotes5, POS tagged and converted using ClearNLP, and trained with a beam of 1 and 20 iterations.  This experiment yielded a performance of 87.35 LAS and 89.25 LAS.


## Technical Details
This parser is an implementation of the arc-eager dependency model [Nivre, 2004] with averaged structured Perceptron [Collins, 2002]. The feature setting is from Zhang and Nivre [2011]. The model can be trained with early update strategery [Collins and Roark, 2004] or max-violation update [Huang et al., 2012]. Oracle search for training can be done either by static oracles or dynamic oracles [Goldberg and Nivre, 2013]. Choosing the best oracles in the dynamic oracle can be done via latent structured Perceptron [Sun et al., 2013]. As in [Ballesteros and Nivre, 2013], the dummy root token can be either in the zeroth or last position of the sentence.

__[References]__

__[Ballesteros and Nivre, 2013]__ Ballesteros, Miguel, and Joakim Nivre. "Going to the roots of dependency parsing." Computational Linguistics 39.1 (2013): 5-13.

__[Collins, 2002]__ Collins, Michael. "Discriminative training methods for hidden markov models: Theory and experiments with perceptron algorithms." Proceedings of the ACL-02 conference on Empirical methods in natural language processing-Volume 10. Association for Computational Linguistics, 2002.

__[Collins and Roark, 2004]__ Collins, Michael, and Brian Roark. "Incremental parsing with the perceptron algorithm." Proceedings of the 42nd Annual Meeting on Association for Computational Linguistics. Association for Computational Linguistics, 2004.

__[Goldberg and Nivre, 2013]__ Goldberg, Yoav, and Joakim Nivre. "Training Deterministic Parsers with Non-Deterministic Oracles." TACL 1 (2013): 403-414.

__[Huang et al., 20012]__ Huang, Liang, Suphan Fayong, and Yang Guo. "Structured perceptron with inexact search." Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies. Association for Computational Linguistics, 2012.

__[Nivre, 2004]__ Nivre, Joakim. "Incrementality in deterministic dependency parsing." In Proceedings of the Workshop on Incremental Parsing: Bringing Engineering and Cognition Together, pp. 50-57. Association for Computational Linguistics, 2004.

__[Sun et al., 2013]__ Sun, Xu, Takuya Matsuzaki, and Wenjie Li. "Latent structured perceptrons for large-scale learning with hidden information." IEEE Transactions on Knowledge and Data Engineering, 25.9 (2013): 2063-2075.

__[Zhang and Nivre, 2011]__ Zhang, Yue, and Joakim Nivre. "Transition-based dependency parsing with rich non-local features." Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies: short papers-Volume 2. Association for Computational Linguistics, 2011.
