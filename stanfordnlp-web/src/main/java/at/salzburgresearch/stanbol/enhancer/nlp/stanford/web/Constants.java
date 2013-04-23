package at.salzburgresearch.stanbol.enhancer.nlp.stanford.web;

import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.StanfordNlpAnalyzer;

public interface Constants {

    public static final String SERVLET_ATTRIBUTE_STANFORD_NLP = StanfordNlpAnalyzer.class.getName();
    public static final String SERVLET_ATTRIBUTE_ANALYSERS_TREADS = 
            Constants.class.getPackage().getName()+".analysersThreads";
    public static final String SERVLET_ATTRIBUTE_CONTENT_ITEM_FACTORY = ContentItemFactory.class.getName();
        
}
