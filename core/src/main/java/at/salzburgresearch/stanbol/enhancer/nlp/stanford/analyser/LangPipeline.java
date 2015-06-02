package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser;

import static edu.stanford.nlp.pipeline.Annotator.STANFORD_SENTIMENT;
import static edu.stanford.nlp.pipeline.StanfordCoreNLP.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.enhancer.nlp.stanford.segment.ArabicSegmentorAnnotator;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.impl.StanbolDeterministicCorefAnnotator;
import edu.stanford.nlp.ie.NERClassifierCombiner;
import edu.stanford.nlp.ie.regexp.NumberSequenceClassifier;
import edu.stanford.nlp.ie.regexp.RegexNERSequenceClassifier;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.AnnotatorFactory;
import edu.stanford.nlp.pipeline.AnnotatorPool;
import edu.stanford.nlp.pipeline.CharniakParserAnnotator;
import edu.stanford.nlp.pipeline.ChineseSegmenterAnnotator;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.pipeline.DeterministicCorefAnnotator;
import edu.stanford.nlp.pipeline.MorphaAnnotator;
import edu.stanford.nlp.pipeline.NERCombinerAnnotator;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.PTBTokenizerAnnotator;
import edu.stanford.nlp.pipeline.ParserAnnotator;
import edu.stanford.nlp.pipeline.RegexNERAnnotator;
import edu.stanford.nlp.pipeline.SentimentAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.WhitespaceTokenizerAnnotator;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.PropertiesUtils;

/**
 * Extends the Stanford NLP {@link AnnotationPipeline} to allow multiple
 * language specific pipelines. This works around the singelton {@link AnnotatorPool}
 * used by the {@link StanfordCoreNLP} implementation that would replace multiple
 * {@link Annotator}s of the same type using different configurations (e.g.
 * modles for different languages).
 * <p>
 * NOTE: That the different {@link AnnotatorFactory} implementations are taken from
 * anonymous classes within {@link StanfordCoreNLP}. Those might need to be
 * adapted after an version upgrade.
 * 
 * @author Rupert Westenthaler
 *
 */
public class LangPipeline extends AnnotationPipeline {

    private static final String STANFORD_SEGMENT = "segment";

    private final Logger log = LoggerFactory.getLogger(LangPipeline.class);
    
    /**
     * Empty properties file used for parsing to the {@link AnnotatorFactory}
     * constructor. 
     */
    protected static final Properties DUMMY_PROPERTIES = new Properties();
    
    private class SegmentorFactory extends AnnotatorFactory {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;

        public SegmentorFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }

        @Override
        public Annotator create() {
            if(language.startsWith("zh")){
                return new ChineseSegmenterAnnotator(STANFORD_SEGMENT, properties);
            } else if(language.startsWith("ar")){
                return new ArabicSegmentorAnnotator(STANFORD_SEGMENT, properties);
            } else {
                throw new IllegalStateException("Segmentor Factory only supports Chinese (zh) "
                    + "and Arabic (ar) but current language is '"+language+"'!");
            }
        }

        @Override
        public String signature() {
            return ""; //not used
        }

    }
    
    private class TokenizerFactory extends AnnotatorFactory {

        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;

        public TokenizerFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }
        
        @Override
        public Annotator create() {
          if (Boolean.valueOf(properties.getProperty("tokenize.whitespace", "false"))) {
            return new WhitespaceTokenizerAnnotator(properties);
          } else {
            String options = properties.getProperty("tokenize.options",
                    PTBTokenizerAnnotator.DEFAULT_OPTIONS);
            boolean keepNewline =
                    Boolean.valueOf(properties.getProperty(NEWLINE_SPLITTER_PROPERTY,
                            "false"));
            // If the user specifies "tokenizeNLs=false" in tokenize.options, then this default will
            // be overridden.
            if (keepNewline) {
              options = "tokenizeNLs," + options;
            }
            return new PTBTokenizerAnnotator(false, options);
          }
        }

        @Override
        public String signature() {
            return "";
        }
    };
    
    private class SentenceSplitterFactory extends AnnotatorFactory {
        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;

        public SentenceSplitterFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }
        
        @Override
        public Annotator create() {
            boolean nlSplitting = Boolean.valueOf(properties.getProperty(NEWLINE_SPLITTER_PROPERTY, "false"));
            if (nlSplitting) {
              boolean whitespaceTokenization = Boolean.valueOf(properties.getProperty("tokenize.whitespace", "false"));
              if (whitespaceTokenization) {
                if (System.getProperty("line.separator").equals("\n")) {
                  return WordsToSentencesAnnotator.newlineSplitter(false, "\n");
                } else {
                  // throw "\n" in just in case files use that instead of
                  // the system separator
                  return WordsToSentencesAnnotator.newlineSplitter(false, System.getProperty("line.separator"), "\n");
                }
              } else {
                return WordsToSentencesAnnotator.newlineSplitter(false, PTBTokenizer.getNewlineToken());
              }

            } else {
              // Treat as one sentence: You get a no-op sentence splitter that always returns all tokens as one sentence.
              String isOneSentence = properties.getProperty("ssplit.isOneSentence");
              if (Boolean.parseBoolean(isOneSentence)) { // this method treats null as false
                return WordsToSentencesAnnotator.nonSplitter(false);
              }

              // multi token sentence boundaries
              String boundaryMultiTokenRegex = properties.getProperty("ssplit.boundaryMultiTokenRegex");

              // Discard these tokens without marking them as sentence boundaries
              String tokenPatternsToDiscardProp = properties.getProperty("ssplit.tokenPatternsToDiscard");
              Set<String> tokenRegexesToDiscard = null;
              if (tokenPatternsToDiscardProp != null){
                String [] toks = tokenPatternsToDiscardProp.split(",");
                tokenRegexesToDiscard = Generics.newHashSet(Arrays.asList(toks));
              }
              // regular boundaries
              String boundaryTokenRegex = properties.getProperty("ssplit.boundaryTokenRegex");
              Set<String> boundariesToDiscard = null;

              // newline boundaries which are discarded.
              String bounds = properties.getProperty("ssplit.boundariesToDiscard");
              if (bounds != null) {
                String [] toks = bounds.split(",");
                boundariesToDiscard = Generics.newHashSet(Arrays.asList(toks));
              }
              Set<String> htmlElementsToDiscard = null;
              // HTML boundaries which are discarded
              bounds = properties.getProperty("ssplit.htmlBoundariesToDiscard");
              if (bounds != null) {
                String [] elements = bounds.split(",");
                htmlElementsToDiscard = Generics.newHashSet(Arrays.asList(elements));
              }
              String nlsb = properties.getProperty("ssplit.newlineIsSentenceBreak", "two");

              return new WordsToSentencesAnnotator(false, boundaryTokenRegex, boundariesToDiscard, htmlElementsToDiscard,
                      nlsb, boundaryMultiTokenRegex, tokenRegexesToDiscard);
            }
        }

        @Override
        public String signature() {
            return "";
        }
    }
    
    private class PosTaggerFactory extends AnnotatorFactory {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;
        
        public PosTaggerFactory(Properties properties){
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }
        
        @Override
        public Annotator create() {
            return new POSTaggerAnnotator("pos",properties);
        }

        @Override
        public String signature() {
            return  "";
        }
    }
    
    private class LemmatizerFactory extends AnnotatorFactory {

        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;
        
        public LemmatizerFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }
        @Override
        public Annotator create() {
          return new MorphaAnnotator(false);
        }

        @Override
        public String signature() {
            return "";
        }
        
    }
    
    private class NerFactory extends AnnotatorFactory {
        
        private static final long serialVersionUID = 1L;

        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;

        public NerFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }

        @Override
        public Annotator create() {
          List<String> models = new ArrayList<String>();
          String modelNames = properties.getProperty("ner.model");
          if (modelNames == null) {
            modelNames = DefaultPaths.DEFAULT_NER_THREECLASS_MODEL + "," + DefaultPaths.DEFAULT_NER_MUC_MODEL + "," + DefaultPaths.DEFAULT_NER_CONLL_MODEL;
          }
          if (modelNames.length() > 0) {
            models.addAll(Arrays.asList(modelNames.split(",")));
          }
          if (models.isEmpty()) {
            // Allow for no real NER model - can just use numeric classifiers or SUTime
            // Will have to explicitly unset ner.model.3class, ner.model.7class, ner.model.MISCclass
            // So unlikely that people got here by accident
            System.err.println("WARNING: no NER models specified");
          }
          NERClassifierCombiner nerCombiner;
          try {
            boolean applyNumericClassifiers =
              PropertiesUtils.getBool(properties,
                  NERClassifierCombiner.APPLY_NUMERIC_CLASSIFIERS_PROPERTY,
                  NERClassifierCombiner.APPLY_NUMERIC_CLASSIFIERS_DEFAULT);
            boolean useSUTime =
              PropertiesUtils.getBool(properties,
                  NumberSequenceClassifier.USE_SUTIME_PROPERTY,
                  NumberSequenceClassifier.USE_SUTIME_DEFAULT);
            nerCombiner = new NERClassifierCombiner(applyNumericClassifiers,
                  useSUTime, properties,
                  models.toArray(new String[models.size()]));
          } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
          }
          return new NERCombinerAnnotator(nerCombiner, false);
        }

        @Override
        public String signature() {
            return "";
        }
    }
    
    private class RegexNerFactory extends AnnotatorFactory {

        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;

        public RegexNerFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }
        
        @Override
        public Annotator create() {
          String mapping = properties.getProperty("regexner.mapping", DefaultPaths.DEFAULT_REGEXNER_RULES);
          String ignoreCase = properties.getProperty("regexner.ignorecase", "false");
          String validPosPattern = properties.getProperty("regexner.validpospattern", RegexNERSequenceClassifier.DEFAULT_VALID_POS);
          return new RegexNERAnnotator(mapping, Boolean.valueOf(ignoreCase), validPosPattern);
        }

        @Override
        public String signature() {
            return "";
        }
    }
    
    private class PhraseDetectorFactory extends AnnotatorFactory {

        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;

        public PhraseDetectorFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }
        
        @Override
        public Annotator create() {
          String parserType = properties.getProperty("parse.type", "stanford");
          String maxLenStr = properties.getProperty("parse.maxlen");

          if (parserType.equalsIgnoreCase("stanford")) {
            ParserAnnotator anno = new ParserAnnotator("parse", properties);
            return anno;
          } else if (parserType.equalsIgnoreCase("charniak")) {
            String model = properties.getProperty("parse.model");
            String parserExecutable = properties.getProperty("parse.executable");
            if (model == null || parserExecutable == null) {
              throw new RuntimeException("Both parse.model and parse.executable properties must be specified if parse.type=charniak");
            }
            int maxLen = 399;
            if (maxLenStr != null) {
              maxLen = Integer.parseInt(maxLenStr);
            }

            CharniakParserAnnotator anno = new CharniakParserAnnotator(model, parserExecutable, false, maxLen);

            return anno;
          } else {
            throw new RuntimeException("Unknown parser type: " + parserType + " (currently supported: stanford and charniak)");
          }
        }

        @Override
        public String signature() {
            return "";
        }
    }

    private class DeterministicCorefFactory extends AnnotatorFactory {
        private static final long serialVersionUID = 1L;
        
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;

        public DeterministicCorefFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }
        
        @Override
        public Annotator create() {
          return new StanbolDeterministicCorefAnnotator(properties);
        }

        @Override
        public String signature() {
            return "";
        }
    }
    
    private class SentimentFactory extends AnnotatorFactory {
        private static final long serialVersionUID = 1L;
        /**
         * the properties.<p>
         * <b>NOTE:</b> Con not use protected field in super class, because is
         * copies the parsed properties and therefore looses the defaults (parent
         * properties)
         */
        private final Properties properties;
        public SentimentFactory(Properties properties) {
            super(DUMMY_PROPERTIES); //Not used
            this.properties = properties;
        }

        @Override
        public Annotator create() {
          return new SentimentAnnotator(STANFORD_SENTIMENT, properties);
        }

        @Override
        public String signature() {
            return "";
        }
    }
    
    private final AnnotatorPool pool = new AnnotatorPool();

    private String language;
    
    public LangPipeline(String config) {
        super();
        if(config == null || !config.endsWith("pipeline")){
            throw new IllegalArgumentException("Annotation Pipeline configurations "
                    + "MUST follow the '{lang}.pipeline' file name pattern (name : "
                    + config + ")");
        }
        this.language = FilenameUtils.getBaseName(config).toLowerCase();
        if(language == null || language.isEmpty()){
            throw new IllegalArgumentException("Annotation Pipeline configurations "
                    + "MUST follow the '{lang}.config' file name pattern (name : "
                    + config + ")");
        } else {
            log.info("Init Annotation Pipeline for Language {} (config: {})", language, config);
        }
        Properties properties = new Properties(LanguageDefaults.getInstance().getDefaults(language));
        File configFile = new File(FilenameUtils.separatorsToSystem(config));
        if(configFile.isFile()){
            InputStream in = null;
            try {
                in = new FileInputStream(configFile);
                log.info("   ... from File {}", configFile.getAbsolutePath());
                properties.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load properties for language '"
                    + language + "form file '"+configFile.getAbsolutePath()+"'!",e);
            } finally {
                IOUtils.closeQuietly(in);
            }
        } else { //load via classpath
            String configResource = FilenameUtils.separatorsToUnix(config);
            InputStream in = LangPipeline.class.getClassLoader().getResourceAsStream(configResource);
            if(in == null){
                throw new IllegalArgumentException("Unable to load parsed config '"
                    +config+"' from the file System or from the Classpath!");
            } else {
                log.info("   ... via Classpath Resource: {}", configFile.getAbsolutePath());
                try {
                    properties.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to load properties for language '"
                        + language + "form file '"+configFile.getAbsolutePath()+"'!",e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        }
        log.info("   ... successfully loaded config for language {}",language);
        //we need to init all factories
        initFactories(properties);
        //but only instantiate annotators mentioned in the pipeline
        initPipeline(properties);
        log.info("   ... successfully initialised annotation pipeline for language {}",language);
    }

    public LangPipeline(String language, Properties properties) {
        super();
        if(language == null || language.isEmpty()){
            throw new IllegalArgumentException("The parsed language MUST NOT be NULL nor empty!");
        }
        if(properties == null){
            throw new IllegalArgumentException("The parsed configuration must not be NULL!");
        }
        this.language = language;
        //we need to init all factories
        initFactories(properties);
        //but only instantiate annotators mentioned in the pipeline
        initPipeline(properties);
    }

    /**
     * @param properties
     */
    private void initPipeline(Properties properties) {
        log.info("reading annotation pipeline {}",properties.getProperty("annotators"));
        Annotator parseAnnotator = null;
        
        for(String name : properties.getProperty("annotators","").split("[, \t]+")){
            Annotator annotator = pool.get(name.trim());
            if(annotator == null){
                throw new IllegalArgumentException("Annotator '"+name+"' is not "
                    + "not supported!");
            }
            
            if (name.equals(STANFORD_PARSE)) {
                parseAnnotator = annotator;
            } else if (name.equals(STANFORD_DETERMINISTIC_COREF)) {
                if (parseAnnotator == null) {
                    throw new IllegalArgumentException(STANFORD_DETERMINISTIC_COREF + " annotator needs the " + STANFORD_PARSE 
                        + " annotator to be defined beforehand.");
                }
                ((StanbolDeterministicCorefAnnotator)annotator).setParserAnnotator(parseAnnotator);
            }
            
            this.addAnnotator(annotator);
        }
    }

    /**
     * @param properties
     */
    private void initFactories(Properties properties) {
        pool.register(STANFORD_TOKENIZE, new TokenizerFactory(properties));
        pool.register(STANFORD_SSPLIT, new SentenceSplitterFactory(properties));
        pool.register(STANFORD_POS, new PosTaggerFactory(properties));
        pool.register(STANFORD_PARSE, new PhraseDetectorFactory(properties));
        pool.register(STANFORD_NER, new NerFactory(properties));
        pool.register(STANFORD_REGEXNER, new RegexNerFactory(properties));
        pool.register(STANFORD_LEMMA, new LemmatizerFactory(properties));
        pool.register(STANFORD_SEGMENT, new SegmentorFactory(properties));
        pool.register(STANFORD_DETERMINISTIC_COREF, new DeterministicCorefFactory(properties));
        pool.register(STANFORD_SENTIMENT, new SentimentFactory(properties));
    }

    public String getLanguage() {
        return language;
    }       
}
