package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser;

import java.util.Collection;
import java.util.Map;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * Data resulted from parsing a given text with the Stanford NLP tool.
 * 
 * @author Cristian Petroaca
 * 
 */
class ParsedData {
    /**
     * Annotated document
     */
    private Annotation document;

    /**
     * Contains the dependency tree relations. Each sentence index has a map with key = token index and value
     * = list of dependency relations of that token.
     */
    private Map<Integer,Map<Integer,Collection<TypedDependency>>> dependencies;

    public ParsedData(Annotation document, Map<Integer,Map<Integer, Collection<TypedDependency>>> dependencies) {
        this.document = document;
        this.dependencies = dependencies;
    }

    public Annotation getAnnotation() {
        return this.document;
    }

    public Map<Integer,Collection<TypedDependency>> getTypedDependencies(int sentenceIdx) {
        if (this.dependencies != null) {
            return this.dependencies.get(sentenceIdx);
        }

        return null;
    }
}
