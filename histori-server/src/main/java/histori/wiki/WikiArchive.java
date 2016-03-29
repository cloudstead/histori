package histori.wiki;

import cloudos.service.asset.AssetStorageService;
import cloudos.service.asset.AssetStream;
import com.amazonaws.util.StringInputStream;
import histori.model.support.MultiNexusRequest;
import histori.model.support.NexusRequest;
import histori.wiki.finder.FinderFactory;
import histori.wiki.finder.MultiNexusFinder;
import histori.wiki.finder.WikiDataFinder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.util.string.StringUtil;
import se.walkercrou.places.GooglePlaces;

import java.util.ArrayList;
import java.util.List;

import static histori.model.CanonicalEntity.canonicalize;
import static histori.wiki.WikiNode.wikiLink;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@AllArgsConstructor @Slf4j
public class WikiArchive {

    private static final Integer SYNOPSIS_MAX_CHARS = 2000;

    private final AssetStorageService storage;

    @Getter @Setter private String placesApiKey;
    public boolean hasPlacesApiKey () { return !empty(placesApiKey); }

    @Getter(lazy=true) private final GooglePlaces placesApi = initPlacesApi();
    private GooglePlaces initPlacesApi() { return hasPlacesApiKey() ? new GooglePlaces(getPlacesApiKey()) : null; }

    public static final String[] SKIP_INDEX_PREFIXES = { "Template:", "File:", "Wikipedia:" };

    public boolean exists (WikiArticle article) {
        final String articlePath = getArticlePath(article.getTitle());
        return articlePath != null && storage.exists(articlePath);
    }

    public void store(WikiArticle article) throws Exception {
        final String articlePath = getArticlePath(article.getTitle());
        if (articlePath == null) {
            // log.info("refusing to index: "+article.getTitle());
            return;
        }
        storage.store(new StringInputStream(toJsonOrDie(article)), articlePath, articlePath);
    }

    public ParsedWikiArticle find (String title) { return find(title, true); }

    public ParsedWikiArticle find (String title, boolean followRedirects) {
        final WikiArticle article = findUnparsed(title);
        if (article == null) return null;

        final ParsedWikiArticle parsed = article.parse();
        if (followRedirects && parsed.hasChildren() && parsed.firstChildName().equals("#REDIRECT")) {
            final WikiNode firstLink = parsed.findFirstWithType(WikiNodeType.link);
            if (firstLink == null) return null;
            return find(firstLink.getName());
        }
        return parsed;
    }

    public WikiArticle findUnparsed (String title) {
        AssetStream stream = null;
        try {
            final String articlePath = getArticlePath(title);
            if (articlePath == null) return null;

            stream = storage.load(articlePath);
            if (stream == null) return null;
            return fromJson(stream.getStream(), WikiArticle.class);

        } catch (Exception e) {
            log.warn("find: "+e);

        } finally {
            if (stream != null) try { stream.close(); } catch (Exception ignored) {}
        }
        return null;
    }

    private static boolean isIndexable(String title) {
        title = title.toLowerCase().trim();
        for (String skip : SKIP_INDEX_PREFIXES) if (title.startsWith(skip.toLowerCase())) return false;
        return true;
    }

    public static String getArticlePath(String title) { return getArticlePath(title, null); }

    public static String getArticlePath(String title, String variant) {
        if (!isIndexable(title)) return null;
        final String sha256 = ShaUtil.sha256_hex(title);
        return sha256.substring(0, 2)
                + "/" + sha256.substring(2, 4)
                + "/" + sha256.substring(4, 6)
                + "/" + StringUtil.truncate(canonicalize(title), 100) + "_" + sha256 + (variant == null ? "" : "_"+variant) + ".json";
    }

    public NexusRequest toNexusRequest(String title) {
        return toNexusRequest(title, new ArrayList<String>());
    }

    public NexusRequest toNexusRequest(String title, List<String> disposition) {
        final WikiArticle article = findUnparsed(title);
        return article == null ? null : toNexusRequest(article, disposition);
    }

    public NexusRequest toNexusRequest(WikiArticle article) { return toNexusRequest(article, new ArrayList<String>()); }

    public NexusRequest toNexusRequest(WikiArticle article, List<String> disposition) {

        final ParsedWikiArticle parsed = article.parse();

        if (article.getText().toLowerCase().startsWith("#redirect")) {
            final List<WikiNode> links = parsed.getLinks();
            if (!empty(links)) {
                final String target = links.get(0).getName();
                String msg = "toNexusRequest: " + article.getTitle() + " is a redirect, following: " + target;
                log.info(msg);
                disposition.add(msg);
                return toNexusRequest(target, disposition);
            } else {
                String msg = "toNexusRequest: " + article.getTitle() + " is a redirect, but could not determine target: " + article.getText();
                log.warn(msg);
                disposition.add(msg);
                return null;
            }
        }

        final WikiDataFinder finder = FinderFactory.build(this, parsed);
        if (finder != null) {
            if (finder instanceof MultiNexusFinder) {
                final List<NexusRequest> requests = ((MultiNexusFinder) finder).find();
                if (empty(requests)) return null;

                final MultiNexusRequest multi = new MultiNexusRequest();
                for (NexusRequest request : requests) {
                    multi.add(finalize(request, parsed));
                }
                return multi;

            } else {
                final NexusRequest nexusRequest = (NexusRequest) finder.find();
                if (nexusRequest == null || !nexusRequest.hasName()) return null;
                return finalize(nexusRequest, parsed);
            }
        }

        return null;
    }

    public NexusRequest finalize(NexusRequest nexusRequest, ParsedWikiArticle parsed) {
        // Did we detect an event_type?
        if (!nexusRequest.hasNexusType()) nexusRequest.setNexusType(nexusRequest.getFirstEventType());
        addCitation(nexusRequest, parsed.getName());
        nexusRequest.setMarkdown(parsed.toMarkdown(SYNOPSIS_MAX_CHARS));
        return nexusRequest;
    }

    public NexusRequest addCitation(NexusRequest nexusRequest, String title) {
        if (empty(title)) return null;
        nexusRequest.addTag(wikiLink(title), "citation");
        return nexusRequest;
    }

}
