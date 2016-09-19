package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.*;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusSummary;
import histori.model.support.SearchSortOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

import static histori.ApiConstants.*;
import static histori.model.support.EntityVisibility.everyone;
import static histori.model.support.SearchSortOrder.up_vote;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.EMPTY_ARRAY;
import static org.cobbzilla.util.string.StringUtil.formatDurationFrom;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(SEARCH_ENDPOINT)
@Service @Slf4j
public class SearchResource {

    @Autowired private NexusSummaryDAO nexusSummaryDAO;
    @Autowired private NexusDAO nexusDAO;
    @Autowired private SearchQueryDAO searchQueryDAO;
    @Autowired private PreferredOwnerDAO preferredOwnerDAO;
    @Autowired private BlockedOwnerDAO blockedOwnerDAO;
    @Autowired private RedisService redisService;

    private static final long SEARCH_CACHE_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(1);

    @Getter(lazy=true) private final RedisService searchCache = initSearchCache();
    private RedisService initSearchCache() { return redisService.prefixNamespace(SearchResource.class.getName(), null); }

    @POST
    @Path(EP_QUERY)
    public Response search(@Context HttpContext ctx,
                           @Valid SearchQuery query){

        final Account account = optionalUserPrincipal(ctx);

        // it must pass validation and be anonymously recorded in order to proceed
        searchQueryDAO.create(new SearchQuery(query));

        return search(account, query);
    }

    @GET
    @Path(EP_QUERY +"/{from}/{to}/{north}/{south}/{east}/{west}")
    public Response search(@Context HttpContext ctx,
                           @PathParam("from") String from,
                           @PathParam("to") String to,
                           @PathParam("north") double north,
                           @PathParam("south") double south,
                           @PathParam("east") double east,
                           @PathParam("west") double west,
                           @QueryParam("q") String query,
                           @QueryParam("a") String authoritative,
                           @QueryParam("v") String visibility,
                           @QueryParam("c") String useCache,
                           @QueryParam("t") long timeout) {

        final Account account = optionalUserPrincipal(ctx);

        final SearchQuery q = new SearchQuery()
                .setQuery(query)
                .setAuthoritative(empty(authoritative) || !authoritative.equals("false"))
                .setVisibility(EntityVisibility.create(visibility, everyone))
                .setUseCache(empty(useCache) || !useCache.equalsIgnoreCase("false"))
                .setRange(from, to)
                .setBounds(north, south, east, west)
                .setTimeout(timeout);

        if (account != null) {
            q.setPreferredOwners(preferredOwnerDAO.findActiveUuidsByOwner(account));
            q.setBlockedOwners(blockedOwnerDAO.findActiveUuidsByOwner(account));
        }

        // it must pass validation and be anonymously recorded in order to proceed
        searchQueryDAO.create(new SearchQuery(q));

        return search(account, q);
    }

    private Response search(Account account, SearchQuery searchQuery) {

        if (empty(searchQuery.getQuery())) return ok(EMPTY_ARRAY);

        long start = now();
        final EntityVisibility visibility = searchQuery.hasVisibility() ? searchQuery.getVisibility() : EntityVisibility.everyone;
        final boolean isPublic = visibility.isEveryone();
        final String cacheKey = (account == null || isPublic ? "null" : account.getUuid()) + ":" + searchQuery.hashCode();
        final String json = searchQuery.isUseCache() ? getSearchCache().get(cacheKey) : null;
        SearchResults<NexusSummary> results = null;
        if (!empty(json)) {
            try {
                results = fromJson(json, SearchResults.jsonType(NexusSummary.class));
            } catch (Exception e) {
                log.error("Error reading JSON under cache key ("+cacheKey+"): "+json+": "+e);
                getSearchCache().del(cacheKey);
            }
        }

        if (results == null) {
            log.info("STARTING FULL search("+searchQuery.getQuery()+")...");
            try {
                results = nexusSummaryDAO.search(account, searchQuery);
            } catch (Exception e) {
                log.error("Error searching ("+searchQuery.getQuery()+", duration "+formatDurationFrom(start)+"): "+e);
                return serverError();
            }
            try {
                getSearchCache().set(cacheKey, toJson(results), "EX", SEARCH_CACHE_TIMEOUT_SECONDS);
            } catch (Exception e) {
                log.error("Error encoding JSON for search cache: "+results+": "+e);
            }
        }
        log.info("FULL search("+searchQuery.getQuery()+"): returning "+results.getResults().size()+" results after "+formatDurationFrom(start));
        return ok(results);
    }

    // todo: support preferred/blocked owners. maybe do this as a POST with a SearchQuery? or a superclass?
    @GET
    @Path(EP_NEXUS+"/{uuid}")
    public Response findByUuid(@Context HttpContext ctx,
                               @PathParam("uuid") String uuid,
                               @QueryParam("sort") String sortOrder,
                               @QueryParam("exact") boolean exact) {

        final Account account = optionalUserPrincipal(ctx);

        final Nexus nexus = nexusDAO.findByUuid(uuid);
        if (nexus == null) return notFound(uuid);

        final SearchSortOrder sort = SearchSortOrder.valueOf(sortOrder, up_vote);
        final NexusSummary summary = nexusSummaryDAO.search(account, nexus, null, sort);
        if (exact && !summary.getPrimary().getUuid().equals(uuid)) {
            summary.addOther(summary.getPrimary().getUuid());
            summary.setPrimary(nexus);
        }

        return summary != null ? ok(summary.setIncomplete(false)) : notFound(uuid);
    }

}
