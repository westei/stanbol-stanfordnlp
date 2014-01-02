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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
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

import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.LangPipeline;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.StanfordNlpAnalyzer;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.mappings.TagSetRegistry;

public class TestNonEnglishConfig {

    private static final Logger log = LoggerFactory.getLogger(TestNonEnglishConfig.class);
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int ANALYZER_THREADS = 4;
    private static final ClassLoader cl = TestNonEnglishConfig.class.getClassLoader();
    
    private static ContentItemFactory cif;
    private static ExecutorService executorService;
        
//    private static final Map<String, Blob> deExamples = new HashMap<String,Blob>();
    private static final Map<String, Blob> frExamples = new HashMap<String,Blob>();
    private static final Map<String, Blob> arExamples = new HashMap<String,Blob>();
    private static final Map<String, Blob> zhExamples = new HashMap<String,Blob>();

    private static final String TEST_FILE_FOLDER = "text-examples";
//    private static final String DE_TEST_CONFIG = "de.pipeline";
//    private static final List<String> DE_TEST_FILE_NAMES = Arrays.asList(
//        "TODO.txt");
    private static final String FR_TEST_CONFIG = "fr.pipeline";
    private static final List<String> FR_TEST_FILE_NAMES = Arrays.asList(
        "mali_vote_france.txt");
    private static final String AR_TEST_CONFIG = "ar.pipeline";
    private static final List<String> AR_TEST_FILE_NAMES = Arrays.asList(
        "gaza_hamas_killing.txt", "test1.txt", "test2.txt", "test3.txt", "test4.txt");
    private static final String ZH_TEST_CONFIG = "zh.pipeline";
    private static final List<String> ZH_TEST_FILE_NAMES = Arrays.asList(
        "liushihui_beaten_linzhao.txt");

    
    private static final TagSetRegistry tagSetRegistry = TagSetRegistry.getInstance();
    
    @BeforeClass
    public static void initStanfordNlpPipeline() throws Exception {
        log.info("Init ExecutorService with {} threads", ANALYZER_THREADS);
        executorService = Executors.newFixedThreadPool(ANALYZER_THREADS);
        cif = InMemoryContentItemFactory.getInstance();
        //init the text eamples
//        for(String name : DE_TEST_FILE_NAMES){
//            String file = TEST_FILE_FOLDER+"/de/"+name;
//            deExamples.put(name, cif.createBlob(new StreamSource(
//                cl.getResourceAsStream(file),"text/plain; charset="+UTF8.name())));
//        }
        log.info("Init French Language Test Files");
        for(String name : FR_TEST_FILE_NAMES){
            log.info(" ... {}",name);
            String file = TEST_FILE_FOLDER+"/fr/"+name;
            frExamples.put(name, cif.createBlob(new StreamSource(
                cl.getResourceAsStream(file),"text/plain; charset="+UTF8.name())));
        }
        log.info("Init Arabic Language Test Files");
        for(String name : AR_TEST_FILE_NAMES){
            log.info(" ... {}",name);
            String file = TEST_FILE_FOLDER+"/ar/"+name;
            arExamples.put(name, cif.createBlob(new StreamSource(
                cl.getResourceAsStream(file),"text/plain; charset="+UTF8.name())));
        }
        log.info("Init Chinese Language Test Files");
        for(String name : ZH_TEST_FILE_NAMES){
            log.info(" ... {}",name);
            String file = TEST_FILE_FOLDER+"/zh/"+name;
            zhExamples.put(name, cif.createBlob(new StreamSource(
                cl.getResourceAsStream(file),"text/plain; charset="+UTF8.name())));
        }
    }


//    @Test
//    public void testDeAnalysis() throws IOException {
//        for(Entry<String,Blob> example : deExamples.entrySet()){
//            AnalysedText at = analyzer.analyse("de",example.getValue());
//            validateAnalysedText(at.getSpan(), at, tagSetRegistry.getPosTagSet("de"));
//        }
//	}

    @Test
    public void testFrAnalysis() throws IOException {
        log.info("init French Analyzer Pipeline");
        StanfordNlpAnalyzer analyzer = new StanfordNlpAnalyzer(executorService, null);
        LangPipeline pipeline = new LangPipeline(FR_TEST_CONFIG);
        analyzer.setPipeline(pipeline.getLanguage(), pipeline);
        log.info(" ... initialised");
        for(Entry<String,Blob> example : frExamples.entrySet()){
            AnalysedText at = analyzer.analyse("fr",example.getValue());
            validateAnalysedText(at.getSpan(), at, tagSetRegistry.getPosTagSet("fr"));
        }
    }
    @Test
    public void testArAnalysis() throws IOException {
        log.info("init Arabic Analyzer Pipeline");
        StanfordNlpAnalyzer analyzer = new StanfordNlpAnalyzer(executorService, null);
        LangPipeline pipeline = new LangPipeline(AR_TEST_CONFIG);
        analyzer.setPipeline(pipeline.getLanguage(), pipeline);
        log.info(" ... initialised");
        for(Entry<String,Blob> example : arExamples.entrySet()){
            AnalysedText at = analyzer.analyse("ar",example.getValue());
            validateAnalysedText(at.getSpan(), at, tagSetRegistry.getPosTagSet("ar"));
        }
    }
    @Test
    public void testZhAnalysis() throws IOException {
        log.info("init Chinese Analyzer Pipeline");
        StanfordNlpAnalyzer analyzer = new StanfordNlpAnalyzer(executorService, null);
        LangPipeline pipeline = new LangPipeline(ZH_TEST_CONFIG);
        analyzer.setPipeline(pipeline.getLanguage(), pipeline);
        log.info(" ... initialised");
        for(Entry<String,Blob> example : zhExamples.entrySet()){
            AnalysedText at = analyzer.analyse("zh",example.getValue());
            validateAnalysedText(at.getSpan(), at, tagSetRegistry.getPosTagSet("zh"));
        }
    }

//    @Test
//    public void testConcurrentAnalyses() throws IOException, InterruptedException, ExecutionException{
//        //warm up
//        log.info("Start concurrent analyses test");
//        log.info("  ... warm up");
//        for(Entry<String,Blob> example : examples.entrySet()){
//            analyzer.analyse("en",example.getValue());
//        }
//
//        //performance test
//        long start = System.currentTimeMillis();
//        int concurrentRequests = 3;
//        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
//        int iterations = 100;
//        log.info("  ... start test with {} iterations", iterations);
//        List<Future<?>> tasks = new ArrayList<Future<?>>(iterations);
//        long[] times = new long[iterations];
//        Iterator<Blob> texts = examples.values().iterator();
//        for(int i=0; i < iterations;i++){
//            if(!texts.hasNext()){
//                texts = examples.values().iterator();
//            }
//            tasks.add(executor.submit(new AnalyzerRequest(i, times, analyzer, texts.next())));
//        }
//        for(Future<?> task : tasks){ //wait for completion of all tasks
//            task.get();
//        }
//        long duration = System.currentTimeMillis()-start;
//        log.info("Processed {} texts",iterations);
//        log.info("  > time       : {}ms",duration);
//        log.info("  > average    : {}ms",(duration)/(double)iterations);
//        long sumTime = 0;
//        for(int i=0;i<times.length;i++){
//            sumTime = sumTime+times[i];
//        }
//        log.info("  > processing : {}ms",sumTime);
//        float concurrency = sumTime/(float)duration;
//        log.info("  > concurrency: {} / {}%",concurrency, concurrency*100/concurrentRequests);
//    }
    
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
    
    
    private void validateAnalysedText(String text, AnalysedText at, TagSet<PosTag> tagset){
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
                            tagset.getTag(posTag.value().getTag()) != null);
                        //assert declining probabilities
                        Assert.assertTrue("Wrong order in "+posTags+" of "+span+"!",
                            prevProb < 0 || posTag.probability() <= prevProb);
                        prevProb = posTag.probability();
                    }
                    Assert.assertNull("Tokens MUST NOT have Phrase annotations!",
                        span.getAnnotation(PHRASE_ANNOTATION));
                    Assert.assertNull("Tokens MUST NOT have NER annotations!",
                        span.getAnnotation(NER_ANNOTATION));
                    break;
                case Chunk:
                    Assert.assertNull("Chunks MUST NOT have POS annotations!",
                        span.getAnnotation(POS_ANNOTATION));
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
                    break;
            }
        }
    }
    
    
    @AfterClass
    public static final void cleanUp(){
        executorService.shutdown();
    }
}
