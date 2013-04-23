package at.salzburgresearch.stanbol.enhancer.nlp.stanford.web.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;

import javax.servlet.ServletContext;

public class Utils {

    /**
     * Tries to retrieve a Resource with the parsed type from the {@link ServletContext}
     * by using {@link Class#getName()} as attribute name
     * @param type the expected type of the resource
     * @param servletContext the servlet context. MUST NOT be <code>null</code>
     * @return the resource (guaranteed to be not <code>null</code>
     * @throws IllegalStateException if the requested attribute is not present in 
     * the parsed context. If the value returned by the parsed context is not 
     * compatible with the requested type
     */
    public static <T> T getResource(Class<T> type, ServletContext servletContext){
        return getResource(type, servletContext, type.getName());
    }
    /**
     * Tries to retrieve a Resource with the parsed type from the {@link ServletContext}
     * by using the parsed attribute name
     * @param type the expected type of the resource
     * @param servletContext the servlet context. MUST NOT be <code>null</code>
     * @param attribute the attribute or <code>null</code> to use the {@link Class#getName()}
     * from the parsed type
     * @return the resource (guaranteed to be not <code>null</code>
     * @throws IllegalStateException if the requested attribute is not present in 
     * the parsed context. If the value returned by the parsed context is not 
     * compatible with the requested type
     */
    public static <T> T getResource(Class<T> type, ServletContext servletContext, String attribute){
        return getResource(type,servletContext,attribute,null);
    }
    /**
     * Tries to retrieve a Resource with the parsed type from the {@link ServletContext}
     * by using the parsed attribute name. If the attribute is not present and the
     * parsed default value is not <code>null</code> than the default value is returned
     * @param type the expected type of the resource
     * @param servletContext the servlet context. MUST NOT be <code>null</code>
     * @param attribute the attribute or <code>null</code> to use the {@link Class#getName()}
     * from the parsed type
     * @param defaultValue the default value or <code>null</code> if none
     * @return the resource (guaranteed to be not <code>null</code>
     * @throws IllegalStateException if the requested attribute is not present in 
     * the parsed context and no default value was given. If the value returned
     * by the parsed context is not compatible with the requested type
     */
    public static <T> T getResource(Class<T> type, ServletContext servletContext, String attribute, T defaultValue){
        Object value = servletContext.getAttribute(attribute);
        if(value == null){
            if(defaultValue != null){
                return defaultValue;
            } else {
                throw new IllegalStateException(type.getSimpleName()+" instance is not "
                    + "available in ServletContext with attribute '"+attribute+"'!");
            }
        }
        if(type.isAssignableFrom(value.getClass())){
            return type.cast(value);
        } else {
            throw new IllegalStateException("Value "+value+" provided by ServletContest attribute '"
                    + attribute +"' is not compatible with the requested type " 
                    + type +"(actual class: "+value.getClass()+")!");
        }

    }
    
    /**
     * Tests if a generic type (may be &lt;?&gt;, &lt;? extends {required}&gt; 
     * or &lt;? super {required}&gt;) is compatible with the required one.
     * TODO: Should be moved to an utility class
     * @param required the required class the generic type MUST BE compatible with
     * @param genericType the required class
     * @return if the generic type is compatible with the required class
     */
    public static boolean testType(Class<?> required, Type type) {
        //for the examples let assume that a Set is the raw type and the
        //requested generic type is a Representation with the following class
        //hierarchy:
        // Object
        //     -> Representation
        //         -> RdfRepresentation
        //         -> InMemoryRepresentation
        //     -> InputStream
        //     -> Collection<T>
        boolean typeOK = false;
        if(type instanceof Class<?>){
            typeOK = required.isAssignableFrom((Class<?>) type);
            type = ((Class<?>)type).getGenericSuperclass();
        } else if(type instanceof WildcardType){
            //In cases <? super {class}>, <? extends {class}, <?>
            WildcardType wildcardSetType = (WildcardType) type;
            if(wildcardSetType.getLowerBounds().length > 0){
                Type lowerBound = wildcardSetType.getLowerBounds()[0];
                //OK
                //  Set<? super RdfRepresentation>
                //  Set<? super Representation>
                //NOT OK
                //  Set<? super InputStream>
                //  Set<? super Collection<Representation>>
                typeOK = lowerBound instanceof Class<?> &&
                    required.isAssignableFrom((Class<?>)lowerBound);
            } else if (wildcardSetType.getUpperBounds().length > 0){
                Type upperBound = wildcardSetType.getUpperBounds()[0];
                //OK
                //  Set<? extends Representation>
                //  Set<? extends Object>
                //NOT OK
                //  Set<? extends RdfRepresentation>
                //  Set<? extends InputStream>
                //  Set<? extends Collection<Representation>
                typeOK = upperBound instanceof Class<?> &&
                    ((Class<?>)upperBound).isAssignableFrom(required); 
            } else { //no upper nor lower bound
                // Set<?>
                typeOK = true;
            }
        } else if(required.isArray() && type instanceof GenericArrayType){
            //In case the required type is an array we need also to support 
            //possible generic Array specifications
            GenericArrayType arrayType = (GenericArrayType)type;
            typeOK = testType(required.getComponentType(), arrayType.getGenericComponentType());
        } else if(type instanceof ParameterizedType){
            ParameterizedType pType = ((ParameterizedType)type);
            typeOK = pType.getRawType() instanceof Class<?> && 
                    required.isAssignableFrom((Class<?>)pType.getRawType());
            type = null;
        } else {
            typeOK = false;
        }
        return typeOK;
    }
    /**
     * Tests the parsed type against the raw type and parsed Type parameters.
     * This allows e.g. to check for <code>Map&lt;String,Number&gt</code> but
     * also works with classes that extend generic types such as
     * <code>Dummy extends {@link HashMap}&lt;String,String&gt</code>.
     * @param rawType the raw type to test against
     * @param parameterTypes the types of the parameters
     * @param type the type to test
     * @return if the type is compatible or not
     */
    public static boolean testParameterizedType(Class<?> rawType, Class<?>[] parameterTypes, Type type) {
        // first check the raw type
        if (!testType(rawType, type)) {
            return false;
        }
        while (type != null) {
            Type[] parameters = null;
            if (type instanceof ParameterizedType) {
                parameters = ((ParameterizedType) type).getActualTypeArguments();
                // the number of type arguments MUST BE the same as parameter types
                if (parameters.length == parameterTypes.length) {
                    boolean compatible = true;
                    // All parameters MUST BE compatible!
                    for (int i = 0; compatible && i < parameters.length; i++) {
                        compatible = testType(parameterTypes[i], parameters[i]);
                    }
                    if (compatible) {
                        return true;
                    }
                } // else check parent types

            } // else not parameterised
            if (type instanceof Class<?>) {
                type = ((Class<?>) type).getGenericSuperclass();
            } else {
                return false;
            }
        }
        return false;
    }
}
