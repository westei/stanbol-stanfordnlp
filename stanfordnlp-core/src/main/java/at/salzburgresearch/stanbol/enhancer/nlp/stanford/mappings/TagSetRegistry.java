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
package at.salzburgresearch.stanbol.enhancer.nlp.stanford.mappings;

import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.pos.olia.English;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;

public class TagSetRegistry {

    private static TagSetRegistry instance = new TagSetRegistry();
    
    private TagSetRegistry(){}
    
    private final Map<String, TagSet<PosTag>> posModels = new HashMap<String,TagSet<PosTag>>();
    /**
     * Adhoc {@link PosTag}s created for string tags missing in the {@link #posModels}
     */
    private Map<String,Map<String,PosTag>> adhocPosTagMap = new HashMap<String,Map<String,PosTag>>();
    
    private final Map<String,TagSet<NerTag>> nerModels = new HashMap<String,TagSet<NerTag>>();
    
    /**
     * Adhoc {@link NerTag}s created for string tags missing in the {@link #nerModels}
     */
    private Map<String,Map<String,NerTag>> adhocNerTagMap = new HashMap<String,Map<String,NerTag>>();
    
    
    public static TagSetRegistry getInstance(){
        return instance;
    }
    
    private void addPosTagSet(TagSet<PosTag> model) {
        for(String lang : model.getLanguages()){
            if(posModels.put(lang, model) != null){
                throw new IllegalStateException("Multiple Pos Models for Language '"
                    + lang+"'! This is an error in the static confituration of "
                    + "this class!");
            }
        }
    }
    private void addNerTagSet(TagSet<NerTag> model) {
        for(String lang : model.getLanguages()){
            if(nerModels.put(lang, model) != null){
                throw new IllegalStateException("Multiple NER Models for Language '"
                    + lang+"'! This is an error in the static confituration of "
                    + "this class!");
            }
        }
    }

    /**
     * Getter for the {@link PosTag} {@link TagSet} by language. If no {@link TagSet}
     * is available for an Language this will return <code>null</code>
     * @param language the language
     * @return the AnnotationModel or <code>null</code> if non is defined
     */
    public TagSet<PosTag> getPosTagSet(String language){
        return posModels.get(language);
    }
    /**
     * Getter for the {@link NerTag} {@link TagSet} by language. If no {@link TagSet}
     * is available for an Language this will return <code>null</code>
     * @param language the language
     * @return the AnnotationModel or <code>null</code> if non is defined
     */
    public TagSet<NerTag> getNerTagSet(String language){
        TagSet<NerTag>  tagset = nerModels.get(language);
        return tagset == null ? DEFAULT_NER_TAGSET : tagset; 
    }
    
    /**
     * Getter for the map holding the adhoc {@link PosTag} for the given language
     * @param language the language
     * @return the map with the adhoc {@link PosTag}s
     */
    public Map<String,PosTag> getAdhocPosTagMap(String language){
        Map<String,PosTag> adhocMap =  adhocPosTagMap.get(language);
        if(adhocMap == null){
            adhocMap = new HashMap<String,PosTag>();
            adhocPosTagMap.put(language, adhocMap);
        }
        return adhocMap;
    }
    public Map<String,NerTag> getAdhocNerTagMap(String language) {
        Map<String,NerTag> adhocMap =  adhocNerTagMap.get(language);
        if(adhocMap == null){
            adhocMap = new HashMap<String,NerTag>();
            adhocNerTagMap.put(language, adhocMap);
        }
        return adhocMap;
    }

    /*
     * English POS TagSet definitions
     */
    static { 
        TagSet<PosTag> treebank = new TagSet<PosTag>(
            "Penn Treebank (Stanford NLP version)", "en");
        treebank.getProperties().put("olia.annotationModel", 
            new UriRef("http://purl.org/olia/penn.owl"));
        treebank.getProperties().put("olia.linkingModel", 
            new UriRef("http://purl.org/olia/penn-link.rdf"));

        treebank.addTag(new PosTag("CC", Pos.CoordinatingConjunction));
        treebank.addTag(new PosTag("CD",Pos.CardinalNumber));
        treebank.addTag(new PosTag("DT",Pos.Determiner));
        treebank.addTag(new PosTag("EX",Pos.ExistentialParticle)); //TODO: unsure mapping
        treebank.addTag(new PosTag("FW",Pos.Foreign));
        treebank.addTag(new PosTag("IN",Pos.Preposition, Pos.SubordinatingConjunction));
        treebank.addTag(new PosTag("JJ",LexicalCategory.Adjective));
        treebank.addTag(new PosTag("JJR",LexicalCategory.Adjective, Pos.ComparativeParticle));
        treebank.addTag(new PosTag("JJS",LexicalCategory.Adjective, Pos.SuperlativeParticle));
        treebank.addTag(new PosTag("LS",Pos.ListMarker));
        treebank.addTag(new PosTag("MD",Pos.ModalVerb));
        treebank.addTag(new PosTag("NN",Pos.CommonNoun, Pos.SingularQuantifier));
        treebank.addTag(new PosTag("NNP",Pos.ProperNoun, Pos.SingularQuantifier));
        treebank.addTag(new PosTag("NNPS",Pos.ProperNoun, Pos.PluralQuantifier));
        treebank.addTag(new PosTag("NNS",Pos.CommonNoun, Pos.PluralQuantifier));
        treebank.addTag(new PosTag("PDT",Pos.Determiner)); //TODO should be Pre-Determiner
        treebank.addTag(new PosTag("POS")); //TODO: map Possessive Ending (e.g., Nouns ending in 's)
        treebank.addTag(new PosTag("PP",Pos.PersonalPronoun));
        treebank.addTag(new PosTag("PP$",Pos.PossessivePronoun));
        treebank.addTag(new PosTag("PRP",Pos.PersonalPronoun));
        treebank.addTag(new PosTag("PRP$",Pos.PossessivePronoun));
        treebank.addTag(new PosTag("RB",LexicalCategory.Adverb));
        treebank.addTag(new PosTag("RBR",LexicalCategory.Adverb,Pos.ComparativeParticle));
        treebank.addTag(new PosTag("RBS",LexicalCategory.Adverb,Pos.SuperlativeParticle));
        treebank.addTag(new PosTag("RP",Pos.Participle));
        treebank.addTag(new PosTag("SYM",Pos.Symbol));
        treebank.addTag(new PosTag("TO",LexicalCategory.Adposition));
        treebank.addTag(new PosTag("UH",LexicalCategory.Interjection));
        treebank.addTag(new PosTag("VB",Pos.Infinitive)); //TODO check a Verb in the base form should be Pos.Infinitive
        treebank.addTag(new PosTag("VBD",Pos.PastParticiple)); //TODO check
        treebank.addTag(new PosTag("VBG",Pos.PresentParticiple,Pos.Gerund));
        treebank.addTag(new PosTag("VBN",Pos.PastParticiple));
        treebank.addTag(new PosTag("VBP",Pos.PresentParticiple));
        treebank.addTag(new PosTag("VBZ",Pos.PresentParticiple));
        treebank.addTag(new PosTag("WDT",Pos.WHDeterminer));
        treebank.addTag(new PosTag("WP",Pos.WHPronoun));
        treebank.addTag(new PosTag("WP$",Pos.PossessivePronoun, Pos.WHPronoun));
        treebank.addTag(new PosTag("WRB",Pos.WHTypeAdverbs));
        treebank.addTag(new PosTag("´´",Pos.CloseQuote));
        treebank.addTag(new PosTag(":",Pos.Colon));
        treebank.addTag(new PosTag(",",Pos.Comma));
        treebank.addTag(new PosTag("$",LexicalCategory.Residual));
        treebank.addTag(new PosTag("\"",Pos.Quote));
        treebank.addTag(new PosTag("''",Pos.Quote));
        treebank.addTag(new PosTag("``",Pos.OpenQuote));
        treebank.addTag(new PosTag(".",Pos.Point));
        treebank.addTag(new PosTag("{",Pos.OpenCurlyBracket));
        treebank.addTag(new PosTag("-LCB-",Pos.OpenCurlyBracket));
        treebank.addTag(new PosTag("}",Pos.CloseCurlyBracket));
        treebank.addTag(new PosTag("-RCB-",Pos.CloseCurlyBracket));
        treebank.addTag(new PosTag("[",Pos.OpenSquareBracket));
        treebank.addTag(new PosTag("-LSB-",Pos.OpenSquareBracket));
        treebank.addTag(new PosTag("]",Pos.CloseSquareBracket));
        treebank.addTag(new PosTag("-RSB-",Pos.CloseSquareBracket));
        treebank.addTag(new PosTag("(",Pos.OpenParenthesis));
        treebank.addTag(new PosTag("-LRB-",Pos.OpenParenthesis));
        treebank.addTag(new PosTag(")",Pos.CloseParenthesis));
        treebank.addTag(new PosTag("-RRB-",Pos.CloseParenthesis));
        
        getInstance().addPosTagSet(treebank);
    }
    
    /*
     * Arabic POS Tagset definitions 
     */
    static {
        TagSet<PosTag> arabicTreebank = new TagSet<PosTag>(
                "Arabic Treebank (Stanford NLP version)", "ar");
        arabicTreebank.addTag(new PosTag("JJ", LexicalCategory.Adjective));
        arabicTreebank.addTag(new PosTag("DTJJ", LexicalCategory.Adjective,Pos.Determiner));
        arabicTreebank.addTag(new PosTag("RB", LexicalCategory.Adverb));
        arabicTreebank.addTag(new PosTag("CC", Pos.CoordinatingConjunction));
        arabicTreebank.addTag(new PosTag("DT", Pos.Determiner,Pos.DemonstrativePronoun));
        arabicTreebank.addTag(new PosTag("FW", Pos.Foreign));
        
        arabicTreebank.addTag(new PosTag("NOUN", LexicalCategory.Noun));
        arabicTreebank.addTag(new PosTag("NN", Pos.CommonNoun));
        arabicTreebank.addTag(new PosTag("NNS", Pos.CommonNoun));
        arabicTreebank.addTag(new PosTag("NNP", Pos.ProperNoun));
        arabicTreebank.addTag(new PosTag("NNPS", Pos.ProperNoun));
        arabicTreebank.addTag(new PosTag("DTNN", Pos.Determiner, Pos.CommonNoun));
        arabicTreebank.addTag(new PosTag("DTNNS", Pos.Determiner, Pos.CommonNoun));
        arabicTreebank.addTag(new PosTag("DTNNP", Pos.Determiner, Pos.ProperNoun));
        arabicTreebank.addTag(new PosTag("DTNNPS", Pos.Determiner, Pos.ProperNoun));
                
        arabicTreebank.addTag(new PosTag("RP",Pos.Particle));
        arabicTreebank.addTag(new PosTag("VBP",Pos.PresentParticiple)); //TODO: validate mapping of imperfect verb
        arabicTreebank.addTag(new PosTag("VBN",Pos.PastParticiple)); //TODO: validate mapping of passive verb
        arabicTreebank.addTag(new PosTag("VBD",LexicalCategory.Verb)); //TODO: improve mapping of perfect verb
        arabicTreebank.addTag(new PosTag("UH",Pos.Interjection));
        arabicTreebank.addTag(new PosTag("PRP",Pos.PersonalPronoun));
        arabicTreebank.addTag(new PosTag("PRP$",Pos.PossessivePronoun, Pos.PersonalPronoun));
        arabicTreebank.addTag(new PosTag("CD",Pos.CardinalNumber));
        arabicTreebank.addTag(new PosTag("IN",Pos.SubordinatingConjunction));
        arabicTreebank.addTag(new PosTag("WP",Pos.RelativePronoun));
        arabicTreebank.addTag(new PosTag("WRB",Pos.WHTypeAdverbs));
        arabicTreebank.addTag(new PosTag("PUNC",LexicalCategory.Punctuation));
        arabicTreebank.addTag(new PosTag(",",Pos.Comma));
        arabicTreebank.addTag(new PosTag(".",Pos.Point));
        arabicTreebank.addTag(new PosTag(":",Pos.Colon));

        getInstance().addPosTagSet(arabicTreebank);

    }
    
    /*
     * Chinese POS tag set definitions 
     * see http://projects.ldc.upenn.edu/Chinese/docs/posguide.pdf
     */
    static {
        TagSet<PosTag> chineseTreebank = new TagSet<PosTag>(
                "Chinese Treebank (Stanford NLP version)", "zh");
        //verbs
        chineseTreebank.addTag(new PosTag("VA",LexicalCategory.Verb,Pos.PredicativeAdjective));
        chineseTreebank.addTag(new PosTag("VC",Pos.Copula));
        chineseTreebank.addTag(new PosTag("VE",LexicalCategory.Verb));
        chineseTreebank.addTag(new PosTag("VV",LexicalCategory.Verb)); //other verbs
        //Nouns
        chineseTreebank.addTag(new PosTag("NR",Pos.ProperNoun));
        chineseTreebank.addTag(new PosTag("NT",LexicalCategory.Noun)); //temporal noun
        chineseTreebank.addTag(new PosTag("NN",Pos.CommonNoun));
        
        chineseTreebank.addTag(new PosTag("PN",Pos.Pronoun));
        chineseTreebank.addTag(new PosTag("AD",LexicalCategory.Adverb));
        //propositions
        chineseTreebank.addTag(new PosTag("LB",Pos.Preposition));
        chineseTreebank.addTag(new PosTag("SB",Pos.Preposition));
        chineseTreebank.addTag(new PosTag("BA",Pos.Preposition));
        chineseTreebank.addTag(new PosTag("P",Pos.Preposition));
        //Determiners & Numbers
        chineseTreebank.addTag(new PosTag("DT",Pos.DemonstrativeDeterminer));
        chineseTreebank.addTag(new PosTag("CD",Pos.CardinalNumber));
        chineseTreebank.addTag(new PosTag("OD",Pos.OrdinalNumber));
        chineseTreebank.addTag(new PosTag("M")); //Measure Words TODO check mapping!
        //conjunctions
        chineseTreebank.addTag(new PosTag("CC",Pos.CoordinatingConjunction));
        chineseTreebank.addTag(new PosTag("CS",Pos.SubordinatingConjunction));

        chineseTreebank.addTag(new PosTag("LC")); //Localisers TODO check mapping!
        //markers
        chineseTreebank.addTag(new PosTag("DEC", Pos.Participle)); //complementizer or normalizer
        chineseTreebank.addTag(new PosTag("DEG", Pos.Participle)); //genitive marker or associative marker
        chineseTreebank.addTag(new PosTag("DER", Pos.Participle)); //resulative
        chineseTreebank.addTag(new PosTag("DEV", Pos.Participle)); //manner
        chineseTreebank.addTag(new PosTag("SP", Pos.Participle)); //Sentence final particle
        chineseTreebank.addTag(new PosTag("AS", Pos.Participle)); //aspect particle
        chineseTreebank.addTag(new PosTag("MSP", Pos.Participle)); //other particle
        
        chineseTreebank.addTag(new PosTag("IJ", Pos.Interjection));
        
        chineseTreebank.addTag(new PosTag("ON")); //Onomatopoeia
        //others
        chineseTreebank.addTag(new PosTag("JJ"));
        chineseTreebank.addTag(new PosTag("PU", LexicalCategory.Punctuation));
        chineseTreebank.addTag(new PosTag("FW", Pos.Foreign));
        chineseTreebank.addTag(new PosTag("X")); //unknown
        
        getInstance().addPosTagSet(chineseTreebank);

    }
    
    /*
     * French POS TagSet definitions
     */
    static {
        TagSet<PosTag> frenchTreebank = new TagSet<PosTag>(
                "French Treebank (Stanford NLP version)", "fr");
        
        frenchTreebank.addTag(new PosTag("A", LexicalCategory.Adjective));
        frenchTreebank.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        frenchTreebank.addTag(new PosTag("C", LexicalCategory.Conjuction)); //TODO: validate mapping
        frenchTreebank.addTag(new PosTag("CC", Pos.CoordinatingConjunction));
        frenchTreebank.addTag(new PosTag("CL", Pos.WeakPersonalPronoun)); //Weak Clitic Pronoun TODO check mapping!
        frenchTreebank.addTag(new PosTag("CS", Pos.SubordinatingConjunction));
        frenchTreebank.addTag(new PosTag("D", Pos.Determiner));
        frenchTreebank.addTag(new PosTag("ET", Pos.Foreign));
        frenchTreebank.addTag(new PosTag("I", Pos.Interjection));
        frenchTreebank.addTag(new PosTag("N", LexicalCategory.Noun));
        frenchTreebank.addTag(new PosTag("NC", Pos.CommonNoun));
        frenchTreebank.addTag(new PosTag("NP", Pos.ProperNoun));
        frenchTreebank.addTag(new PosTag("P", Pos.Preposition));
        frenchTreebank.addTag(new PosTag("PREF")); //prefix
        frenchTreebank.addTag(new PosTag("PRO", Pos.Pronoun));
        frenchTreebank.addTag(new PosTag("V", LexicalCategory.Verb));
        frenchTreebank.addTag(new PosTag("PUNC", LexicalCategory.Punctuation));
        
        getInstance().addPosTagSet(frenchTreebank);
    }
    /*
     * NER TagSet definitions
     */
    static {
        //the english TagSet for the NER tags used for English
        TagSet<NerTag> nerTags = new TagSet<NerTag>("Default Stanford NLP NER Tagset", "en");
        nerTags.addTag(new NerTag("PERSON", OntologicalClasses.DBPEDIA_PERSON));
        nerTags.addTag(new NerTag("ORGANIZATION", OntologicalClasses.DBPEDIA_ORGANISATION));
        nerTags.addTag(new NerTag("LOCATION", OntologicalClasses.DBPEDIA_PLACE));
        
        nerTags.addTag(new NerTag("MISC"));
        nerTags.addTag(new NerTag("MONEY"));
        nerTags.addTag(new NerTag("DURATION"));
        nerTags.addTag(new NerTag("PERCENT"));
        nerTags.addTag(new NerTag("DATE"));
        getInstance().addNerTagSet(nerTags);
        
    }

    /*
     * A default (fallback NER tag set)
     * TODO: remove as soon as a serializer/parser for NER Tagsets is present
     */
    private static final TagSet<NerTag> DEFAULT_NER_TAGSET = new TagSet<NerTag>("Fallback NER Tagset");
    static {
        DEFAULT_NER_TAGSET.addTag(new NerTag("PERSON", OntologicalClasses.DBPEDIA_PERSON));
        DEFAULT_NER_TAGSET.addTag(new NerTag("person", OntologicalClasses.DBPEDIA_PERSON));
        DEFAULT_NER_TAGSET.addTag(new NerTag("Person", OntologicalClasses.DBPEDIA_PERSON));
        DEFAULT_NER_TAGSET.addTag(new NerTag("PER", OntologicalClasses.DBPEDIA_PERSON));
        DEFAULT_NER_TAGSET.addTag(new NerTag("per", OntologicalClasses.DBPEDIA_PERSON));
        DEFAULT_NER_TAGSET.addTag(new NerTag("ORGANIZATION", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("organization", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("Organization", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("ORGANISATION", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("organisation", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("Organisation", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("ORG", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("org", OntologicalClasses.DBPEDIA_ORGANISATION));
        DEFAULT_NER_TAGSET.addTag(new NerTag("LOCATION", OntologicalClasses.DBPEDIA_PLACE));
        DEFAULT_NER_TAGSET.addTag(new NerTag("Location", OntologicalClasses.DBPEDIA_PLACE));
        DEFAULT_NER_TAGSET.addTag(new NerTag("location", OntologicalClasses.DBPEDIA_PLACE));
        DEFAULT_NER_TAGSET.addTag(new NerTag("LOC", OntologicalClasses.DBPEDIA_PLACE));
        DEFAULT_NER_TAGSET.addTag(new NerTag("loc", OntologicalClasses.DBPEDIA_PLACE));

    }
}
