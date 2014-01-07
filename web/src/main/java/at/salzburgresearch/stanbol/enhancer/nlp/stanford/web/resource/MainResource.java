package at.salzburgresearch.stanbol.enhancer.nlp.stanford.web.resource;

import java.io.InputStream;
import java.net.URLConnection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class MainResource {

    private Logger log = LoggerFactory.getLogger(MainResource.class);
    
    @GET
    public Response getHomepage(){
        return streamResource("index.html",
            MainResource.class.getClassLoader().getResourceAsStream("index.html"),
            MediaType.TEXT_HTML_TYPE);
    }

    @GET
    @Path("static/{path}")
    public Response getStaticResource(@PathParam("path") String path){
        String resource = "static/"+path;
        String mediaType = URLConnection.guessContentTypeFromName(FilenameUtils.getName(path));
        return streamResource(resource,
            MainResource.class.getClassLoader().getResourceAsStream("static/"+path),
            mediaType != null ? MediaType.valueOf(mediaType) : null);
    }

    /**
     * @param in
     * @return
     */
    private Response streamResource(String resource, final InputStream in, MediaType mediaType) {
        if(in == null){
            log.warn("requested resource '{}' not found", resource);
            return Response.status(Status.NOT_FOUND).build();
        } else {
            return Response.ok(in,mediaType).build();
        }
    }
    
    
}
