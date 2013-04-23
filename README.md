Apache Stanbol Stanford NLP integration
================

[Stanford NLP](http://www-nlp.stanford.edu/) is a [GPL](http://www.fsf.org/licenses/gpl.html) licensed language analysis tool suite that supports several languages. This project aims to provide a standalone server providing a RESTful API that can than be used by Apache Stanbol for NLP processing of texts.

## Install Stanford NLP Apache Stanbol integration

All dependencies of this project are downloaded from Maven central. Cloning and building the project by using 

    mvm clean install
    
is everything that is required

## Running the Stanbol Stanford NLP Server

### Building the Server

A runable jar is created as part of the build process. To build the project call

    mvm clean install
    
in the root directory. The assembled runable JAR for the server is available under

    stanfordnlp-server/target/at.salzburgresearch.stanbol.stanbol.enhancer.nlp.stanford.server-1.0.0-SNAPSHOT-jar-with-dependencies.jar

Before running the server you should copy this jar file to an dedicated directory.

### Running the Server

The server supports the following command line parameters

* `-h --help` : Prints an help screen similar to this documentation
* `-p --port {port}`: the port (default 8080)
* `-t --analyser-threads {analyzer-thread}`: The size of the thread pool used for Stanford NLP to analyze texts (default: 10).
* `-c --config-dir`: Path to the directory with the '{lang}.pipeline' configuration files (default: `./config`). If the default is used and the directory does not exist, the configuration is initialized by using the defaults.

__Example__: To following command will start the server on port 8082

    java -Xmx1g -jar at.salzburgresearch.stanbol.stanbol.enhancer.nlp.stanford.server-1.0.0-SNAPSHOT-jar-with-dependencies.jar -p 8082


License(s):
-----------

All modules are dual licensed under the [GPL](http://www.fsf.org/licenses/gpl.html) and the [Apache License Version 2.0](LICENSE).

### Why two licenses

While I am no expert the intension of having two licenses is the following: Executing this code requires to confirm to the more restrictive rules defined by the [GPL](http://www.fsf.org/licenses/gpl.html) as this software does depend to the GPL licensed Stanford NLP libraries. The more permissive Apache License will still allow users to take code snippets or utility classes and do with them what ever they want.