package histori.resources;

import edu.emory.mathcs.backport.java.util.Collections;
import histori.dao.AccountDAO;
import histori.dao.TagDAO;
import histori.model.Account;
import histori.model.CanonicalEntity;
import histori.model.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(TAGS_ENDPOINT)
@Service @Slf4j
public class TagsResource {

    public static final String TYPE_DELIM = "~";

    @Autowired private TagDAO tagDAO;
    @Autowired private AccountDAO accountDAO;

    @GET
    @Path(EP_TAG + "/{name: .+}")
    public Response findTag (@PathParam("name") String name) {
        if (empty(name) || name.length() > NAME_MAXLEN) return notFound();
        final Tag found = tagDAO.findByCanonicalName(name);
        return found != null ? ok(found) : notFound(name);
    }

    @POST
    @Path(EP_RESOLVE)
    public Response findTags (String[] names) {
        if (names.length > 100) return invalid("err.names.tooMany");
        if (names.length == 0) return ok(Collections.emptyList());
        return ok(tagDAO.findByCanonicalNames(names));
    }

    @GET
    @Path(EP_OWNER+"/{uuid}")
    public Response findOwner (@PathParam("uuid") String uuid) {
        final Account found = accountDAO.findByUuid(uuid);
        return found == null ? notFound(uuid) : ok(found.getName());
    }

    @GET
    @Path(EP_AUTOCOMPLETE)
    public Response autocomplete (@QueryParam(QPARAM_AUTOCOMPLETE) String query) {
        return autocomplete(query, null);
    }

    @GET
    @Path(EP_AUTOCOMPLETE+"/{tagType}")
    public Response autocomplete (@QueryParam(QPARAM_AUTOCOMPLETE) String query,
                                  @PathParam("tagType") String matchType) {
        final String canonical = CanonicalEntity.canonicalize(query);
        return ok(tagDAO.findByCanonicalNameStartsWith(canonical, matchType));
    }
}
