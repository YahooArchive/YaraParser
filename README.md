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
	
	javac Parser/YaraParser.java

Then you can test the code via the following command:

    java Parser.YaraParser
    
or
    
    java Parser/YaraParser


## Command Line Options

__NOTE:__ All the examples bellow are using the jar file for running the application but you can also use the java class files after manually compiling the code.

### Train a parser

__WARNING:__ The training code ignores non-projective trees in the training data. If you want to include them, try to projectivize them; e.g. by [tree projectivizer](http://www.cs.bgu.ac.il/~yoavg/software/projectivize/).

* __java -jar jar/YaraParser.jar train --train-file [train-file] --dev-file [dev-file] --model-file [model-file]__
	
	*	 The model for each iteration is with the pattern [model-file]_iter[iter#]; e.g. mode_iter2
	
	* The inf file is [model-file] for parsing
	
	*	 Other options
	 	 
	 	 * beam:[beam-width] (default:1)
	 	 
	 	 * iter:[training-iterations] (default:50)
	 	 
	 	 * unlabeled (default: labeled parsing, unless explicitly put `unlabeled')
	 	 
	 	 * lowercase (default: case-sensitive words, unless explicitly put 'lowercase')
	 	 
	 	 * basic (default: use extended feature set, unless explicitly put 'basic')
	 	 
	 	 * early (default: use max violation update, unless explicitly put `early' for early update)
	 	 
	 	 
	 	 * random (default: choose maximum scoring oracle, unless explicitly put `random' for randomly choosing an oracle)
	 

### Parse a CoNLL_2006 file
* __java -jar jar/YaraParser.jar parse_conll --test-file [test-file] --out [output-file] --inf-file [inf-file] --model-file [model-file]__
	
	* The inf file is [model-file] for parsing (used in the testing phase)
	
	* The test file should have the conll 2006 format

### Parse a POS tagged file:
* __java -jar jar/YaraParser.jar parse_tagged --test-file [test-file] --out [output-file] --inf-file [inf-file] --model-file [model-file]__
	
	* The test file should have each sentence in line and word_tag pairs are space-delimited
	
	* Optional:  --delim [delim] (default is _)
	
	* The inf file is [model-file] for parsing (used in the testing phase)
	 
	* Example line: He_PRP is_VBZ nice_AJ ._.

## Evaluate the Parser
__WARNING__ The current evaluation script does take into account every dependency relation. If you want to ignore the punctuations, you have to either tweak the code or write your own script.

* __java -jar jar/YaraParser.jar eval --gold-file [gold-file] --parsed-file [parsed-file] --inf-file [inf-file]__
	
	* Both files should have conll 2006 format

### Example Usage
There is small portion from Google Universal Treebank for the German language in the __sample\_data__ directory. 

     java -jar jar/YaraParser.jar train --train-file sample_data/train.conll --dev-file sample_data/dev.conll --model-file /tmp/model beam:64 iter:10

You can kill the process whenever you find that the model performance is converging on the dev data. The parser achieved an unlabeled accuracy __88.95__ and labeled accuracy __83.00__ on the dev set in the 10th iteration. 

Performance numbers are produced after each iteration. The following is the performance on the dev after the 10th iteration:

    3.54 ms for each arc!
	46.87 ms for each sentence!

	Labeled accuracy: 83.00
	Unlabeled accuracy:  88.95
	Labeled exact match:  23.94
	Unlabeled exact match:  42.25  

Next, you can run the developed model on the test data:

     java -jar jar/YaraParser.jar parse_conll --test-file sample_data/test.conll --model-file /tmp/model_iter10 --inf-file /tmp/model --out /tmp/test.output.conll

You can finally evaluate the output data:

	java -jar jar/YaraParser.jar eval --gold-file sample_data/test.conll --parsed-file /tmp/test.output.conll --inf-file /tmp/model 

    Labeled accuracy: 68.86
	Unlabeled accuracy:  73.90
	Labeled exact match:  19.30
	Unlabeled exact match:  24.56  

# API USAGE

You can look at the class __Parser/API_UsageExample__ to see an example of using the parser inside your code.

# NOTES

## A Useful Trick to Improve Performance
It is shown that replacing all numbers (integers and floating points), with a dummy token (e.g. <num>) helps the accuracy in _some treebanks but not always_. If you want to try that idea, simply replace the numbers in your training, development and test data with that dummy token.

## Memory size
For very large training sets, you may need to increase the java memory heap size by -Xmx option; e.g. java -Xmx3g jar/YaraParser.jar ...


## Technical Details
This parser is an implementation of the arc-eager dependency model [Nivre, 2004] with averaged structured Perceptron [Collins, 2002]. The feature setting is from Zhang and Nivre [2011]. The model can be trained with early update strategery [Collins and Roark, 2004] or max-violation update [Huang et al., 2012]. Oracle search for training is done with dynamic oracles [Goldberg and Nivre, 2013]. Choosing the best oracles in the dynamic oracle can be done via latent structured Perceptron [Sun et al., 2013]. 

__[References]__


__[Collins, 2002]__ Collins, Michael. "Discriminative training methods for hidden markov models: Theory and experiments with perceptron algorithms." Proceedings of the ACL-02 conference on Empirical methods in natural language processing-Volume 10. Association for Computational Linguistics, 2002.

__[Collins and Roark, 2004]__ Collins, Michael, and Brian Roark. "Incremental parsing with the perceptron algorithm." Proceedings of the 42nd Annual Meeting on Association for Computational Linguistics. Association for Computational Linguistics, 2004.

__[Goldberg and Nivre, 2013]__ Goldberg, Yoav, and Joakim Nivre. "Training Deterministic Parsers with Non-Deterministic Oracles." TACL 1 (2013): 403-414.

__[Huang et al., 20012]__ Huang, Liang, Suphan Fayong, and Yang Guo. "Structured perceptron with inexact search." Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies. Association for Computational Linguistics, 2012.

__[Nivre, 2004]__ Nivre, Joakim. "Incrementality in deterministic dependency parsing." In Proceedings of the Workshop on Incremental Parsing: Bringing Engineering and Cognition Together, pp. 50-57. Association for Computational Linguistics, 2004.

__[Sun et al., 2013]__ Sun, Xu, Takuya Matsuzaki, and Wenjie Li. "Latent structured perceptrons for large-scale learning with hidden information." IEEE Transactions on Knowledge and Data Engineering, 25.9 (2013): 2063-2075.

__[Zhang and Nivre, 2011]__ Zhang, Yue, and Joakim Nivre. "Transition-based dependency parsing with rich non-local features." Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies: short papers-Volume 2. Association for Computational Linguistics, 2011.