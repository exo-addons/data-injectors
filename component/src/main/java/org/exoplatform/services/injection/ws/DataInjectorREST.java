package org.exoplatform.services.injection.ws;

import org.exoplatform.services.injection.DataInjector;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.RuntimeDelegate;

@Path("tribe/onboarding/data/")
public class DataInjectorREST implements ResourceContainer {
    private static final Log log = ExoLogger.getLogger(DataInjectorREST.class);

    private static final CacheControl cacheControl_;
    private DataInjector dataInjector;

    static {
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
        cacheControl_ = new CacheControl();
        cacheControl_.setNoCache(true);
        cacheControl_.setNoStore(true);
    }

    public DataInjectorREST(DataInjector dataInjector) {
        this.dataInjector = dataInjector;

    }

    @GET
    @Path("inject")
    @RolesAllowed("administrators")
    public Response inject(@Context SecurityContext sc, @Context UriInfo uriInfo) throws Exception {

        try {
            this.dataInjector.inject();

        } catch (Exception e) {
            log.error("Data Injection Failed", e);
            return Response.ok("Errors happens, can not inject fake data as expected " , MediaType.APPLICATION_JSON).cacheControl(cacheControl_).build();
        }
        return Response.ok("Data has been injected successfully" , MediaType.APPLICATION_JSON).cacheControl(cacheControl_).build();
    }

    @GET
    @Path("purge")
    @RolesAllowed("administrators")
    public Response purge(@Context SecurityContext sc,@Context UriInfo uriInfo) throws Exception {


        try {
            this.dataInjector.purge();
        } catch (Exception e) {
            log.error("Data purging failed", e);
            return Response.ok("Errors happens, can not purge fake data as expected " , MediaType.APPLICATION_JSON).cacheControl(cacheControl_).build();
        }
        return Response.ok("Data has been purged successfully" , MediaType.APPLICATION_JSON).cacheControl(cacheControl_).build();
    }
}
