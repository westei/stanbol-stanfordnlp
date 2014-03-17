package at.salzburgresearch.stanbol.enhancer.nlp.stanford.impl;

import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.pipeline.Annotator;

/**
 * Used in the {@link StanbolDeterministicCorefAnnotator} class. 
 * We need to pass it a parse {@link Annotator} when it is instantiated
 * so that later on the call to StanfordCoreNLP.getExistingAnnotator("parse") is
 * avoided since that call uses the static pool at {@link StanfordCoreNLP} which
 * is not used in our case and it would crash with a NPE.
 * 
 * @author Cristian Petroaca
 *
 */
public class StanbolRuleBasedCorefMentionFinder extends RuleBasedCorefMentionFinder {

	public StanbolRuleBasedCorefMentionFinder(Annotator parserProcessor, boolean allowReparsing) {
		super(allowReparsing);
		
		this.parserProcessor = parserProcessor;
	}
}
