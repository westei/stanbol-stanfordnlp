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
import java.util.Locale;
import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelationTag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagSetRegistry {
    
    private final Logger log = LoggerFactory.getLogger(TagSetRegistry.class);

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
    
    private final Map<String, TagSet<GrammaticalRelationTag>> gramRelationModels = 
        new HashMap<String, TagSet<GrammaticalRelationTag>>();
    
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

    private void addDependencyTreeTagSet(TagSet<GrammaticalRelationTag> model) {
        for(String lang : model.getLanguages()) {
            if(gramRelationModels.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Dependency Models for Language '"
                    + lang+"'! This is an error in the static configuration of "
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
        for(String lang : parseLanguage(language)){
            TagSet<PosTag> tagset = posModels.get(lang);
            if(tagset != null){
                log.debug("select {} POS tagset for language {}", lang, language);
                return tagset;
            }
        }
        log.debug("No POS tagset registered for language {}", language);
        return null;
    }
    /**
     * Getter for the {@link NerTag} {@link TagSet} by language. If no {@link TagSet}
     * is available for an Language this will return <code>null</code>
     * @param language the language
     * @return the AnnotationModel or <code>null</code> if non is defined
     */
    public TagSet<NerTag> getNerTagSet(String language){
        for(String lang : parseLanguage(language)){
            TagSet<NerTag>  tagset = nerModels.get(lang);
            if(tagset != null){
                log.debug("select {} NER tagset for language {}", lang, language);
                return tagset;
            }
        }
        return DEFAULT_NER_TAGSET; //fallback to default 
    }
    
    /**
     * Parses languages with extensions and also converts the parsed language
     * to lower case
     * @param language the language
     * @return the array of compatible languages. Most specific first. If
     * <code>null<code> or an empty string is parsed an empty array is returned.
     */
    protected String[] parseLanguage(String language){
        if(language == null || language.isEmpty()){
            return new String[]{};
        }
        language = language.toLowerCase(Locale.ROOT);
        int idx = language.indexOf('-');
        if(idx <= 1){ //ignore 'x-*' language codes
            return new String[]{language};
        } else { //split languages with extension 'en-US'
            return new String[]{language, language.substring(0,idx)};
        }
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

    /**
     * Getter for the {@link GrammaticalRelationTag} {@link TagSet} by language. If no {@link TagSet}
     * is available for an Language this will return <code>null</code>
     * @param language the language
     * @return the AnnotationModel or <code>null</code> if non is defined
     */
    public TagSet<GrammaticalRelationTag> getGrammaticalRelationTagSet(String language) {
        for(String lang : parseLanguage(language)){
            TagSet<GrammaticalRelationTag> tagset = gramRelationModels.get(language);
            if(tagset != null){
                log.debug("select {} GrammaticalRelationTag tagset for language {}", lang, language);
                return tagset;
            }
        }
        log.debug("No GrammaticalRelationTag tagset registered for language {}", language);
        return null;
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
        arabicTreebank.addTag(new PosTag("DTJJR", LexicalCategory.Adjective,Pos.Determiner)); //Not found assumed same as DTJJ
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
        //Mapping found in http://aclweb.org/anthology//C/C10/C10-1045.pdf
        arabicTreebank.addTag(new PosTag("VN", Pos.VerbalNoun));
        arabicTreebank.addTag(new PosTag("VBD",LexicalCategory.Verb)); //TODO: improve mapping of perfect verb
        //mapping from http://www.researchgate.net/publication/228340249_Enhanced_annotation_and_parsing_of_the_arabic_treebank/file/5046351802c78cf6f6.pdf
        arabicTreebank.addTag(new PosTag("VBG",Pos.VerbalParticle));
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
        //Person, Organization and Locations mapped to the according DBPEDIA ontology types
        nerTags.addTag(new NerTag("PERSON", OntologicalClasses.DBPEDIA_PERSON));
        nerTags.addTag(new NerTag("ORGANIZATION", OntologicalClasses.DBPEDIA_ORGANISATION));
        nerTags.addTag(new NerTag("LOCATION", OntologicalClasses.DBPEDIA_PLACE));
        
        //other NER tags mapped to some Ontology Concept
        nerTags.addTag(new NerTag("MONEY", new UriRef(NamespaceEnum.dbpedia_ont + "Currency")));
        nerTags.addTag(new NerTag("DATE", new UriRef("http://www.w3.org/2006/time#Instant")));
        nerTags.addTag(new NerTag("TIME", new UriRef("http://www.w3.org/2006/time#Instant")));
        nerTags.addTag(new NerTag("DURATION", new UriRef("http://www.w3.org/2006/time#Interval")));

        //further unmapped POS tags
        nerTags.addTag(new NerTag("MISC"));
        nerTags.addTag(new NerTag("ORDINAL"));
        nerTags.addTag(new NerTag("NUMBER"));
        nerTags.addTag(new NerTag("PERCENT"));
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
    
    /*
     * Dependency Tree TagSet definitions
     */
    static {
        //the default TagSet for the grammatical relation tags used for English
        TagSet<GrammaticalRelationTag> gramRelationTags = 
            new TagSet<GrammaticalRelationTag>("Default Stanford NLP Dependency Tree Tagset", "en");
        
        gramRelationTags.addTag(new GrammaticalRelationTag("abbrev", GrammaticalRelation.AbbreviationModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("acomp", GrammaticalRelation.AdjectivalComplement));
        gramRelationTags.addTag(new GrammaticalRelationTag("amod", GrammaticalRelation.AdjectivalModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("advcl", GrammaticalRelation.AdverbialClauseModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("advmod", GrammaticalRelation.AdverbialModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("agent", GrammaticalRelation.Agent));
        gramRelationTags.addTag(new GrammaticalRelationTag("appos", GrammaticalRelation.AppositionalModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("attr", GrammaticalRelation.Attributive));
        gramRelationTags.addTag(new GrammaticalRelationTag("aux", GrammaticalRelation.Auxiliary));
        gramRelationTags.addTag(new GrammaticalRelationTag("xcomp", GrammaticalRelation.ClausalComplementWithExternalSubject));
        gramRelationTags.addTag(new GrammaticalRelationTag("ccomp", GrammaticalRelation.ClausalComplementWithInternalSubject));
        gramRelationTags.addTag(new GrammaticalRelationTag("csubj", GrammaticalRelation.ClausalSubject));
        gramRelationTags.addTag(new GrammaticalRelationTag("complm", GrammaticalRelation.Complementizer));
        gramRelationTags.addTag(new GrammaticalRelationTag("number", GrammaticalRelation.CompountNumberElement));
        gramRelationTags.addTag(new GrammaticalRelationTag("conj", GrammaticalRelation.Conjunct));
        gramRelationTags.addTag(new GrammaticalRelationTag("xsubj", GrammaticalRelation.ControllingSubject));
        gramRelationTags.addTag(new GrammaticalRelationTag("cc", GrammaticalRelation.Coordination));
        gramRelationTags.addTag(new GrammaticalRelationTag("cop", GrammaticalRelation.Copula));
        gramRelationTags.addTag(new GrammaticalRelationTag("dep", GrammaticalRelation.Dependent));
        gramRelationTags.addTag(new GrammaticalRelationTag("det", GrammaticalRelation.Determiner));
        gramRelationTags.addTag(new GrammaticalRelationTag("dobj", GrammaticalRelation.DirectObject));
        gramRelationTags.addTag(new GrammaticalRelationTag("expl", GrammaticalRelation.Expletive));
        gramRelationTags.addTag(new GrammaticalRelationTag("iobj", GrammaticalRelation.IndirectObject));
        gramRelationTags.addTag(new GrammaticalRelationTag("infmod", GrammaticalRelation.InfinitivalModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("mark", GrammaticalRelation.Marker));
        gramRelationTags.addTag(new GrammaticalRelationTag("mwe", GrammaticalRelation.MultiWordExpression));
        gramRelationTags.addTag(new GrammaticalRelationTag("neg", GrammaticalRelation.NegationModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("nsubj", GrammaticalRelation.NominalSubject));
        gramRelationTags.addTag(new GrammaticalRelationTag("nn", GrammaticalRelation.NounCompoundModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("npadvmod", GrammaticalRelation.NounPhraseAsAdverbialModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("num", GrammaticalRelation.NumericModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("pobj", GrammaticalRelation.ObjectOfPreposition));
        gramRelationTags.addTag(new GrammaticalRelationTag("parataxis", GrammaticalRelation.Parataxis));
        gramRelationTags.addTag(new GrammaticalRelationTag("partmod", GrammaticalRelation.ParticipalModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("auxpass", GrammaticalRelation.PassiveAuxiliary));
        gramRelationTags.addTag(new GrammaticalRelationTag("csubjpass", GrammaticalRelation.PassiveClausalSubject));
        gramRelationTags.addTag(new GrammaticalRelationTag("nsubjpass", GrammaticalRelation.PassiveNominalSubject));
        gramRelationTags.addTag(new GrammaticalRelationTag("prt", GrammaticalRelation.PhrasalVerbParticle));
        gramRelationTags.addTag(new GrammaticalRelationTag("poss", GrammaticalRelation.PossessionModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("possessive", GrammaticalRelation.PossessiveModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("preconj", GrammaticalRelation.Preconjunct));
        gramRelationTags.addTag(new GrammaticalRelationTag("predet", GrammaticalRelation.Predeterminer));
        gramRelationTags.addTag(new GrammaticalRelationTag("prepc", GrammaticalRelation.PrepositionalClausalModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("pcomp", GrammaticalRelation.PrepositionalComplement));
        gramRelationTags.addTag(new GrammaticalRelationTag("prep", GrammaticalRelation.PrepositionalModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("punct", GrammaticalRelation.Punctuation));
        gramRelationTags.addTag(new GrammaticalRelationTag("purpcl", GrammaticalRelation.PurposeClauseModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("quantmod", GrammaticalRelation.QuantifierModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("ref", GrammaticalRelation.Referent));
        gramRelationTags.addTag(new GrammaticalRelationTag("rel", GrammaticalRelation.Relative));
        gramRelationTags.addTag(new GrammaticalRelationTag("rcmod", GrammaticalRelation.RelativeClauseModifier));
        gramRelationTags.addTag(new GrammaticalRelationTag("root", GrammaticalRelation.Root));
        gramRelationTags.addTag(new GrammaticalRelationTag("tmod", GrammaticalRelation.TemporalModifier));
        
        getInstance().addDependencyTreeTagSet(gramRelationTags);
    }
}
