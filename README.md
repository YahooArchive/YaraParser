Yara Parser
===================

&copy; Copyright 2014, Yahoo! Inc.

&copy; Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.


# Yara K-Beam Arc-Eager Dependency Parser

This project is implemented by [Mohammad Sadegh Rasooli](www.cs.columbia.edu:/~rasooli) during his internship in Yahoo! labs. For more details, see the technical details. The parser can be trained on any syntactic dependency treebank with Conll'06 format and can parse sentences afterwards. It can also parse partial sentences (a sentence with partial gold dependencies) and it is also possible to train on partial trees.

## Performance and Speed on WSJ/Penn Treebank
__Performance__ really depends on the quality of POS taggers. In academic papers, researchers try n-way jackknifing for training a very optimized POS tagger. I basically used the best off-the-shelf POS tagging model from [Stanford POS tagger](http://nlp.stanford.edu/software/tagger.shtml) (which is not as optimized as doing n-way jackknifing) with [Penn2Malt](http://stp.lingfil.uu.se/~nivre/research/Penn2Malt.html) conversion. The best unlabeled accuracy on the dev file was 93.20 (92.01 labeled, 49.88 unlabeled exact match) and with that model I got 92.71 (91.66 labeled, 47.23 unlabeled exact match) on the test data. All the settings that I used were defaults with 64 beams.

__Speed__ really depends on your machine but this parser is very fast. In my experiments, I used a 24 core machine (Intel(R) Xeon(R) 2.00 GHz) and set number of threads to 16. The most accurate and slowest model (64 beams) can parse 85 sentences per second (average on 20 runs) with 16 threads. Each training iteration takes less than 20 minutes (20 iterations may take up to 12 hours). In the very fastest mode (1 beam, unlabeled, basic features) the parser can parse 5000 sentences per second and each training iteration takes less than 1 minute. In the fastest labeled parsing mode(1 beam, basic features), the parser can parse 2700 sentences per second and each training iteration takes less than 2 minutes.

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

### Train a Parser

__WARNING:__ The training code ignores non-projective trees in the training data. If you want to include them, try to projectivize them; e.g. by [tree projectivizer](http://www.cs.bgu.ac.il/~yoavg/software/projectivize/).

* __java -jar jar/YaraParser.jar train --train-file [train-file] --dev-file [dev-file] --model-file [model-file] --punc_file [punc-file]__
	
	*	 The model for each iteration is with the pattern [model-file]_iter[iter#]; e.g. mode_iter2
	* [punc-file]: File contains list of pos tags for punctuations in the treebank, each in one line
	* The inf file is [model-file] for parsing
	
	*	 Other options (__there are 128 combinations of options and also beam size and thread but the default is the best setting given a big beam (e.g. 64)__)
	 	 
	 	 * beam:[beam-width] (default:64)
	 	 
	 	 * iter:[training-iterations] (default:20)
	 	 
	 	 * unlabeled (default: labeled parsing, unless explicitly put `unlabeled')
	 	 
	 	 * lowercase (default: case-sensitive words, unless explicitly put 'lowercase')
	 	 
	 	 * basic (default: use extended feature set, unless explicitly put 'basic')
	 	 
	 	 *  static (default: use dynamic oracles, unless explicitly put `static' for static oracles)
	 	 
	 	 * early (default: use max violation update, unless explicitly put `early' for early update)
	 	 
	 	 
	 	 * random (default: choose maximum scoring oracle, unless explicitly put `random' for randomly choosing an oracle)
	 	 
	 	 * nt:[#_of_threads] (default:8)
	 	 
	 	 * root_first (default: put ROOT in the last position, unless explicitly put 'root_first')
	 

### Parse a CoNLL_2006 File

__WARNING__ Because of some technical reasons, all words connected to the dummy root word, will be labeled as ``ROOT``. If your treebank convention is different, try to refactor the ``ROOT`` dependency in the final output.

* __java -jar jar/YaraParser.jar parse_conll --test-file [test-file] --out [output-file] --inf-file [inf-file] --model-file [model-file]__
	
	* The inf file is [model-file] for parsing (used in the testing phase)
	
	* The test file should have the conll 2006 format

### Parse a POS Tagged File

__WARNING__ Because of some technical reasons, all words connected to the dummy root word, will be labeled as ``ROOT``. If your treebank convention is different, try to refactor the ``ROOT`` dependency in the final output.


* __java -jar jar/YaraParser.jar parse_tagged --test-file [test-file] --out [output-file] --inf-file [inf-file] --model-file [model-file]__
	
	* The test file should have each sentence in line and word_tag pairs are space-delimited
	
	* Optional:  --delim [delim] (default is _)
	
	* The inf file is [model-file] for parsing (used in the testing phase)
	 
	* Example line: He_PRP is_VBZ nice_AJ ._.
	
### Train a Parser with Partial Training data
There are some occasions that the training data does not always have full trees. In such cases, you can still train a parsing model with CoNLL format but the words that do not have a head, should have ``-1`` as their heads. For the partial trees in the training data, dynamic oracles will be applied, regardless of your choice of using static or dynamic oracles. 

The parser starts parsing only on full trees for 2 iterations and then works on partial trees as well. If you want to change its default (3), use __``pt:#pt``__ (e.g. pt:5) as an argument.

__NOTE:__ If there is a tree in the data that has a cycle or cannot be projectivized at all, the trainer gives a message ``no oracle(sen#)`` and ignores that specific sentence and continues its training procedure. There is no crucial difference in the command line options compared to training a parser on full trees.

__WARNING__ Training on partial trees is noisy because the dynamic oracle decides about what path to choose as a gold tree and in many cases, the choice by the dynamic oracle is not correct, leading to noisy data situation.
	
### Parse a Partial Tree with Some Gold Dependencies
__NOTE:__ There are some occasions where you need to parse a sentence, but already know about some of its dependencies. Yara tries to find the best parse tree for a sentence: 1) if the original partial tree is projective and there is at least one way to fill in the other heads and preserve projectivity, all gold parses will be preserved, 2) if there is some nonprojectivity or loop in the partial tree, some or even all of gold dependencies will be ignored.

__WARNING__ Because of some technical reasons, all words connected to the dummy root word, will be labeled as ``ROOT``. If your treebank convention is different, try to refactor the ``ROOT`` dependency in the final output.


* __java -jar YaraParser.jar parse_partial --test-file [test-file] --out [output-file] --inf-file [inf-file] --model-file [model-file] nt:[#_of_threads (optional -- default:8)]__ 
		
	* The inf file is [model-file] for parsing (used in the testing phase)
	
	* The test file should have the conll 2006 format; each word that does not have a parent, should have a -1 parent-index


## Evaluate the Parser

__WARNING__ The evaluation script is Yara, takes care of ``ROOT`` output, so you do not have to change anything in the output.

* __java -jar YaraParser.jar eval --gold-file [gold-file] --parsed-file [parsed-file]  --punc_file [punc-file]__
	* [punc-file]: File contains list of pos tags for punctuations in the treebank, each in one line
	* Both files should have conll 2006 format

### Example Usage
There is small portion from Google Universal Treebank for the German language in the __sample\_data__ directory. 

     java -jar jar/YaraParser.jar train --train-file sample_data/train.conll --dev-file sample_data/dev.conll --model-file /tmp/model beam:64 iter:10 --punc_file punc_files/google_universal.puncs

You can kill the process whenever you find that the model performance is converging on the dev data. The parser achieved an unlabeled accuracy __87.27__ and labeled accuracy __80.54__ on the dev set in the 10th iteration. 

Performance numbers are produced after each iteration. The following is the performance on the dev after the 10th iteration:

    2.35 ms for each arc!
	31.17 ms for each sentence!

	Labeled accuracy: 80.54
	Unlabeled accuracy:  87.27
	Labeled exact match:  29.58
	Unlabeled exact match:  45.07  

Next, you can run the developed model on the test data:

     java -jar jar/YaraParser.jar parse_conll --test-file sample_data/test.conll --model-file /tmp/model_iter10 --inf-file /tmp/model --out /tmp/test.output.conll

You can finally evaluate the output data:

	java -jar jar/YaraParser.jar eval --gold-file sample_data/test.conll --parsed-file /tmp/test.output.conll --inf-file /tmp/model 

    Labeled accuracy: 71.02
	Unlabeled accuracy:  76.56
	Labeled exact match:  19.30
	Unlabeled exact match:  29.82

# API Usage

You can look at the class __Parser/API_UsageExample__ to see an example of using the parser inside your code.

# NOTES

## A Useful Trick to Improve Performance
It is shown that replacing all numbers (integers and floating points), with a dummy token (e.g. \<num\>) helps the accuracy in _some treebanks but not always_. If you want to try that idea, simply replace the numbers in your training, development and test data with that dummy token.

## Memory size
For very large training sets, you may need to increase the java memory heap size by -Xmx option; e.g. ``java -Xmx3g jar/YaraParser.jar``. For WSJ data, 4g is more than enough.


## Technical Details
This parser is an implementation of the arc-eager dependency model [Nivre, 2004] with averaged structured Perceptron [Collins, 2002]. The feature setting is from Zhang and Nivre [2011]. The model can be trained with early update strategy [Collins and Roark, 2004] or max-violation update [Huang et al., 2012]. Oracle search for training is done with either dynamic oracles [Goldberg and Nivre, 2013] or original static oracles.  Choosing the best oracles in the dynamic oracle can be done via latent structured Perceptron [Sun et al., 2013] and also randomly. The dummy root token can be placed in the end or in the beginning of the sentence [Ballesteros and Nivre, 2013]. When the dummy root token is placed in the beginning, tree constraints are applied [Nivre and Fernández-González, 2014].

__[References]__

__[Ballesteros and Nivre, 2013]__ Ballesteros, Miguel, and Joakim Nivre. "Going to the roots of dependency parsing." Computational Linguistics 39.1 (2013): 5-13.
	

__[Collins, 2002]__ Collins, Michael. "Discriminative training methods for hidden markov models: Theory and experiments with perceptron algorithms." Proceedings of the ACL-02 conference on Empirical methods in natural language processing-Volume 10. Association for Computational Linguistics, 2002.

__[Collins and Roark, 2004]__ Collins, Michael, and Brian Roark. "Incremental parsing with the perceptron algorithm." Proceedings of the 42nd Annual Meeting on Association for Computational Linguistics. Association for Computational Linguistics, 2004.

__[Goldberg and Nivre, 2013]__ Goldberg, Yoav, and Joakim Nivre. "Training Deterministic Parsers with Non-Deterministic Oracles." TACL 1 (2013): 403-414.

__[Huang et al., 20012]__ Huang, Liang, Suphan Fayong, and Yang Guo. "Structured perceptron with inexact search." Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies. Association for Computational Linguistics, 2012.

__[Nivre, 2004]__ Nivre, Joakim. "Incrementality in deterministic dependency parsing." In Proceedings of the Workshop on Incremental Parsing: Bringing Engineering and Cognition Together, pp. 50-57. Association for Computational Linguistics, 2004.

__[Nivre and Fernández-González, 2014]__ Nivre, Joakim, and Daniel Fernández-González. "Arc-Eager Parsing with the Tree Constraint." Computational Linguistics, 40(2), (2014): 259-267.


__[Sun et al., 2013]__ Sun, Xu, Takuya Matsuzaki, and Wenjie Li. "Latent structured perceptrons for large-scale learning with hidden information." IEEE Transactions on Knowledge and Data Engineering, 25.9 (2013): 2063-2075.

__[Zhang and Nivre, 2011]__ Zhang, Yue, and Joakim Nivre. "Transition-based dependency parsing with rich non-local features." Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies: short papers-Volume 2. Association for Computational Linguistics, 2011. non-local features." Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies: short papers-Volume 2. Association for Computational Linguistics, 2011.