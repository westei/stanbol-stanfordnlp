Apache Stanbol Stanford NLP integration
================

[Stanford NLP](http://www-nlp.stanford.edu/) is a [GPL](http://www.fsf.org/licenses/gpl.html) licensed language analysis tool suite that supports several languages. This project aims to provide a standalone server providing a RESTful API that can than be used by Apache Stanbol for NLP processing of texts.

## Install Stanford NLP Apache Stanbol integration

All dependencies of this project are downloaded from Maven central. Cloning and building the project by using 

    mvm clean install
    
is everything that is required for using the English only server.

### Build with International model support

If you also want the French, Chinese and Arabic models you will need to run

    cd models
    ./download-models.sh
    mvn clean install
    
This will download the various models from the [Stanford NLP](http://www-nlp.stanford.edu/) and move to the resource directories so that they are included in the `at.salzburgresearch.stanbol:stanbol-stanfordnlp-modles` module.


## Running the Stanbol Stanford NLP Server

### Building the Server

A runable jar is created as part of the build process. See the previous section for more information on how to build the project. 

This project includes two runnable jar file:

* An English language only server: 

        server/target/stanfordnlp-server/target/stanbol-stanfordnlp-server-{version}-jar-with-dependencies.jar

* An International Server that supports English, French, Arabic and Chinese (zh-cn)
 The assembled runable JAR for the server is available under

        server-int/target/stanbol-stanfordnlp-server-int-1.1.0-SNAPSHOT-jar-with-dependencies.jar
    

Before running any of the two server you should copy (or symlink) this jar file to an dedicated directory.

### Running the Server

The server supports the following command line parameters

* `-h --help` : Prints an help screen similar to this documentation
* `-p --port {port}`: the port (default 8080)
* `-t --analyser-threads {analyzer-thread}`: The size of the thread pool used for Stanford NLP to analyze texts (default: 10).
* `-c --config-dir`: Path to the directory with the '{lang}.pipeline' configuration files (default: `./config`). If the default is used and the directory does not exist, the configuration is initialized by using the defaults.

__Example__: To following command will start the international server on port 8082

    java -Xmx1g -jar at.salzburgresearch.stanbol.stanbol.enhancer.nlp.stanford.server-int-*-jar-with-dependencies.jar -p 8082

### Customize the configuration

The configuration of the server is read from the config directory (see `-c --config-dir` parameter). By default it will be under `./config`. On the first start this is initialized with the default configuration.

For each supported language a `{lang}.properties` file need to be present in the config directory. For supported languages those files need only to include the definition of the annotation pipeline (e.g. `annotators = tokenize, ssplit, pos, lemma, ner`). This is because the server provides defaults for all other required configurations (such as the NLP models for POS and NER). However the properties files do support all configuration options as described on the [CoreNLP](http://www-nlp.stanford.edu/software/corenlp.shtml) webpage. So if you want to override/change some defaults (e.g. providing your own models) just add the according properties in the `{lang}.properties` file

When adding support for a new language (lets say you do have POS and NER models for Spanish) you need to create a `es.properties` file and configure the language as described on the [CoreNLP](http://www-nlp.stanford.edu/software/corenlp.shtml) webpage.


License(s):
-----------

All modules are dual licensed under the [GPL](http://www.fsf.org/licenses/gpl.html) and the [Apache License Version 2.0](LICENSE).

### Why two licenses

While I am no expert the intension of having two licenses is the following: Executing this code requires to confirm to the more restrictive rules defined by the [GPL](http://www.fsf.org/licenses/gpl.html) as this software does depend to the GPL licensed Stanford NLP libraries. The more permissive Apache License will still allow users to take code snippets or utility classes and do with them what ever they want.

Acknowledgements:
-----------------

Original work on this project was partly funded by [IKS-Project](http://iks-project.eu/) a European Community's Seventh Framework Programme (FP7/2007-2013) under grant agreement n° 231527.

Recent work was funded by [MICO](http://www.mico-project.eu/) also a FP7 project under grant agreement n° 610480

