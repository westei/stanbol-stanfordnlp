package at.salzburgresearch.enhancer.nlp.stanford.segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.international.arabic.ArabicMorphoFeatureSpecification;
import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.international.morph.MorphoFeatureSpecification;
import edu.stanford.nlp.international.morph.MorphoFeatureSpecification.MorphoFeatureType;
import edu.stanford.nlp.international.morph.MorphoFeatures;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EndIndexAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.util.Generics;

/**
 * A class for converting strings to input suitable for processing by and IOB sequence model.
 * <p>
 * Based on the code of the e.s.nlp.international.arabic.process.IOBUtils this
 * class (1) ensures that String indexes are preserved and (2) provides a
 * utility method that recreates a list of {@link CoreLabel} representing the
 * results of the Segmentation process.
 * <p>
 * NOTE that this class does not currently not work with the {@link ArabicTokenizer}
 * as it does not correctly set {@link CoreAnnotations.CharacterOffsetBeginAnnotation}
 * and {@link CoreAnnotations.CharacterOffsetEndAnnotation} as well as
 * {@link CoreAnnotations.BeforeAnnotation} and {@link CoreAnnotations.AfterAnnotation}.
 * Those Annotations are however expected by this utility!
 * 
 * @author Spence Green (original IOBUtils)
 * @author Rupert Westenthaler
 */
public class IOBUtils {

    private static Logger log = LoggerFactory.getLogger(IOBUtils.class); 
    
    // Training token types.
    private enum TokenType {
        BeginMarker,
        EndMarker,
        BothMarker,
        NoMarker
    }

    // Label inventory
    private static final String BeginSymbol = "BEGIN";
    private static final String ContinuationSymbol = "CONT";
    private static final String NosegSymbol = "NOSEG";
    private static final String BoundarySymbol = ".##.";
    private static final String BoundaryChar = ".#.";
    private static final String RewriteTahSymbol = "REWTA";
    private static final String RewriteTareefSymbol = "REWAL";

    // Patterns for tokens that should not be segmented.
    private static final Pattern isPunc = Pattern.compile("\\p{Punct}+");
    private static final Pattern isDigit = Pattern.compile("\\p{Digit}+");
    private static final Pattern notUnicodeArabic = Pattern.compile("\\P{InArabic}+");

    // The set of clitics segmented in the ATBv3 training set (see the annotation guidelines).
    // We need this list for tagging the clitics when reconstructing the segmented sequences.
    private static final Set<String> arAffixSet;
    static {
        String arabicAffixString = "ل ف و ما ه ها هم هن نا كم تن تم ى ي هما ك ب م س";
        arAffixSet =
                Collections
                        .unmodifiableSet(Generics.newHashSet(Arrays.asList(arabicAffixString.split("\\s+"))));
    }

    // Only static methods
    private IOBUtils() {}

    public static String getBoundaryCharacter() {
        return BoundaryChar;
    }
    public static List<CoreLabel> StringToIOB(String str, Character segMarker) {
        // Whitespace tokenization
        List<CoreLabel> toks = Sentence.toCoreLabelList(str.trim().split("\\s+"));
        return StringToIOB(toks, segMarker);
      }

    /**
     * Convert a Tokens a list of characters suitable for labeling in an IOB segmentation model.
     * <p>
     * In addition to the std. IOBUtils this also adds the original char position
     * 
     * @param tokenList
     * @param segMarker
     * @param applyRewriteRules
     *            add rewrite labels (for training data)
     */
    public static List<CoreLabel> StringToIOB(List<CoreLabel> tokenList, Character segMarker) {
        List<CoreLabel> iobList = new ArrayList<CoreLabel>(tokenList.size() * 7 + tokenList.size());
        final String strSegMarker = String.valueOf(segMarker);

        boolean addWhitespace = false;
        int charIndex = 0;
        final int numTokens = tokenList.size();
        String lastToken = "";
        for (int i = 0; i < numTokens; ++i) {
            if (addWhitespace) {
                iobList.add(createDatum(BoundaryChar, BoundarySymbol, charIndex++, "", "", null, null, null));
                addWhitespace = false;
            }

            // What type of token is this?
            CoreLabel token = tokenList.get(i);
            String tokenText = token.word();
            TokenType tokType = getTokenType(tokenText, strSegMarker);
            tokenText = stripSegmentationMarkers(tokenText, tokType);
            assert tokenText.length() != 0;
            if (shouldNotSegment(tokenText)) {
                iobList.add(createDatum(tokenText, NosegSymbol, charIndex++, tokenText,
                    token.get(CoreAnnotations.OriginalTextAnnotation.class),
                    token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
                    token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class),
                    token.get(CoreAnnotations.BeforeAnnotation.class)));
                addWhitespace = true;

            } else {
                // Iterate over the characters in the token
                tokenToDatums(iobList, tokenText, tokType, tokenList.get(i), lastToken, charIndex, false);
                //MM addWhitespace = true;
                addWhitespace = (tokType == TokenType.BeginMarker || tokType == TokenType.NoMarker);
            }
            lastToken = tokenText;
        }
        return iobList;
    }

    /**
     * Convert token to a sequence of datums and add to iobList.
     * 
     * @param iobList
     * @param tokenText
     * @param tokenLabel
     * @param lastToken
     * @param charIndex
     * @param applyRewriteRules
     */
    private static void tokenToDatums(List<CoreLabel> iobList, String token, TokenType tokType,
            CoreLabel tokenLabel, String lastToken, int charIndex, boolean applyRewriteRules) {
        String lastLabel = ContinuationSymbol;
        String firstLabel = BeginSymbol;
        if (applyRewriteRules) {
            // Apply Arabic-specific re-write rules
            String rawToken = tokenLabel.word();
            String tag = tokenLabel.tag();
            MorphoFeatureSpecification featureSpec = new ArabicMorphoFeatureSpecification();
            featureSpec.activate(MorphoFeatureType.NGEN);
            featureSpec.activate(MorphoFeatureType.NNUM);
            MorphoFeatures features = featureSpec.strToFeatures(tag);

            // Rule #1 : ت --> ة
            if (features.getValue(MorphoFeatureType.NGEN).equals("F")
                    && features.getValue(MorphoFeatureType.NNUM).equals("SG") && rawToken.endsWith("ت-")) {
                lastLabel = RewriteTahSymbol;
            }

            // Rule #2 : لل --> ل ال
            if (lastToken.equals("ل") && rawToken.startsWith("-ل")) {
                firstLabel = RewriteTareefSymbol;
            }
        }
        int index = tokenLabel.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
        String origToken = tokenLabel.get(CoreAnnotations.OriginalTextAnnotation.class);
        // Create datums and add to iobList
        String firstChar = String.valueOf(token.charAt(0));
        iobList.add(createDatum(firstChar, firstLabel, charIndex++, firstChar,
            String.valueOf(origToken.charAt(0)), index++,
            index, tokenLabel.get(CoreAnnotations.BeforeAnnotation.class)));
        final int numChars = token.length();
        for (int j = 1; j < numChars; ++j) {
            String thisChar = String.valueOf(token.charAt(j));
            String charLabel = (j == numChars - 1) ? lastLabel : ContinuationSymbol;
            iobList.add(createDatum(thisChar, charLabel, charIndex++, thisChar, 
                String.valueOf(origToken.charAt(j)),index++, index, ""));
        }
    }

    /**
     * Identify tokens that should not be segmented.
     * 
     * @param token
     * @return
     */
    private static boolean shouldNotSegment(String token) {
        return (isDigit.matcher(token).find() || isPunc.matcher(token).find() || notUnicodeArabic.matcher(
            token).find());
    }

    /**
     * Strip segmentation markers.
     *
     * @param tok
     * @param tokType
     * @return
     */
    private static String stripSegmentationMarkers(String tok, TokenType tokType) {
      int beginOffset = (tokType == TokenType.BeginMarker || tokType == TokenType.BothMarker) ? 1 : 0;
      int endOffset = (tokType == TokenType.EndMarker || tokType == TokenType.BothMarker) ? tok.length()-1 : tok.length();
      return tokType == TokenType.NoMarker ? tok : tok.substring(beginOffset, endOffset);
    }

//    /**
//     * Strip segmentation markers.
//     * 
//     * @param tok
//     * @param tokType
//     * @return
//     */
//    private static int[] getStartEndOffset(String tok, TokenType tokType) {
//        return new int[] {
//                (tokType == TokenType.BeginMarker || tokType == TokenType.BothMarker) ? 1 : 0,
//                (tokType == TokenType.EndMarker || tokType == TokenType.BothMarker) ? tok.length() - 1 : tok
//                        .length()};
//    }

    /**
     * Create a datum from a string. The CoreAnnotations must correspond to those used by SequenceClassifier.
     * <p>
     * This also add the string index offset positions. Those may be used ofter Segmentation to correctly
     * recreate the original tokens
     * 
     * @param token
     * @param label
     * @param index
     *            token index
     * @param startOffset
     *            the {@link BeginIndexAnnotation} of the token
     * @param endOffset
     *            the {@link EndIndexAnnotation} of the token
     * @return
     */
    private static CoreLabel createDatum(String token, String label, int index, String word,
            String origWord, Integer startOffset, Integer endOffset, String before) {
        CoreLabel newTok = new CoreLabel();
        newTok.setWord(word == null ? token : word);
        newTok.set(CoreAnnotations.CharAnnotation.class, token);
        newTok.set(CoreAnnotations.AnswerAnnotation.class, label);
        newTok.set(CoreAnnotations.GoldAnswerAnnotation.class, label);
        if (startOffset != null) {
            newTok.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, startOffset);
        }
        if (endOffset != null) {
            newTok.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, endOffset);
        }
        if (before != null) {
            newTok.setBefore(before);
        }
        if(origWord != null){
            newTok.set(CoreAnnotations.OriginalTextAnnotation.class, origWord);
        }
        newTok.setIndex(index);
        return newTok;
    }

    /**
     * Deterministically classify a token.
     *
     * @param token
     * @param segMarker
     * @return
     */
    private static TokenType getTokenType(String token, String segMarker) {
      if (segMarker == null || token.equals(segMarker)) {
        return TokenType.NoMarker;
      }
  
      TokenType tokType = TokenType.NoMarker;
      boolean startsWithMarker = token.startsWith(segMarker);
      boolean endsWithMarker = token.endsWith(segMarker);

      if (startsWithMarker && endsWithMarker) {
        tokType = TokenType.BothMarker;

      } else if (startsWithMarker) {
        tokType = TokenType.BeginMarker;

      } else if (endsWithMarker) {
        tokType = TokenType.EndMarker;
      }
      return tokType;
    }

    /**
     * Convert a list of labeled characters to a String. Include segmentation markers in the string.
     * 
     * @param labeledSequence
     * @param prefixMarker
     */
    public static List<CoreLabel> IOBToToken(List<CoreLabel> labeledSequence) {
        String lastLabel = "";
        final int sequenceLength = labeledSequence.size();
        List<CoreLabel> wordList = new ArrayList<CoreLabel>(32);
        StringBuilder tokenBuilder = new StringBuilder();
        StringBuilder origTokenBuilder = new StringBuilder();
        Integer tokenStart = null;
        Integer tokenEnd = null;
        String tokenBefore = null;
        CoreLabel word = null; //the last word created
        for (int i = 0; i < sequenceLength; ++i) {
            CoreLabel labeledChar = labeledSequence.get(i);
            String token = labeledChar.get(CoreAnnotations.CharAnnotation.class);
            String origToken = labeledChar.get(CoreAnnotations.OriginalTextAnnotation.class);
            String label = labeledChar.get(CoreAnnotations.AnswerAnnotation.class);
            Integer startIndex = labeledChar.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            Integer endIndex = labeledChar.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
            log.debug("> {}: {}",token,label);
            String tokenText = null;
            String origTokenText = null;
            if (label.equals(BeginSymbol)) {
                if (lastLabel.equals(ContinuationSymbol) || lastLabel.equals(BeginSymbol)) {
                    tokenText = tokenBuilder.toString();
                    tokenBuilder = new StringBuilder();
                    origTokenText = origTokenBuilder.toString();
                    origTokenBuilder = new StringBuilder();
                }
            } else if (label.equals(ContinuationSymbol)) {
                // nothing todo
            } else if (label.equals(NosegSymbol)) {
                if (!lastLabel.equals(BoundarySymbol)) {
                    tokenText = tokenBuilder.toString();
                    tokenBuilder = new StringBuilder();
                    origTokenText = origTokenBuilder.toString();
                    origTokenBuilder = new StringBuilder();
                }
            } else if (label.equals(BoundarySymbol)) {
                tokenText = tokenBuilder.toString();
                tokenBuilder = new StringBuilder();
                origTokenText = origTokenBuilder.toString();
                origTokenBuilder = new StringBuilder();
            } else if (label.equals(RewriteTahSymbol)) {
                tokenBuilder.append("ة");
                tokenText = tokenBuilder.toString();
                tokenBuilder = new StringBuilder();
                origTokenBuilder.append("ة");
                origTokenText = origTokenBuilder.toString();
                origTokenBuilder = new StringBuilder();
            } else if (label.equals(RewriteTareefSymbol)) {
                tokenText = tokenBuilder.toString();
                tokenBuilder = new StringBuilder();
                tokenBuilder.append("ال");
                origTokenText = origTokenBuilder.toString();
                origTokenBuilder = new StringBuilder();
                origTokenBuilder.append("ال");
            } else {
                throw new RuntimeException("Unknown label: " + label);
            }
            if (tokenText != null) {
                word = new CoreLabel();
                word.setWord(tokenText);
                word.setValue(tokenText);
                word.setOriginalText(origTokenText);
                word.setBeginPosition(tokenStart == null ? -1 : tokenStart);
                word.setEndPosition(tokenEnd == null ? -1 : tokenEnd);
                word.setBefore(tokenBefore);
                word.setIndex(wordList.size());
                wordList.add(word);
                tokenText = null;
                tokenStart = null;
                tokenEnd = null;
                // tokenBefore for the next token
            }
            if(tokenStart == null){ //first char of the word
                tokenBefore = labeledChar.get(CoreAnnotations.BeforeAnnotation.class);
                tokenStart = startIndex;
                if(word != null && //not applicable for the first word
                        tokenStart != null && tokenBefore != null){
                    //set the after of the previouse word to the before of the current
                    word.setAfter(tokenBefore);
                }
            }
            tokenBuilder.append(labeledChar.word());
            origTokenBuilder.append(labeledChar.originalText());
            tokenEnd = endIndex;
            lastLabel = label;
        }
        if(tokenBuilder.length() > 0){
            String tokenText = tokenBuilder.toString();
            word = new CoreLabel();
            word.setWord(tokenText);
            word.setValue(tokenText);
            word.setOriginalText(origTokenBuilder.toString());
            word.setBeginPosition(tokenStart == null ? -1 : tokenStart);
            word.setEndPosition(tokenEnd == null ? -1 : tokenEnd);
            word.setBefore(tokenBefore);
            word.setIndex(wordList.size());
            word.setAfter("");
            wordList.add(word);
            
        }
        return wordList;
    }

//    private static boolean addPrefixMarker(int focus, List<CoreLabel> labeledSequence) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = focus - 1; i >= 0; --i) {
//            String token = labeledSequence.get(i).get(CoreAnnotations.CharAnnotation.class);
//            String label = labeledSequence.get(i).get(CoreAnnotations.AnswerAnnotation.class);
//            sb.append(token);
//            if (label.equals(BeginSymbol) || label.equals(BoundarySymbol)) {
//                break;
//            }
//        }
//        return arAffixSet.contains(sb.toString());
//    }
//
//    private static boolean addSuffixMarker(int focus, List<CoreLabel> labeledSequence) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = focus; i < labeledSequence.size(); ++i) {
//            String token = labeledSequence.get(i).get(CoreAnnotations.CharAnnotation.class);
//            String label = labeledSequence.get(i).get(CoreAnnotations.AnswerAnnotation.class);
//            if (label.equals(BoundarySymbol)) {
//                break;
//            } else if (i != focus && label.equals(BeginSymbol)) {
//                return false;
//            }
//            sb.append(token);
//        }
//        return arAffixSet.contains(sb.toString());
//    }
}
