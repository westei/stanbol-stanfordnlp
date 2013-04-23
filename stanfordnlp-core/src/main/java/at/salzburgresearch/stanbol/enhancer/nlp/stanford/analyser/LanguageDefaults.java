package at.salzburgresearch.stanbol.enhancer.nlp.stanford.analyser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LanguageDefaults {

    Logger log = LoggerFactory.getLogger(LanguageDefaults.class);
    
    private static final LanguageDefaults DEFAULTS = new LanguageDefaults();
    private LanguageDefaults() {}
    
    private final Map<String,Properties> languageDefautls = new HashMap<String,Properties>();
    
    public Properties getDefaults(String language){
        Properties properties = languageDefautls.get(language);
        if(properties == null){
            String defaultsResource = new StringBuilder("defaults/").append(language)
                    .append(".properties").toString();
            InputStream in = LanguageDefaults.class.getClassLoader().getResourceAsStream(defaultsResource);
            if(in == null){
                log.warn("No defaults for Language '{}' (expected resource path {})",
                    language,defaultsResource);
                return new Properties();
            } else {
                try {
                    properties = new Properties();
                    properties.load(in);
                    languageDefautls.put(language, properties);
                } catch (IOException e) {
                    log.error("Unable to loead defualts for Language '"+language
                        +"' from resource '"+defaultsResource+"'!",e);
                    return new Properties();
                }
            }
        }
        return properties;
    }

    public static LanguageDefaults getInstance() {
        return DEFAULTS;
    }
    
    
}
