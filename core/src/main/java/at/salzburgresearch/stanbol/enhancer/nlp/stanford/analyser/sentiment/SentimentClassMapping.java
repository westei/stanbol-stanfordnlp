package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.sentiment;

/**
 * Stanford NLP uses Sentiment Classes. Implementations of this interface are
 * used to assign double sentiment values (typically in the range [-1..+1]) to
 * those classes.
 * 
 * @author Rupert Westenthaler
 *
 */
public interface SentimentClassMapping {

    /**
     * Getter for the double weight for the sentiment class with a given index
     * @param index the index of the sentiment class
     * @return the double sentiment weight or {@link Double#NaN} if no weight is
     * defined for this index
     */
    double getIndexWeight(int index);

}
