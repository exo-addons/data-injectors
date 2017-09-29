package org.exoplatform.services.injection.ws;

import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Path("tribe/ping/")
public class PingREST implements ResourceContainer {

    protected static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

    /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
    protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";



    @GET
    @Path("inject")
    @RolesAllowed("administrators")
    public Response inject(@Context SecurityContext sc, @Context UriInfo uriInfo) throws Exception {

        DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
        return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();

    }

}
