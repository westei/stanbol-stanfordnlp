StanfordNLP Server
=============

This provides a embedded Jetty running the RESTful services of the [StanfordNLP](http://nlp.stanford.edu/) [Stanbol NLP processing](http://stanbol.apache.org/docs/trunk/components/enhancer/nlp/) integration .

Building the Server
-------------------

First you need to assembly the runable Jar

    mvm clean install
    mvn assembly:single
    
to build the jar with all the dependencies.

If the build succeeds go to the /target directory and copy the

    stanbol-stanfordnlp-server-int-*-jar-with-dependencies.jar

to the directory you would like to run the server.

Running the Server
------------------

The module builds a runable jar. The main has the following parameters

    java -Xmx{size} -jar {jar-file} [options]
        Indexing Commandline Utility:
        size:  Heap requirements depend on the dataset and the configuration.
        1024m should be a reasonable default.
        -c,--config-dir <arg>       Path to the directory with the
                                    '{lang}.pipeline' configuration files
                                    (default:
                                    /Volumes/development/workspace/stanbol-stan
                                    fordnlp/server-int/target/config)
         -h,--help                  display this help and exit
        -p,--port <arg>             The port for the server (default: 8080)
        -t,--analyser-threads <arg> The size of the thread pool used for
                                    Talismane to tokenize and POS tag sentences
                                    (default: 10)

