/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.salzburgresearch.stanbol.enhancer.nlp.stanford.impl;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.NER_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.DEPENDENCY_ANNOTATION;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelationTag;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.DependencyTreeParser;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.LangPipeline;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.StanfordNlpAnalyzer;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.mappings.TagSetRegistry;

public class TestStanfordNlpAnalyser {

    private static final Logger log = LoggerFactory.getLogger(TestStanfordNlpAnalyser.class);
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int ANALYZER_THREADS = 10;
    private static final ClassLoader cl = TestStanfordNlpAnalyser.class.getClassLoader();
    
    private static StanfordNlpAnalyzer analyzer;
    
    private static ContentItemFactory cif;
    private static ExecutorService executorService;
    
    private static TagSet<PosTag> TAG_SET = TagSetRegistry.getInstance().getPosTagSet("en");
    private static TagSet<GrammaticalRelationTag> GRAMMATICAL_RELATION_TAG_SET = 
        TagSetRegistry.getInstance().getGrammaticalRelationTagSet("en");
    
    private static final Map<String, Blob> examples = new HashMap<String,Blob>();

    private static final String TEST_CONFIG = "en.pipeline";
    private static final String TEST_FILE_FOLDER = "text-examples";
    private static final List<String> TEST_FILE_NAMES = Arrays.asList("countrymention.txt");/*,
        "Egypt-protests_wikinews-org.txt", "jimi-hendrix.txt", "nuclear-fusion.txt", 
        "plasma.txt", "astronomers-discover-star.txt", "china-oil-spill.txt", 
        "obama-oil-drilling.txt", "robben-ford.txt", "australia-debate.txt",
        "mitchell-bio.txt", "obama-signing.txt", "robbie-williams.txt",
        "hillary-clinton.txt", "mitchell-joy-in-town.txt", "obama-tsa.txt",
        "singapore-police.txt", "indian-train-crash.txt", "michael-jackson.txt",
        "mitchell-safaris.txt", "obama.txt", "turkish-internet.txt",
        "coalition-australia.txt");*/
    
    @BeforeClass
    public static void initStanfordNlpPipeline() throws Exception {
        executorService = Executors.newFixedThreadPool(ANALYZER_THREADS);
        analyzer = new StanfordNlpAnalyzer(executorService, null);
        Assert.assertNotNull("Unable to find test configuration '"+TEST_CONFIG+"'!", 
            TestStanfordNlpAnalyser.class.getClassLoader().getResource(TEST_CONFIG));
        LangPipeline pipeline = new LangPipeline(TEST_CONFIG);
        analyzer.setPipeline(pipeline.getLanguage(), pipeline);
        
        DependencyTreeParser dtParser = DependencyTreeParserFactory.getDependencyTreeParser(TEST_CONFIG);
            analyzer.setDependencyTreeParser(dtParser);
        
        cif = InMemoryContentItemFactory.getInstance();
        //init the text eamples
        for(String name : TEST_FILE_NAMES){
            String file = TEST_FILE_FOLDER+'/'+name;
            examples.put(name, cif.createBlob(new StreamSource(
                cl.getResourceAsStream(file),"text/plain; charset="+UTF8.name())));
        }
    }


    @Test
    public void testAnalysis() throws IOException {
        for(Entry<String,Blob> example : examples.entrySet()){
            AnalysedText at = analyzer.analyse("en",example.getValue());
            validateAnalysedText(at.getSpan(), at);
        }
	}

    @Test
    public void testConcurrentAnalyses() throws IOException, InterruptedException, ExecutionException{
        //warm up
        log.info("Start concurrent analyses test");
        log.info("  ... warm up");
        for(Entry<String,Blob> example : examples.entrySet()){
            analyzer.analyse("en",example.getValue());
        }

        //performance test
        long start = System.currentTimeMillis();
        int concurrentRequests = 3;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        int iterations = 100;
        log.info("  ... start test with {} iterations", iterations);
        List<Future<?>> tasks = new ArrayList<Future<?>>(iterations);
        long[] times = new long[iterations];
        Iterator<Blob> texts = examples.values().iterator();
        for(int i=0; i < iterations;i++){
            if(!texts.hasNext()){
                texts = examples.values().iterator();
            }
            tasks.add(executor.submit(new AnalyzerRequest(i, times, analyzer, texts.next())));
        }
        for(Future<?> task : tasks){ //wait for completion of all tasks
            task.get();
        }
        long duration = System.currentTimeMillis()-start;
        log.info("Processed {} texts",iterations);
        log.info("  > time       : {}ms",duration);
        log.info("  > average    : {}ms",(duration)/(double)iterations);
        long sumTime = 0;
        for(int i=0;i<times.length;i++){
            sumTime = sumTime+times[i];
        }
        log.info("  > processing : {}ms",sumTime);
        float concurrency = sumTime/(float)duration;
        log.info("  > concurrency: {} / {}%",concurrency, concurrency*100/concurrentRequests);
    }
    
    private class AnalyzerRequest implements Runnable {

        private int index;
        private long[] times;
        private Blob blob;
        private StanfordNlpAnalyzer ta;

        private AnalyzerRequest(int index, long[] times, StanfordNlpAnalyzer ta, Blob blob){
            this.index = index;
            this.times = times;
            this.blob = blob;
            this.ta = ta;
        }
        
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            try {
                ta.analyse("en",blob);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            times[index] = System.currentTimeMillis()-start;
            log.info(" > finished task {} in {}ms",index+1,times[index]);
        }
        
    }
    
    
    private void validateAnalysedText(String text, AnalysedText at){
        Assert.assertNotNull(text);
        Assert.assertNotNull(at);
        //Assert the AnalysedText
        Assert.assertEquals(0, at.getStart());
        Assert.assertEquals(text.length(), at.getEnd());
        Iterator<Span> it = at.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));
        while(it.hasNext()){
            //validate that the span|start|end corresponds with the Text
            Span span = it.next();
            Assert.assertNotNull(span);
            Assert.assertEquals(text.substring(span.getStart(), span.getEnd()), 
                span.getSpan());
            switch (span.getType()) {
                case Token:
                    double prevProb = -1;
                    List<Value<PosTag>> posTags = span.getAnnotations(POS_ANNOTATION);
                    Assert.assertTrue("All Tokens need to have a PosTag (missing for "
                        + span+ ")", posTags != null && !posTags.isEmpty());
                    for(Value<PosTag> posTag : posTags){
                        //assert Mapped PosTags
                        Assert.assertTrue("PosTag "+posTag+" used by "+span+" is not present in the PosTagSet",
                            TAG_SET.getTag(posTag.value().getTag()) != null);
                        //assert declining probabilities
                        Assert.assertTrue("Wrong order in "+posTags+" of "+span+"!",
                            prevProb < 0 || posTag.probability() <= prevProb);
                        prevProb = posTag.probability();
                    }
                    
                    List<Value<DependencyRelation>> dependencyRelations = span.getAnnotations(DEPENDENCY_ANNOTATION);
                    if (dependencyRelations != null) {
                        for(Value<DependencyRelation> dependencyRelation : dependencyRelations) {
                            //assert Mapped GrammaticalRelationTags
                            Assert.assertTrue("DependencyRelation "+dependencyRelation+" used by "
                                +span+" is not present in the GrammaticalRelationTagSet",
                                GRAMMATICAL_RELATION_TAG_SET.getTag(dependencyRelation.value().getGrammaticalRelationTag().getTag()) != null);
                        }
                    }
                    
                    Assert.assertNull("Tokens MUST NOT have Phrase annotations!",
                        span.getAnnotation(PHRASE_ANNOTATION));
                    Assert.assertNull("Tokens MUST NOT have NER annotations!",
                        span.getAnnotation(NER_ANNOTATION));
                    break;
                case Chunk:
                    Assert.assertNull("Chunks MUST NOT have POS annotations!",
                        span.getAnnotation(POS_ANNOTATION));
                    Assert.assertNull("Chunks MUST NOT have DEPENDENCY annotations!",
                        span.getAnnotation(DEPENDENCY_ANNOTATION));
                    
                    prevProb = -1;
                    List<Value<NerTag>> nerTags = span.getAnnotations(NER_ANNOTATION);
                    boolean hasNerTag = (nerTags != null && !nerTags.isEmpty());
                    Assert.assertTrue("All Chunks need to have a NER Tag (missing for "
                            + span+ ")",  hasNerTag);
                    for(Value<NerTag> nerTag : nerTags){
                        Assert.assertNotEquals("NER Tags MUST NOT use '0' as Tag",
                            "0",nerTag.value().getTag());
                    }
                    break;
                default:
                    Assert.assertNull(span.getType()+" type Spans MUST NOT have POS annotations!",
                        span.getAnnotation(POS_ANNOTATION));
                    Assert.assertNull(span.getType()+" type Spans MUST NOT have Phrase annotations!",
                        span.getAnnotation(PHRASE_ANNOTATION));
                    Assert.assertNull(span.getType()+" type Spans MUST NOT have NER annotations!",
                        span.getAnnotation(NER_ANNOTATION));
                    Assert.assertNull(span.getType()+" type Spans MUST NOT have DEPENDENCY annotations!",
                        span.getAnnotation(DEPENDENCY_ANNOTATION));
                    break;
            }
        }
    }
    
    
    @AfterClass
    public static final void cleanUp(){
        executorService.shutdown();
    }
}
