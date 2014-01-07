package at.salzburgresearch.enhancer.nlp.stanford.segment;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.Timing;

public class ArabicSegmentorAnnotator implements Annotator {

    private final Logger log = LoggerFactory.getLogger(ArabicSegmentorAnnotator.class);

    private Timing timer = new Timing();
    private static long millisecondsAnnotating = 0;
    private boolean VERBOSE = true;

    CRFClassifier<CoreLabel> classifier;

    public ArabicSegmentorAnnotator(String name, Properties props) {
        // We are only interested in {name}.* properties
        String prefix = name + '.';
        String model = null;
        Properties segProps = new Properties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                // skip past name and the subsequent "."
                String modelKey = key.substring(prefix.length());
                if (modelKey.equals("model")) {
                    model = props.getProperty(key);
                } else {
                    segProps.setProperty(modelKey, props.getProperty(key));
                }
            }
        }
        this.VERBOSE = PropertiesUtils.getBool(props, name + ".verbose", true);
        init(model, segProps);

    }

    /**
     * @param segProps
     */
    private void init(String model, Properties segProps) {
        if (VERBOSE) {
            timer.start();
            System.err.print("Loading Segmentation Model [" + model + "]...");
        }
        try {
            classifier = (CRFClassifier<CoreLabel>) CRFClassifier.getClassifier(model, segProps);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (VERBOSE) {
            timer.stop("done.");
        }
    }

    @Override
    public void annotate(Annotation annotation) {
        if (VERBOSE) {
            timer.start();
            System.err.print("Adding Segmentation annotation...");
        }
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences != null) {
            for (CoreMap sentence : sentences) {
                segmentSentence(sentence);
            }
        } else {
            segmentSentence(annotation);
        }
        if (VERBOSE) {
            millisecondsAnnotating += timer.stop("done.");
            // System.err.println("output: "+l+"\n");
        }

    }

    private void segmentSentence(CoreMap annotation) {
        String text = annotation.get(CoreAnnotations.TextAnnotation.class);
        log.debug("Input: \n\n{}", text);
        List<CoreLabel> originalTokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
        if(originalTokens == null){
            throw new IllegalStateException("Unable to Segment Arabic Text because "
                + "no Tokens are available! Please make sure that a Tokenizer is present "
                + "in the configured Annotation Pipeline!");
        }
        List<CoreLabel> labeledCharList = IOBUtils.StringToIOB(originalTokens, null);
        labeledCharList = classifier.classify(labeledCharList);
        if(log.isDebugEnabled()){
            debugLabeldCharList(labeledCharList, IOBUtils.StringToIOB(originalTokens, null));
        }
        List<CoreLabel> words = IOBUtils.IOBToToken(labeledCharList);
        annotation.set(CoreAnnotations.TokensAnnotation.class, words);

        // StringBuilder sb = new StringBuilder();
        // Integer startIndex = null;
        // Integer lastIndex = null;
        // String lastLabel = "";
        // //create words from the chars
        // int sequenceLength = labeledCharList.size();
        // for (int i = 0; i < sequenceLength; ++i) {
        // CoreLabel labeledChar = labeledCharList.get(i);
        // String token = labeledChar.get(CoreAnnotations.CharAnnotation.class);
        // String label = labeledChar.get(CoreAnnotations.AnswerAnnotation.class);
        // Integer index = labeledChar.get(CoreAnnotations.IndexAnnotation.class);
        // if (label.equals(BeginSymbol)) {
        // if (lastLabel.equals(ContinuationSymbol) || lastLabel.equals(BeginSymbol)) {
        // //TORO: new Word starts
        // String word = sb.toString();
        // Integer start = startIndex;
        // Integer end = lastIndex;
        // startIndex = index;
        // }
        // sb.append(token);
        //
        // } else if (label.equals(ContinuationSymbol)) {
        // sb.append(token);
        //
        // } else if (label.equals(NosegSymbol)) {
        // if ( ! lastLabel.equals(BoundarySymbol)) {
        // sb.append(" ");
        // }
        // sb.append(token);
        //
        // } else if (label.equals(BoundarySymbol)) {
        // sb.append(" ");
        //
        // } else if (label.equals(RewriteTahSymbol)) {
        // sb.append("ة ");
        // } else if (label.equals(RewriteTareefSymbol)) {
        // sb.append(" ال");
        // } else {
        // throw new RuntimeException("Unknown label: " + label);
        // }
        // lastLabel = label;
        // lastIndex = index;
        // }
    }

    /**
     * @param labeledCharList
     * @param labeledCharList2
     */
    private void debugLabeldCharList(List<CoreLabel> labeledCharList, List<CoreLabel> labeledCharList2) {
        for(int i = 0; i < labeledCharList.size() || i < labeledCharList2.size(); i++ ){
            String char1 = null;
            String label1 = null;
            if(labeledCharList.size() > i){
                char1 = labeledCharList.get(i).get(CoreAnnotations.CharAnnotation.class);
                label1 = labeledCharList.get(i).get(CoreAnnotations.AnswerAnnotation.class);
            }
            String char2 = null;
            String label2 = null;
            if(labeledCharList2.size() > i){
                char2 = labeledCharList2.get(i).get(CoreAnnotations.CharAnnotation.class);
                label2 = labeledCharList2.get(i).get(CoreAnnotations.AnswerAnnotation.class);
            }
            log.debug("{}|{} <=> {}|{}", new Object[]{char1,label1,char2,label2});
        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(TOKENIZE_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requires() {
        return Collections.emptySet();
    }

}
