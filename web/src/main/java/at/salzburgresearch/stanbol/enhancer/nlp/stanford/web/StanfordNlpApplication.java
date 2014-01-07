package at.salzburgresearch.stanbol.enhancer.nlp.stanford.web;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.enhancer.nlp.json.writer.AnalyzedTextWriter;

import at.salzburgresearch.stanbol.enhancer.nlp.stanford.web.reader.BlobReader;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.web.resource.AnalysisResource;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.web.resource.MainResource;

public class StanfordNlpApplication extends Application {
    
    @Override
    @SuppressWarnings("unchecked")
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(
            AnalyzedTextWriter.class, BlobReader.class, MainResource.class,
            AnalysisResource.class));
    }

}
