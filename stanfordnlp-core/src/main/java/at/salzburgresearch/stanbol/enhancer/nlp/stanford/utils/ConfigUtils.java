package at.salzburgresearch.stanbol.enhancer.nlp.stanford.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.LangPipeline;
import at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser.LanguageDefaults;

/**
 * Utils for manipulating configuration files.
 * 
 * @author Cristian Petroaca
 *
 */
public final class ConfigUtils {
	private static final Logger log = LoggerFactory.getLogger(ConfigUtils.class);
	
	public static Properties loadConfigProperties(String config, String language) {
		Properties properties = new Properties(LanguageDefaults.getInstance().getDefaults(language));
        File configFile = new File(FilenameUtils.separatorsToSystem(config));
        if(configFile.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(configFile);
                log.info("   ... from File {}", configFile.getAbsolutePath());
                properties.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load properties for language '"
                    + language + "form file '"+configFile.getAbsolutePath()+"'!",e);
            } finally {
                IOUtils.closeQuietly(in);
            }
        } else { //load via classpath
            String configResource = FilenameUtils.separatorsToUnix(config);
            InputStream in = LangPipeline.class.getClassLoader().getResourceAsStream(configResource);
            if(in == null){
                throw new IllegalArgumentException("Unable to load parsed config '"
                    +config+"' from the file System or from the Classpath!");
            } else {
                log.info("   ... via Classpath Resource: {}", configFile.getAbsolutePath());
                try {
                    properties.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to load properties for language '"
                        + language + "form file '"+configFile.getAbsolutePath()+"'!",e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        }
        
        return properties;
	}
}
