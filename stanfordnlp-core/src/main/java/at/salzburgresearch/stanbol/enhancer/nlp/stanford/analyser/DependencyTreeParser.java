package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.international.arabic.ArabicTreebankLanguagePack;
import edu.stanford.nlp.trees.international.french.FrenchTreebankLanguagePack;
import edu.stanford.nlp.trees.international.hebrew.HebrewTreebankLanguagePack;
import edu.stanford.nlp.trees.international.pennchinese.ChineseTreebankLanguagePack;
import edu.stanford.nlp.trees.international.tuebadz.TueBaDZLanguagePack;

/**
 * Wrapper class which uses the Stanford {@link LexicalizedParser} class to construct the dependency tree for
 * a given sentence.
 * 
 * @author Cristian Petroaca
 * 
 */
public class DependencyTreeParser {
    /**
     * The Stanford Parser used to get the dependency tree
     */
    private LexicalizedParser lexicalizedParser;

    /**
     * Language/Treebank information used by the {@link LexicalizedParser}
     */
    private TreebankLanguagePack treebankLanguagePack;

    /**
     * The language supported by this parser (defined by ISO 639-1 language codes)
     */
    private String language;

    public DependencyTreeParser(Properties properties, String language) {
        this.language = language;

        String maxLength = properties.getProperty("dependency.tree.sentence.maxlength");

        if (maxLength != null) {
            lexicalizedParser = LexicalizedParser.loadModel(properties.getProperty("parse.model"),
                "-maxLength", maxLength, "-retainTmpSubcategories");
        } else {
            lexicalizedParser = LexicalizedParser.loadModel(properties.getProperty("parse.model"),
                "-retainTmpSubcategories");
        }

        if (language.equals("en")) {
            treebankLanguagePack = new PennTreebankLanguagePack();
        } else if (language.equals("de")) {
            treebankLanguagePack = new TueBaDZLanguagePack();
        } else if (language.equals("fr")) {
            treebankLanguagePack = new FrenchTreebankLanguagePack();
        } else if (language.equals("ar")) {
            treebankLanguagePack = new ArabicTreebankLanguagePack();
        } else if (language.equals("zh")) {
            treebankLanguagePack = new ChineseTreebankLanguagePack();
        } else if (language.equals("he")) {
            treebankLanguagePack = new HebrewTreebankLanguagePack();
        } else {
            throw new IllegalArgumentException("Language " + language + "is not supported");
        }
    }

    /**
     * Parses the given word list to extract the typed dependencies
     * 
     * TODO - for the moment only basic non collapsed dependencies are returned; need to add support for
     * collapsed dependencies; for more information go to
     * http://nlp.stanford.edu/downloads/dependencies_manual.pdf
     * 
     * @param words
     * @return
     */
    public Collection<TypedDependency> parse(List<String> words) {
        Tree parse = lexicalizedParser.apply(Sentence.toWordList(words));
        GrammaticalStructureFactory gsf = treebankLanguagePack.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);

        return gs.typedDependencies();
    }

    public String getLanguage() {
        return this.language;
    }
}
