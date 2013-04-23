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
        return nerModels.get(language);
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

    /*
     * POS TagSet definitions
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
     * NER TagSet definitions
     */
    static {
        //the default TagSet for the NER tags used for English
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
    
}
