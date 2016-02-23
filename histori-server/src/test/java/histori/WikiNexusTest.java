package histori;

import cloudos.service.asset.ResourceStorageService;
import histori.model.NexusTag;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.wiki.WikiArchive;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.math.Cardinal;
import org.geojson.Point;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.math.Cardinal.east;
import static org.cobbzilla.util.math.Cardinal.north;
import static org.cobbzilla.util.math.Cardinal.west;
import static org.junit.Assert.*;

@Slf4j
public class WikiNexusTest {

    private WikiArchive wiki = new WikiArchive(new ResourceStorageService("wiki/index"));

    public static TestPage[] TESTS = {
            // Test case: A very famous historical battle -- lots of tags to extract
            new TestPage("Battle of Actium")
                    .location(38, 56, 4, north, 20, 44, 19, east)
                    .range("-31-09-02")
                    .tag("event_type", "battle")
                    .tag("event", "Final War of the Roman Republic", "relationship", "part_of")
                    .tag("result", "Decisive Octavian victory")
                    .tag("world_actor", "Octavian's Roman and allied supporters and forces", "role", "combatant")
                    .tag("person", "Marcus Vipsanius Agrippa", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("person", "Lucius Arruntius", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("person", "Marcus Lurius", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("impact", "dead", "estimate", "2500", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("world_actor", "Mark Antony's Roman and allied supporters", "role", "combatant")
                    .tag("world_actor", "Ptolemaic Egypt", "role", "combatant")
                    .tag("person", "Mark Antony", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Gaius Sosius", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Marcus Octavius (admiral)", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Cleopatra VII", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("impact", "dead", "estimate" , "5000", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("impact", "ships sunk or captured", "estimate" , "200", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Actium"),

            // Test case: a more obscure battle, we must lookup another wiki page to determine the location
            new TestPage("Battle of Purandar")
                    .location(18, 17, north, 73, 59, east)
                    .range("1665")
                    .tag("event_type", "battle")
                    .tag("event", "Imperial Maratha Conquests", "relationship", "part_of")
                    .tag("result", "Mughal Victory. Shivaji surrenders.")
                    .tag("world_actor", "Maratha Empire", "role", "combatant")
                    .tag("person", "Shivaji", "role", "commander", "world_actor", "Maratha Empire")
                    .tag("person", "Murarbaji Deshpande", "role", "commander", "world_actor", "Maratha Empire")
                    .tag("world_actor", "Mughal Empire", "role", "combatant")
                    .tag("person", "Dilir Khan", "role", "commander", "world_actor", "Mughal Empire")
                    .tag("person", "Mirza Jai Singh", "role", "commander", "world_actor", "Mughal Empire")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Purandar"),

            // A page title with non-ASCII characters, and 'commanders' that contain special characters
            new TestPage("Battle of Świecino")
                    .location(54.787222, 18.087778)
                    .range("1462-09-17")
                    .tag("event_type", "battle")
                    .tag("event", "Thirteen Years' War (1454–66)", "relationship", "part_of")
                    .tag("result", "Decisive Polish Victory")
                    .tag("world_actor", "Teutonic Order", "role", "combatant")
                    .tag("person", "Fritz Raweneck", "role", "commander", "world_actor", "Teutonic Order")
                    .tag("person", "Kaspar Nostyc", "role", "commander", "world_actor", "Teutonic Order")
                    .tag("impact", "dead", "estimate", "1000", "world_actor", "Teutonic Order")
                    .tag("impact", "captured", "estimate", "50", "world_actor", "Teutonic Order")
                    .tag("world_actor", "Poland", "role", "combatant")
                    .tag("person", "Piotr Dunin", "role", "commander", "world_actor", "Poland")
                    .tag("impact", "dead", "estimate", "250", "world_actor", "Poland")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_%C5%9Awiecino"),

            // Another big battle with lots of data to extract and a very large document (>100KB)
            // Also a very complex infobox of combatants and commanders
            new TestPage("Battle of the Bulge")
                    .location(50, 15, north, 5, 40, east)
                    .range("1944-12-16", "1945-01-25")
                    .tag("event_type", "battle")
                    .tag("event", "World War II", "relationship", "part_of")
                    .tag("result", "Allied victory, German operational failure")
                    .tag("world_actor", "United States", "role", "combatant")
                    .tag("world_actor", "United Kingdom", "role", "combatant")
                    .tag("world_actor", "Canada", "role", "combatant")
                    .tag("world_actor", "France", "role", "combatant")
                    .tag("world_actor", "Belgium", "role", "combatant")
                    .tag("world_actor", "Luxembourg", "role", "combatant")
                    .tag("person", "Dwight D. Eisenhower", "role", "commander", "world_actor", "United States")
                    .tag("person", "Bernard Montgomery", "role", "commander", "world_actor", "United Kingdom")
                    .tag("person", "Omar Bradley", "role", "commander", "world_actor", "United States")
                    .tag("person", "Courtney Hodges", "role", "commander", "world_actor", "United States")
                    .tag("person", "George S. Patton", "role", "commander", "world_actor", "United States")
                    .tag("person", "Anthony McAuliffe", "role", "commander", "world_actor", "United States")
                    .tag("impact", "casualties", "estimate", "89500", "world_actor", "United States")
                    .tag("impact", "dead", "estimate", "19000", "world_actor", "United States")
                    .tag("impact", "wounded", "estimate", "47500", "world_actor", "United States")
                    .tag("impact", "captured or missing", "estimate", "23000", "world_actor", "United States")
                    .tag("impact", "tanks/assault guns destroyed", "low_estimate", "700", "estimate", "750", "high_estimate", "800", "world_actor", "United States")
                    .tag("impact", "aircraft destroyed", "estimate", "647", "world_actor", "United States")
                    .tag("impact", "casualties", "estimate", "1408", "world_actor", "United Kingdom")
                    .tag("impact", "dead", "estimate", "200", "world_actor", "United Kingdom")
                    .tag("impact", "wounded", "estimate", "969", "world_actor", "United Kingdom")
                    .tag("impact", "missing", "estimate", "239", "world_actor", "United Kingdom")
                    .tag("world_actor", "Nazi Germany", "role", "combatant")
                    .tag("person", "Adolf Hitler", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Walter Model", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Gerd von Rundstedt", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Hasso von Manteuffel", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Sepp Dietrich", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Erich Brandenberger", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("impact", "casualties", "low_estimate", "67459", "estimate", "96229", "high_estimate", "125000", "world_actor", "Nazi Germany")
                    .tag("impact", "dead", "estimate", "10749", "world_actor", "Nazi Germany")
                    .tag("impact", "wounded", "estimate", "34225", "world_actor", "Nazi Germany")
                    .tag("impact", "captured", "estimate", "22487", "world_actor", "Nazi Germany")
                    .tag("impact", "tanks/assault guns destroyed", "low_estimate", "600", "estimate", "700", "high_estimate", "800", "world_actor", "Nazi Germany")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_the_Bulge"),

            // Another one with coordinates that are difficult to find
            new TestPage("Battle of Peleliu", false)
                    .location(7, 0, north, 134, 15, east)
                    .range("1944-09-15", "1944-11-27")
                    .tag("event_type", "battle")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Peleliu"),

            new TestPage("Battle of Waterloo", false)
                    .location(50.68016, 4.41169)
                    .range("1815-06-18")
                    .tag("event_type", "battle")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Waterloo"),

            // Includes wikitables, increased parsing complexity
            new TestPage("Battle of Kadesh", false)
                    .location(34.57, 36.51)
                    .range("-1274-05")
                    .tag("event_type", "battle")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Kadesh"),

            // Uses {{start date}} info block for date, complex casualties stats
            new TestPage("Battle of the Crater", false)
                    .location(37.2183, -77.3777)
                    .range("1864-07-30")
                    .tag("event_type", "battle")
                    .tag("event", "American Civil War", "relationship", "part_of")
                    .tag("result", "Confederate States of America victory")
                    .tag("world_actor", "United States", "role", "combatant")
                    .tag("world_actor", "Union (American Civil War)", "role", "combatant")
                    .tag("impact", "casualties", "estimate", "3798", "world_actor", "United States", "world_actor", "Union (American Civil War)")
                    .tag("impact", "dead", "estimate", "504", "world_actor", "United States", "world_actor", "Union (American Civil War)")
                    .tag("impact", "wounded", "estimate", "1881", "world_actor", "United States", "world_actor", "Union (American Civil War)")
                    .tag("impact", "captured or missing", "estimate", "1413", "world_actor", "United States", "world_actor", "Union (American Civil War)")
                    .tag("world_actor", "Confederate States of America", "role", "combatant")
                    .tag("impact", "casualties", "estimate", "1491", "world_actor", "Confederate States of America")
                    .tag("impact", "dead", "estimate", "361", "world_actor", "Confederate States of America")
                    .tag("impact", "wounded", "estimate", "727", "world_actor", "Confederate States of America")
                    .tag("impact", "captured or missing", "estimate", "403", "world_actor", "Confederate States of America")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_the_Crater"),

            // Had a funky link that was being parsed as a commander, now fixed. Keep to avoid regressing
            new TestPage("Battle of Mortimer's Cross")
                    .location(52, 19, 7, north, 2, 52, 9, west)
                    .range("1461-02-02")
                    .tag("event_type", "battle")
                    .tag("event", "Wars of the Roses", "relationship", "part_of")
                    .tag("result", "Decisive Yorkist victory")
                    .tag("world_actor", "House of York", "role", "combatant")
                    .tag("person", "Edward IV of England", "world_actor", "House of York", "role", "commander")
                    .tag("world_actor", "House of Lancaster", "role", "combatant")
                    .tag("person", "Owen Tudor", "world_actor", "House of Lancaster", "role", "commander")
                    .tag("person", "Jasper Tudor", "world_actor", "House of Lancaster", "role", "commander")
                    .tag("person", "James Butler", "world_actor", "House of Lancaster", "role", "commander")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Mortimer%27s_Cross"),

            // Had a Portuguese flag icon identified as a world_actor, also uses West cardinality, testing geo coordinate parsing
            new TestPage("Battle of Roliça")
                    .location(39.3136, -9.1836)
                    .range("1808-08-17")
                    .tag("event_type", "battle")
                    .tag("event", "Peninsular War", "relationship", "part_of")
                    .tag("result", "Anglo-Portuguese victory, tactical French retreat")
                    .tag("world_actor", "United Kingdom", "role", "combatant")
                    .tag("world_actor", "United Kingdom of Great Britain and Ireland", "role", "combatant")
                    .tag("world_actor", "Portugal", "role", "combatant")
                    .tag("world_actor", "Kingdom of Portugal", "role", "combatant")
                    .tag("person", "Arthur Wellesley", "role", "commander", "world_actor", "United Kingdom")
                    .tag("impact", "dead and wounded", "estimate", "487", "world_actor", "United Kingdom", "world_actor", "United Kingdom of Great Britain and Ireland", "world_actor", "Portugal", "world_actor", "Kingdom of Portugal")
                    .tag("world_actor", "France", "role", "combatant")
                    .tag("world_actor", "First French Empire", "role", "combatant")
                    .tag("world_actor", "Switzerland", "role", "combatant")
                    .tag("world_actor", "Swiss Confederation (Napoleonic)", "role", "combatant")
                    .tag("person", "Henri Delaborde", "role", "commander", "world_actor", "France")
                    .tag("impact", "dead and wounded", "estimate", "700", "world_actor", "France", "world_actor", "First French Empire", "world_actor", "Switzerland", "world_actor", "Swiss Confederation (Napoleonic)")
                    .tag("impact", "guns captured", "estimate", "3", "world_actor", "France", "world_actor", "First French Empire", "world_actor", "Switzerland", "world_actor", "Swiss Confederation (Napoleonic)")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Roli%C3%A7a"),

            // fixing bug with casualty parsing
            new TestPage("Battle of Chusto-Talasah", false)
                    .location(36.2823, -95.9502)
                    .range("1861-12-09")
                    .tag("impact", "dead", "estimate", "9", "world_actor", "Creek (people)", "world_actor", "Seminole")
                    .tag("impact", "casualties", "estimate", "500", "world_actor", "Creek (people)", "world_actor", "Seminole"),

            // detection of "damaged" casualty that references previous line. complex casualty logic.
            new TestPage("Battle of Britain Day", false)
                    .tag("impact", "aircraft destroyed", "estimate", "29", "world_actor", "United Kingdom")
                    .tag("impact", "aircraft damaged", "estimate", "21", "world_actor", "United Kingdom")
                    .tag("impact", "dead", "low_estimate", "14", "estimate", "15", "high_estimate", "16", "world_actor", "United Kingdom")
                    .tag("impact", "wounded", "estimate", "14", "world_actor", "United Kingdom")
                    .tag("impact", "captured", "estimate", "1", "world_actor", "United Kingdom")
                    .tag("impact", "dead", "low_estimate", "63", "estimate", "72", "high_estimate", "81", "world_actor", "Nazi Germany", "world_actor", "Germany")
                    .tag("impact", "aircraft destroyed", "low_estimate", "57", "estimate", "59", "high_estimate", "61", "world_actor", "Nazi Germany", "world_actor", "Germany")
                    .tag("impact", "aircraft damaged", "estimate", "20", "world_actor", "Nazi Germany", "world_actor", "Germany")
                    .tag("impact", "captured", "low_estimate", "63", "estimate", "64", "high_estimate", "65", "world_actor", "Nazi Germany", "world_actor", "Germany")
                    .tag("impact", "wounded", "low_estimate", "30", "estimate", "30", "high_estimate", "31", "world_actor", "Nazi Germany", "world_actor", "Germany")
                    .tag("impact", "missing", "estimate", "21", "world_actor", "Nazi Germany", "world_actor", "Germany"),

            new TestPage("Naval Battle of Guadalcanal", false)
                    .tag("world_actor", "United States", "role", "combatant")
                    .tag("world_actor", "Empire of Japan", "role", "combatant")
                    .tag("impact", "light cruisers sunk", "estimate", "2", "world_actor", "United States")
                    .tag("impact", "destroyers sunk", "estimate", "3", "world_actor", "United States")
                    .tag("impact", "destroyers sunk", "estimate", "4", "world_actor", "United States")
                    .tag("impact", "aircraft destroyed", "estimate", "36", "world_actor", "United States")
                    .tag("impact", "dead", "estimate", "1732", "world_actor", "United States")
                    .tag("impact", "battleships sunk", "estimate", "1", "world_actor", "Empire of Japan")
                    .tag("impact", "destroyers sunk", "estimate", "2", "world_actor", "Empire of Japan")
                    .tag("impact", "transports lost", "estimate", "7", "world_actor", "Empire of Japan")
                    .tag("impact", "battleships sunk", "estimate", "1", "world_actor", "Empire of Japan")
                    .tag("impact", "destroyers sunk", "estimate", "1", "world_actor", "Empire of Japan")
                    .tag("impact", "transports lost", "estimate", "4", "world_actor", "Empire of Japan")
                    .tag("impact", "aircraft destroyed", "estimate", "64", "world_actor", "Empire of Japan")
                    .tag("impact", "dead", "estimate", "1900", "world_actor", "Empire of Japan"),

            new TestPage("Battle of Short Hills", false)
                    .tag("person", "William Alexander (American general)", "role", "commander", "world_actor", "United States")
    };

    @Test public void testNexusCreationFromWiki() throws Exception {
        validateCorrectNexus(TESTS[TESTS.length-1]);
//        validateCorrectNexus(TESTS[2]);
        for (TestPage test : TESTS) {
            validateCorrectNexus(test);
        }
    }

    public void validateCorrectNexus(TestPage test) {
        final NexusRequest nexusRequest = wiki.toNexusRequest(test.title);
        assertNotNull("error parsing article: "+test.title, nexusRequest);
        if (test.location != null) assertEquals(test.getGeoJson(), nexusRequest.getGeoJson());
        if (test.range != null) assertEquals(test.range, nexusRequest.getTimeRange());
        if (test.fullCheck) assertEquals("wrong # of tags for "+test.title, test.tags.size(), nexusRequest.getTagCount());
        for (NexusTag tag : test.tags) {
            assertTrue("missing tag: "+tag.getTagType()+"/"+tag.getTagName(), nexusRequest.hasTag(tag.getTagName()));
            assertTrue("tag doesn't match: "+tag.getTagName(), nexusRequest.hasExactTag(tag));
        }
    }

    private static class TestPage {

        public String title;
        public boolean fullCheck;

        public TestPage (String title) { this(title, true); }
        public TestPage (String title, boolean fullCheck) { this.title = title; this.fullCheck = fullCheck; }

        public LatLon location;
        public TestPage location (LatLon location) { this.location = location; return this; }
        public TestPage location (double lat, double lon) { return location(new LatLon(lat, lon)); }
        public TestPage location (int latDeg, Integer latMin, Integer latSec, Cardinal latDir, int lonDeg, Integer lonMin, Integer lonSec, Cardinal lonDir) {
            this.location = new LatLon(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir);
            return this;
        }
        public TestPage location (int latDeg, Integer latMin, Cardinal latDir, int lonDeg, Integer lonMin, Cardinal lonDir) {
            return location(latDeg, latMin, null, latDir, lonDeg, lonMin, null, lonDir);
        }
        public String getGeoJson() { return toJsonOrDie(new Point(location.getLon(), location.getLat())); }

        public TimeRange range;
        public TestPage range(String date) { this.range = new TimeRange(date); return this; }
        public TestPage range(String start, String end) { this.range = new TimeRange(start, end); return this; }

        public List<NexusTag> tags = new ArrayList<>();
        public TestPage tag(String tagType, String tagName) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1, String f2, String v2) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1, String f2, String v2, String f3, String v3) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2).setValue(f3, v3));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1, String f2, String v2, String f3, String v3, String f4, String v4) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2).setValue(f3, v3).setValue(f4, v4));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1, String f2, String v2, String f3, String v3, String f4, String v4, String f5, String v5) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2).setValue(f3, v3).setValue(f4, v4).setValue(f5, v5));
            return this;
        }
    }
}