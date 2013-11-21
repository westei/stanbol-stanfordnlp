package at.salzburgresearch.stanbol.enhancer.nlp.stanford.impl;

import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.DependencyTreeParser;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.utils.ConfigUtils;

/**
 * Factory which creates a {@link DependencyTreeParser} based on the config file.
 * 
 * @author Cristian Petroaca
 * 
 */
public class DependencyTreeParserFactory {
    private static final Logger log = LoggerFactory.getLogger(DependencyTreeParserFactory.class);

    public static DependencyTreeParser getDependencyTreeParser(String configFile) {
        DependencyTreeParser parser = null;

        String language = FilenameUtils.getBaseName(configFile).toLowerCase();
        if (language == null || language.isEmpty()) {
            throw new IllegalArgumentException("Dependency Tree Parser configurations "
                                               + "MUST follow the '{lang}.config' file name pattern (name : "
                                               + configFile + ")");
        } else {
            log.info("Init Dependency Tree Parser for Language {} (config: {})", language, configFile);
        }

        Properties properties = ConfigUtils.loadConfigProperties(configFile, language);

        String isActive = properties.getProperty("dependency.tree.parser.active");

        if (isActive != null && isActive.equals("yes")) {
            parser = new DependencyTreeParser(properties, language);
        }

        return parser;
    }
}
