package histori.main;

import histori.main.internal.*;
import histori.main.wiki.ArticleNexusMain;
import histori.main.wiki.ArticlePathMain;
import histori.main.wiki.WikiIndexerMain;
import histori.main.wiki.WikiTitleIndexMain;
import histori.server.HistoriServer;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.main.ConfigurationDataBagMain;
import org.cobbzilla.wizard.main.MainBase;

import java.util.Map;

// todo: add "--help" and "-h" commands, to display help for all commands or a specific command
public class HistoriMain {

    private static Map<String, Class<? extends MainBase>> mainClasses = MapBuilder.build(new Object[][]{
            {"server",            HistoriServer.class},
            {"index",             WikiIndexerMain.class},
            {"index-titles",      WikiTitleIndexMain.class},
            {"path",              ArticlePathMain.class},
            {"nexus",             ArticleNexusMain.class},
            {"feed",              FeedMain.class},
            {"import",            NexusImportMain.class},
            {"shard-update",      ShardUpdateMain.class},
            {"shard-bulk-update", ShardBulkUpdateMain.class},
            {"shard-list",        ShardListMain.class},
            {"shard-remove",      ShardRemoveMain.class},
            {"shard-gen-sql",     ShardSqlGenMain.class},
            {"data-bag",          ConfigurationDataBagMain.class}
    });

    public static void main(String[] args) throws Exception {

        if (args.length == 0) die("No command provided, use one of: "+ StringUtil.toString(mainClasses.keySet(), " "));

        // extract command
        final String command = args[0];
        final Class mainClass = mainClasses.get(command.toLowerCase());
        if (mainClass == null) die("Command not found: "+command+", use one of: "+StringUtil.toString(mainClasses.keySet(), " "));

        // shift
        final String[] newArgs = new String[args.length-1];
        System.arraycopy(args, 1, newArgs, 0, args.length-1);

        // invoke "real" main
        mainClass.getMethod("main", String[].class).invoke(null, (Object) newArgs);
    }

    private static void die(String s) {
        System.out.println(s);
        System.exit(1);
    }
}
