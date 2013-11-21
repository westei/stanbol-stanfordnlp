package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.MORPHO_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.NER_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.DEPENDENCY_ANNOTATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelationTag;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.stanford.mappings.TagSetRegistry;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

public class StanfordNlpAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(StanfordNlpAnalyzer.class);

    protected final AnalysedTextFactory analysedTextFactory;
    private final ExecutorService executor;

    private TagSetRegistry tagSetRegistry;

    private final Map<String,AnnotationPipeline> pipelines = new HashMap<String,AnnotationPipeline>();
    private Collection<String> supported = Collections.emptyList();
    
    /**
     * Map of {@link DependencyTreeParser}s by language
     */
    private Map<String, DependencyTreeParser> dependencyTreeParsers = 
        new HashMap<String, DependencyTreeParser>();
    
    public StanfordNlpAnalyzer(ExecutorService executor, AnalysedTextFactory atf) {
        this.executor = executor;
        this.analysedTextFactory = atf == null ? AnalysedTextFactory.getDefaultInstance() : atf;
        this.tagSetRegistry = TagSetRegistry.getInstance();
    }
    /**
     * Sets the {@link AnnotationPipeline} for a language
     * @param language the language
     * @param pipeline the pipeline
     * @return the old pipeline for this language or <code>null</code> if none
     */
    public AnnotationPipeline setPipeline(String language, AnnotationPipeline pipeline){
        if(language == null || language.isEmpty()){
            throw new IllegalArgumentException("The parsed language MUST NOT be NULL nor empty!");
        }
        if(pipeline == null){
            throw new IllegalArgumentException("The parsed annotation pipeline MUST NOT be NULL!");
        }
        AnnotationPipeline old = pipelines.put(language, pipeline);
        if(old == null){
            List<String> supported = new ArrayList<String>(pipelines.keySet());
            Collections.sort(supported);
            this.supported = Collections.unmodifiableCollection(supported);
        } //language was already present ... no need to update supported
        return old;
    }
    
    /**
     * Sets the {@link DependencyTreeParser} for a language
     * @param parser
     */
    public void setDependencyTreeParser(DependencyTreeParser parser) {
        if(parser == null) {
            return;
        }
        
        String language = parser.getLanguage();
        
        if(language == null || language.isEmpty()) {
            throw new IllegalArgumentException("The parsed language MUST NOT be NULL nor empty!");
        }
        
        dependencyTreeParsers.put(language, parser);
    }
    
    /**
     * Checks if the parsed language is supported by this Analyzer
     * @param language the language
     * @return <code>true</code> it texts in the parsed language are supported.
     */
    public boolean isSupported(String language){
        return pipelines.containsKey(language.toLowerCase(Locale.ROOT));
    }
    /**
     * A alphabetical sorted list of supported languages intended to be used
     * for logging.<p>
     * <b>NOTE:</b> This method is not indented to be used to check if a 
     * language is supported. Users should use {@link #isSupported(String)} for
     * that.
     * @return an alphabetical sorted list of supported languages
     */
    public Collection<String> getSupported(){
        return supported;
    }

    public AnalysedText analyse(String lang, Blob blob) throws IOException {
        if(lang == null || lang.isEmpty()){
            throw new IllegalStateException("The parsed Language MUST NOT be NULL nor empty!");
        }
        lang = lang.toLowerCase(Locale.ROOT); //languages are case insensitive
        if(blob == null){
            throw new IllegalStateException("The parsed Blob MUST NOT be NULL!");
        }
        final AnnotationPipeline pipeline = pipelines.get(lang);
        if(pipeline == null){
            throw new IllegalArgumentException("The parsed language '" + lang
                + "'is not supported (supported: " + supported+ ")!");
        }
        
        final DependencyTreeParser dtParser = dependencyTreeParsers.get(lang);
        
        // create an empty Annotation just with the given text
        final AnalysedText at = analysedTextFactory.createAnalysedText(blob);
        TagSet<PosTag> posTagSet = tagSetRegistry.getPosTagSet(lang);
        Map<String,PosTag> adhocPosTags = tagSetRegistry.getAdhocPosTagMap(lang);
        TagSet<NerTag> nerTagSet = tagSetRegistry.getNerTagSet(lang);
        TagSet<GrammaticalRelationTag> gramRelationTagSet = 
            tagSetRegistry.getGrammaticalRelationTagSet(lang);

        // run all Annotators and dependency tree parser on this text
        ParsedData parsedData;
        try { //process the text using the executor service
            parsedData = executor.submit(new ParserCallable(at, pipeline, dtParser)).get(); //and wait for the results
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interupped while processing text",e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new IllegalStateException(cause.getClass().getSimpleName() +
                "Exception while procesing an '"+lang+"' language text (message: "
                + cause.getMessage() + ")!",cause);
        }

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        Annotation document = parsedData.getAnnotation();
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        int sentenceCount = 0;
        
        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            Token sentStart = null;
            Token sentEnd = null;
            Token nerStart = null;
            Token nerEnd = null;
            NerTag nerTag = null;
            int tokenCount = 0;
            
            List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
            Map<Integer, Collection<TypedDependency>> sentenceDependencies = 
                parsedData.getTypedDependencies(++sentenceCount);
            
            for (CoreLabel token : tokens) {
                Token t = at.addToken(token.beginPosition(), token.endPosition());
                // This can be used to ensure that the text indexes are correct
//              String word = token.get(OriginalTextAnnotation.class);
//              String span = t.getSpan();
//              if(!word.equals(span)){
//                  log.warn("word: '{}' != span: '{}'",word,span);
//              }
                if(sentStart == null){
                    sentStart = t;
                }
                sentEnd = t;
                // Process POS annotations
                String pos = token.get(PartOfSpeechAnnotation.class);
                PosTag posTag;
                if(pos != null){
                    posTag = posTagSet != null ? posTagSet.getTag(pos) : null;
                    if(posTag == null){
                        posTag = adhocPosTags.get(pos);
                        if(posTag == null){
                            log.info("Unmapped POS tag '{}' for language {}",pos,lang);
                            posTag = new PosTag(pos);
                            adhocPosTags.put(pos, posTag);
                        }
                    }
                    t.addAnnotation(POS_ANNOTATION, Value.value(posTag));
                } else {
                    posTag = null;
                }
                log.info(" > '{}' pos: {}",t.getSpan(),posTag);
                // Process NER annotations
                String ne = token.get(NamedEntityTagAnnotation.class);
                //NOTE: '0' is used to indicate that the current token is no 
                //      named entities
                NerTag actNerTag = nerTagSet != null && !"O".equals(ne) ? 
                        nerTagSet.getTag(ne) : null;
                if(nerTag != null && !nerTag.equals(actNerTag)){
                    Chunk nerChunk = at.addChunk(nerStart.getStart(), nerEnd.getEnd());
                    nerChunk.addAnnotation(NER_ANNOTATION, Value.value(nerTag));
                    nerTag = null;
                    nerStart = null;
                    nerEnd = null;
                } 
                if(actNerTag != null){
                    if(nerStart == null){
                        nerStart = t;
                    }
                    nerTag = actNerTag;
                    nerEnd = t;
                }
                //Process the Lemma
                String lemma = token.get(LemmaAnnotation.class);
                if(lemma != null && !lemma.equals(t.getSpan())){
                    MorphoFeatures morpho = new MorphoFeatures(lemma);
                    if(posTag != null){
                        morpho.addPos(posTag);
                    }
                    t.addAnnotation(MORPHO_ANNOTATION, Value.value(morpho));
                }
                
                // Add dependency tree annotations
                if (sentenceDependencies != null) {
                    addDependencyRelations(tokens, ++tokenCount, 
                        sentenceDependencies.get(tokenCount), at, gramRelationTagSet, t);
                }  

            } //end iterate over tokens in sentence
            //clean up sentence
            at.addSentence(sentStart.getStart(), sentEnd.getEnd());
            sentStart = null;
            sentEnd = null;
            //we might have still an open NER annotation
            if(nerTag != null){
                Chunk nerChunk = at.addChunk(nerStart.getStart(), nerEnd.getEnd());
                nerChunk.addAnnotation(NER_ANNOTATION, Value.value(nerTag));
            }
        }

        // This is the coreference link graph
        // Each chain stores a set of mentions that link to each other,
        // along with a method for getting the most representative mention
        // Both sentence and token offsets start at 1!
        // Map<Integer, CorefChain> graph =
        // document.get(CorefChainAnnotation.class);

        return at;
    }

    /**
     * Adds dependency relations to the current token
     * 
     * @param tokens
     * @param tokenIdxInSentence - idx for the token which is currently processed
     * @param sentenceDependencies
     * @param at
     * @param relationTagSet
     * @param currentToken
     */
    private void addDependencyRelations(List<CoreLabel> tokens,
            int tokenIdxInSentence,
            Collection<TypedDependency> sentenceDependencies, AnalysedText at,
            TagSet<GrammaticalRelationTag> relationTagSet, Token currentToken) {

        if (sentenceDependencies == null) {
            return;
        }
        
        for (TypedDependency dependency : sentenceDependencies) {
            TreeGraphNode dependent = dependency.dep();
            TreeGraphNode governor = dependency.gov();
            int dependentIndex = dependent.label().index();
            int governorIndex = governor.label().index();
            GrammaticalRelation gramRel = dependency.reln();
            GrammaticalRelationTag relTag = relationTagSet.getTag(gramRel.getShortName());
            
            boolean isDependent = false;
            Span partner = null;
            
            if (governorIndex == tokenIdxInSentence) {
                CoreLabel dependentLabel = tokens.get(dependentIndex - 1);
                Token dependentToken = at.addToken(dependentLabel.beginPosition(),
                    dependentLabel.endPosition());
                partner = dependentToken;
            } else if (dependentIndex == tokenIdxInSentence) {
                isDependent = true;
                
                if (governorIndex != 0) {
                    CoreLabel governorLabel = tokens.get(governorIndex - 1);
                    Token governorToken = at.addToken(governorLabel.beginPosition(),
                        governorLabel.endPosition());
                    partner = governorToken;
                }
            } else {
                // We should not be in this situation but just in case
                continue;
            }
            
            currentToken.addAnnotation(DEPENDENCY_ANNOTATION, 
                Value.value(new DependencyRelation(relTag, isDependent, partner)));
        }
    }
}
