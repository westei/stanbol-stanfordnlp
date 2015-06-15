package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.sentiment;

public class LinearSentimentClassMapping implements SentimentClassMapping {

    final int numClasses;
    final int minValue;
    final int maxValue;
    final double increment;
    
    public LinearSentimentClassMapping(int numClasses) {
        this(numClasses,-1,1);
    }
    
    public LinearSentimentClassMapping(int numClasses, int minValue, int maxValue) {
        assert numClasses > 1;
        assert maxValue > minValue;
        this.numClasses = numClasses;
        this.minValue = minValue;
        this.maxValue = maxValue;
        increment = (maxValue - minValue)/((double)numClasses - 1);
    }
    
    @Override
    public double getIndexWeight(int index) {
        return minValue + index*increment;
    }
}
