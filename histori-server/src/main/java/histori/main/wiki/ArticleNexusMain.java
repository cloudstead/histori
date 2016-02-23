package histori.main.wiki;

import histori.model.support.NexusRequest;
import histori.wiki.WikiArticle;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileSuffixFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static histori.wiki.WikiArchive.getArticlePath;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.listFiles;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class ArticleNexusMain extends MainBase<ArticleNexusOptions> {

    public static void main(String[] args) { main(ArticleNexusMain.class, args); }

    @Override protected void run() throws Exception {

        final ArticleNexusOptions options = getOptions();

        String input = options.getInput();
        if (input == null) {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    nexus(line);
                } catch (Exception e) {
                    err("Error processing: "+line+": "+e);
                }
            }
        } else {
            nexus(input);
        }
    }

    private void nexus(String input) throws Exception {

        input = input.trim(); // no sane input would have leading/trailing whitespace, but a line from a file or stdin just might. play it safe.

        WikiArticle article;
        final ArticleNexusOptions options = getOptions();
        final File file = new File(input);

        if (!file.exists()) {
            // maybe it is an article name?
            article = options.getWikiArchive().findUnparsed(input);
            if (article != null) {
                writeNexus(article);
            } else {
                err("Article not found: "+input+", path: "+getArticlePath(input));
            }
            return;
        }

        if (file.isDirectory()) {
            // import all json files in directory, if they are valid WikiArticle json files
            for (File articleJson : listFiles(file, new FileSuffixFilter(".json"))) {
                try {
                    article = fromJson(FileUtil.toString(articleJson), WikiArticle.class);
                    writeNexus(article);

                } catch (Exception e) {
                    err("Error importing " + abs(articleJson) + ": " + e);
                }
            }
        } else {
            // import a single file
            article = fromJson(FileUtil.toString(file), WikiArticle.class);
            writeNexus(article);
        }
    }

    private void writeNexus(WikiArticle article) {

        final ArticleNexusOptions options = getOptions();
        final File outputFile = getOutputFile(article.getTitle());
        if (outputFile.exists()) {
            if (!options.isOverwrite()) {
                err("writeNexus: article file exists, not overwriting: "+ outputFile);
                return;
            }
        }

        final NexusRequest nexusRequest = options.getWikiArchive().toNexusRequest(article);
        if (nexusRequest == null) {
            err("writeNexus: Error building NexusRequest");
            return;
        }

        final File outputDir = options.getOutputDir();
        if (outputDir != null && !outputDir.isDirectory()) die("Output directory does not exist or is not a directory: "+abs(outputDir));

        String title = nexusRequest.getName();
        try {
            final String nexusJson = toJson(nexusRequest);
            if (outputDir != null) {
                final File out = getOutputFile(title);
                final File parent = out.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) die("Error creating parent dir: "+abs(parent));
                FileUtil.toFile(out, nexusJson);
                out("\nWROTE: "+abs(out));
            } else {
                out("\n----------\n" + nexusJson);
            }

        } catch (Exception e) {
            err("Error processing article: "+ title +": "+e);
        }
    }

    private boolean articleFileExists (String title) {
        final String path = getArticlePath(title);
        return path != null && new File(path).exists();
    }

    private File getOutputFile(String title) {
        final String path = getArticlePath(title, "nexus");
        if (path == null) die("Cannot save: "+ title);
        return new File(abs(getOptions().getOutputDir()) + "/" + path);
    }

}
