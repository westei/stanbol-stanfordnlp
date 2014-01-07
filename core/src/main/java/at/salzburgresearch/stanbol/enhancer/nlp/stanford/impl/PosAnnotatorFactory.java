package at.salzburgresearch.stanbol.enhancer.nlp.stanford.impl;

import java.util.Properties;

import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.AnnotatorFactory;

public class PosAnnotatorFactory extends AnnotatorFactory {

    
    public PosAnnotatorFactory() {
        super(new Properties());
    }
    
    @Override
    public Annotator create() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String signature() {
        // TODO Auto-generated method stub
        return null;
    }

}
