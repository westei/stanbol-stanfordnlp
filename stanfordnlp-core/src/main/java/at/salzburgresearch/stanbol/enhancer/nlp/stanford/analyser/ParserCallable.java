package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

/**
 * Class which processes a given text using the annotation pipeline and dependency tree parser.
 * 
 * @author Cristian Petroaca
 * 
 */
class ParserCallable implements Callable<ParsedData> {
    private AnnotationPipeline pipeline;
    private DependencyTreeParser dtParser;
    private AnalysedText at;

    public ParserCallable(AnalysedText at, AnnotationPipeline pipeline, DependencyTreeParser dtParser) {
        this.pipeline = pipeline;
        this.dtParser = dtParser;
        this.at = at;
    }

    @Override
    public ParsedData call() throws Exception {
        Annotation document = new Annotation(this.at.getSpan());
        this.pipeline.annotate(document);

        if (this.dtParser == null) {
            return new ParsedData(document, null);
        }

        /*
         * Here we parse each sentence using the dependency tree parser. For each sentence we get a list of
         * typed dependencies. Each typed dependency contains a dependent and a governor and the grammatical
         * relation between them. We create a map between each sentence index in the text and another map
         * containing for each token index in the sentence a list of typed dependencies in which it is either
         * a dependent or a governor. This is done so that the StanfordNlpAnalyser does not go thorugh the
         * same list of dependencies for each token in a sentence.
         */
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        Map<Integer,Map<Integer,Collection<TypedDependency>>> textDependencies = 
                new HashMap<Integer,Map<Integer,Collection<TypedDependency>>>();
        int sentenceIdx = 0;

        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
            List<String> wordList = toStringList(tokens);

            Collection<TypedDependency> sentenceDependencies = this.dtParser.parse(wordList);
            Map<Integer,Collection<TypedDependency>> mappedSentenceDependencies = 
                    new HashMap<Integer,Collection<TypedDependency>>();

            for (TypedDependency dependency : sentenceDependencies) {
                TreeGraphNode dependent = dependency.dep();
                TreeGraphNode governor = dependency.gov();
                int dependentIndex = dependent.label().index();
                int governorIndex = governor.label().index();

                Collection<TypedDependency> mappedDependencyList = mappedSentenceDependencies
                        .get(dependentIndex);
                if (mappedDependencyList == null) {
                    mappedDependencyList = new ArrayList<TypedDependency>();
                    mappedSentenceDependencies.put(dependentIndex, mappedDependencyList);
                }
                mappedDependencyList.add(dependency);

                mappedDependencyList = mappedSentenceDependencies.get(governorIndex);
                if (mappedDependencyList == null) {
                    mappedDependencyList = new ArrayList<TypedDependency>();
                    mappedSentenceDependencies.put(governorIndex, mappedDependencyList);
                }
                mappedDependencyList.add(dependency);
            }

            textDependencies.put(++sentenceIdx, mappedSentenceDependencies);
        }

        return new ParsedData(document, textDependencies);
    }

    private List<String> toStringList(List<CoreLabel> tokens) {
        List<String> tokensStringList = new ArrayList<String>();

        for (CoreLabel label : tokens) {
            tokensStringList.add(label.originalText());
        }

        return tokensStringList;
    }
}
