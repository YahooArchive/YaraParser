Yara Parser
===================

&copy; Copyright 2014-2015, Yahoo! Inc.

&copy; Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.


# Yara K-Beam Arc-Eager Dependency Parser

This core functionality of the project is implemented by [Mohammad Sadegh Rasooli](www.cs.columbia.edu:/~rasooli) during his internship in Yahoo! labs and it was later modified in Columbia University. For more details, see the technical details. The parser can be trained on any syntactic dependency treebank with Conll'06 format and can parse sentences afterwards. It can also parse partial sentences (a sentence with partial gold dependencies) and it is also possible to train on partial trees.

__Please cite the following technical report if you use Yara in your research:__

* Mohammad Sadegh Rasooli and Joel Tetreault. [Yara Parser: A Fast and Accurate Dependency Parser](http://arxiv.org/abs/1503.06733). arXiv:1503.06733v2 [cs.CL] Mar 2015.

### Version Log
- V0.2 (10 Feb. 2015) Some problems fixed in search pruning and dependency features, and brown cluster features added; compressed model file saving.
- V0.1 (January 2015) First version of the parser with features roughly the same as Zhang and Nivre (2011).

# WARNING
If you use the extended feature set or brown cluster features, currently the parser supports just 64 unique dependency relations and 1M unique words in the training data. If the number of unique relations in your training data is more than 64, your results with extended or brown cluster features may not be precise! 

## Performance and Speed on WSJ/Penn Treebank
Performance and speed really depends on the quality of POS taggers and machine power and memory. I used [my own pos tagger v0.2](https://github.com/rasoolims/SemiSupervisedPosTagger/releases/tag/v0.2) and tagged the train file with 10-way jackknifing. I got POS accuracy of 97.14, 97.18 and 97.37 in the train, dev and test files respectively. I converted the data to dependencies with [Penn2Malt tool](http://stp.lingfil.uu.se/~nivre/research/Penn2Malt.html). The following tables are the results.


|YaraParser.Parser| Dep. Rep.      |beam| Features     |Iter#| Dev UAS | Test UAS | Test LAS | sen/sec|
|:----:|:---------------|----|:-------------|:---:|:-------:|:--------:|:--------:|:------:|
| ZPar | Penn2Malt      | 64 | [ZN (11)](http://www.sutd.edu.sg/cmsresource/faculty/yuezhang/acl11j.pdf)      | 15  |  93.14  |   92.9   |   91.8   |  29    |
| Yara | Penn2Malt      | 1  | [ZN (11)](http://www.sutd.edu.sg/cmsresource/faculty/yuezhang/acl11j.pdf) (11) basic| 6   |  89.54  |   89.34  |   88.02  | 6000   |
| Yara | Penn2Malt      | 1  | [ZN (11)](http://www.sutd.edu.sg/cmsresource/faculty/yuezhang/acl11j.pdf) (11) + BC | 13  |  89.98  |   89.74  |   88.52  | 1300   |
| Yara | Penn2Malt      | 64 | [ZN (11)](http://www.sutd.edu.sg/cmsresource/faculty/yuezhang/acl11j.pdf) (11)      | 13  |  93.31  |   92.97  |   91.93  |  133   |
| Yara | Penn2Malt      | 64 | [ZN (11)](http://www.sutd.edu.sg/cmsresource/faculty/yuezhang/acl11j.pdf) (11) + BC | 13  |  93.42  |   93.32  |   92.32  |   45   |


## Meaning of Yara
Yara (Yahoo-Rasooli) is a word that means __strength__ , __boldness__, __bravery__ or __something robust__ in Persian. In other languages it has other meanings, e.g. in Amazonian language it means __daughter of the forests__, in Malaysian it means  __the beams of the sun__ and in ancient Egyptian it means __pure__ and __beloved__.

## Compilation

Go to the root directory of the project and run the following command:
	
	javac YaraParser.Parser/YaraParser.java

Then you can test the code via the following command:

    java YaraParser.Parser.YaraParser
    
or
    
    java YaraParser.Parser/YaraParser


## Command Line Options

__NOTE:__ All the examples bellow are using the jar file for running the application but you can also use the java class files after manually compiling the code.

### Train a YaraParser.Parser

__WARNING:__ The training code ignores non-projective trees in the training data. If you want to include them, try to projectivize them; e.g. by [tree projectivizer](http://www.cs.bgu.ac.il/~yoavg/software/projectivize/).

* __java -jar jar/YaraParser.jar train  -train-file [train-file]  -dev [dev-file] -model [model-file]  -punc [punc-file]__
	
	*	 The model for each iteration is with the pattern [model-file]_iter[iter#]; e.g. mode_iter2
	
	* [punc-file]: File contains list of pos tags for punctuations in the treebank, each in one line
	
	*	 Other options (__there are 128 combinations of options and also beam size and thread but the default is the best setting given a big beam (e.g. 64)__)
		*  -cluster [cluster-file] Brown cluster file: at most 4096 clusters are supported by the parser (default: empty)
			 
			 * The format should be the same as https://github.com/percyliang/brown-cluster/blob/master/output.txt 
	 	 
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

* __java -jar jar/YaraParser.jar parse_conll -input [test-file] -out [output-file] -model [model-file]__
	
	* The test file should have the conll 2006 format
	
	* Optional: nt:#_of_threads (default:8) 
	
	* Optional: -score [score file] averaged score of each output parse tree in a file

### Parse a POS Tagged File

* __java -jar jar/YaraParser.jar parse_tagged -input [test-file] -out [output-file] -model [model-file]__
	
	* The test file should have each sentence in line and word_tag pairs are space-delimited
	
	* Optional:  -delim [delim] (default is _)
	
	* Optional: nt:#_of_threads (default:8) 
	
	* Example line: He_PRP is_VBZ nice_AJ ._.
	
### Train a YaraParser.Parser with Partial Training data
There are some occasions that the training data does not always have full trees. In such cases, you can still train a parsing model with CoNLL format but the words that do not have a head, should have ``-1`` as their heads. For the partial trees in the training data, dynamic oracles will be applied, regardless of your choice of using static or dynamic oracles. 

The parser starts parsing only on full trees for 2 iterations and then works on partial trees as well. If you want to change its default (3), use __``pt:#pt``__ (e.g. pt:5) as an argument.

__NOTE:__ If there is a tree in the data that has a cycle or cannot be projectivized at all, the trainer gives a message ``no oracle(sen#)`` and ignores that specific sentence and continues its training procedure. There is no crucial difference in the command line options compared to training a parser on full trees.

__WARNING__ Training on partial trees is noisy because the dynamic oracle decides about what path to choose as a gold tree and in many cases, the choice by the dynamic oracle is not correct, leading to noisy data situation.
	
### Parse a Partial Tree with Some Gold Dependencies
__NOTE:__ There are some occasions where you need to parse a sentence, but already know about some of its dependencies. Yara tries to find the best parse tree for a sentence: 1) if the original partial tree is projective and there is at least one way to fill in the other heads and preserve projectivity, all gold parses will be preserved, 2) if there is some nonprojectivity or loop in the partial tree, some or even all of gold dependencies will be ignored.

__WARNING__ Because of some technical reasons, all words connected to the dummy root word, will be labeled as ``ROOT``. If your treebank convention is different, try to refactor the ``ROOT`` dependency in the final output.


* __java -jar YaraParser.jar parse_partial -input [test-file] -out [output-file] -model [model-file] nt:[#_of_threads (optional -- default:8)]__ 
		
	* The test file should have the conll 2006 format; each word that does not have a parent, should have a -1 parent-index
	
	* Optional: -score [score file] averaged score of each output parse tree in a file


## Evaluate the YaraParser.Parser

__WARNING__ The evaluation script is Yara, takes care of ``ROOT`` output, so you do not have to change anything in the output.

* __java -jar YaraParser.jar eval  -gold [gold-file]  -parse [parsed-file]   -punc [punc-file]__
	* [punc-file]: File contains list of pos tags for punctuations in the treebank, each in one line
	* Both files should have conll 2006 format

### Example Usage
There is small portion from Google Universal Treebank for the German language in the __sample\_data__ directory. 

### Training without Brown Cluster Features

     java -jar jar/YaraParser.jar train  -train-file sample_data/train.conll  -dev sample_data/dev.conll -model /tmp/model iter:10  -punc punc_files/google_universal.puncs

You can kill the process whenever you find that the model performance is converging on the dev data. The parser achieved an unlabeled accuracy __87.52__ and labeled accuracy __81.15__ on the dev set in the 7th iteration. 

Performance numbers are produced after each iteration. The following is the performance on the dev after the 10th iteration:

    1.07 ms for each arc!
	14.18 ms for each sentence!

	Labeled accuracy: 79.93
	Unlabeled accuracy:  87.15
	Labeled exact match:  22.54
	Unlabeled exact match:  45.07  

Next, you can run the developed model on the test data:

     java -jar jar/YaraParser.jar parse_conll -input sample_data/test.conll -model /tmp/model_iter10  -out /tmp/test.output.conll

You can finally evaluate the output data:

	java -jar jar/YaraParser.jar eval  -gold sample_data/test.conll  -parse /tmp/test.output.conll 

    Labeled accuracy: 70.91
	Unlabeled accuracy:  75.97
	Labeled exact match:  17.54
	Unlabeled exact match:  28.07

### Training with Brown Cluster Features
You can apply the same way you did without Brown cluster features but with `` -cluster`` option.

	java -jar jar/YaraParser.jar train  -train-file sample_data/train.conll  -dev sample_data/dev.conll -model /tmp/model iter:10  -punc punc_files/google_universal.puncs  -cluster sample_data/german_clusters_europarl_universal_train.cluster





# API Usage

You can look at the class __Parser/API_UsageExample__ to see an example of using the parser inside your code.

# NOTES

## How to create word clusters?
Given a tokenized raw text file, you can use [Percy Liang's Brown clustering code](https://github.com/percyliang/brown-cluster) to cluster the words. I put the cluster files for English and German but if you think you have a bigger text file for those you can use them as long as the formatting is ok.

## Memory size
For very large training sets, you may need to increase the java memory heap size by -Xmx option; e.g. ``java -Xmx10g jar/YaraParser.jar``.


## Technical Details
This parser is an implementation of the arc-eager dependency model [Nivre, 2004] with averaged structured Perceptron [Collins, 2002]. The feature setting is from Zhang and Nivre [2011] with additional Brown cluster features inspired from Koo et al. [2008] and Honnibal and Johnson [2014]. The model can be trained with early update strategy [Collins and Roark, 2004] or max-violation update [Huang et al., 2012]. Oracle search for training is done with either dynamic oracles [Goldberg and Nivre, 2013] or original static oracles.  Choosing the best oracles in the dynamic oracle can be done via latent structured Perceptron [Sun et al., 2013] and also randomly. The dummy root token can be placed in the end or in the beginning of the sentence [Ballesteros and Nivre, 2013]. When the dummy root token is placed in the beginning, tree constraints are applied [Nivre and Fernández-González, 2014].

##References

__[Ballesteros and Nivre, 2013]__ Ballesteros, Miguel, and Joakim Nivre. "Going to the roots of dependency parsing." Computational Linguistics 39.1 (2013): 5-13.
	

__[Collins, 2002]__ Collins, Michael. "Discriminative training methods for hidden markov models: Theory and experiments with perceptron algorithms." Proceedings of the ACL-02 conference on Empirical methods in natural language processing-Volume 10. Association for Computational Linguistics, 2002.

__[Collins and Roark, 2004]__ Collins, Michael, and Brian Roark. "Incremental parsing with the perceptron algorithm." Proceedings of the 42nd Annual Meeting on Association for Computational Linguistics. Association for Computational Linguistics, 2004.

__[Goldberg and Nivre, 2013]__ Goldberg, Yoav, and Joakim Nivre. "Training Deterministic Parsers with Non-Deterministic Oracles." TACL 1 (2013): 403-414.

__[Honnibal and Johnson]__ Honnibal, Matthew, and Mark Johnson. "Joint incremental disfluency detection and dependency parsing." Transactions of the Association for Computational Linguistics 2 (2014): 131-142.	


__[Huang et al., 20012]__ Huang, Liang, Suphan Fayong, and Yang Guo. "Structured perceptron with inexact search." Proceedings of the 2012 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies. Association for Computational Linguistics, 2012.

__[Koo et al., 2008]__ Koo, Terry, Xavier Carreras, and Michael Collins. "Simple Semi-supervised Dependency Parsing." Proceedings of ACL-08: HLT, pages 595–603, Columbus, Ohio, USA, Association for Computational Linguistics, 2008.

__[Nivre, 2004]__ Nivre, Joakim. "Incrementality in deterministic dependency parsing." In Proceedings of the Workshop on Incremental Parsing: Bringing Engineering and Cognition Together, pp. 50-57. Association for Computational Linguistics, 2004.

__[Nivre and Fernández-González, 2014]__ Nivre, Joakim, and Daniel Fernández-González. "Arc-Eager Parsing with the Tree Constraint." Computational Linguistics, 40(2), (2014): 259-267.


__[Sun et al., 2013]__ Sun, Xu, Takuya Matsuzaki, and Wenjie Li. "Latent structured perceptrons for large-scale learning with hidden information." IEEE Transactions on Knowledge and Data Engineering, 25.9 (2013): 2063-2075.

__[Zhang and Nivre, 2011]__ Zhang, Yue, and Joakim Nivre. "Transition-based dependency parsing with rich non-local features." Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies: short papers-Volume 2. Association for Computational Linguistics, 2011.s, 2011.