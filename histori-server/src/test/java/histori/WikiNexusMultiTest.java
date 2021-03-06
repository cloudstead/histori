package histori;

import histori.model.support.MultiNexusRequest;
import histori.model.support.NexusRequest;
import org.junit.Test;

import static org.cobbzilla.util.math.Cardinal.north;
import static org.cobbzilla.util.math.Cardinal.west;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class WikiNexusMultiTest extends WikiTest {

    static ArticleNexusExpectedResult[] TESTS = {

            new ArticleNexusExpectedResult("London")

                .nexus(new ArticleNexusExpectedResult("London Settled by Roman Empire")
                    .location(51, 30, 26, north, 0, 7, 39, west)
                    .range("43")
                    .tag("event_type", "founding")
                    .tag("world_actor", "Roman Empire", "role", "founder")
                    .tag("citation", "https://en.wikipedia.org/wiki/London")),

            new ArticleNexusExpectedResult("San Francisco")

                    .nexus(new ArticleNexusExpectedResult("San Francisco mission")
                            .location(37, 47, north, 122, 25, west)
                            .range("1776-06-29")
                            .tag("event_type", "founding")
                            .tag("citation", "https://en.wikipedia.org/wiki/San_Francisco"))

                    .nexus(new ArticleNexusExpectedResult("San Francisco incorporated")
                    .location(37, 47, north, 122, 25, west)
                    .range("1850-04-15")
                    .tag("event_type", "founding")
                    .tag("citation", "https://en.wikipedia.org/wiki/San_Francisco")),

            new ArticleNexusExpectedResult("Glasnevin")
                    .nexus(new ArticleNexusExpectedResult("Glasnevin founded")
                    .location(53.371859, -6.267357)
                    .range("500")
                    .tag("event_type", "founding")
                    .tag("citation", "https://en.wikipedia.org/wiki/Glasnevin")),

            new ArticleNexusExpectedResult("Albion, Michigan")
                    .nexus(new ArticleNexusExpectedResult("Albion, Michigan settled")
                    .location(42, 14, 48, north, 84, 45, 12, west)
                    .range("1833")
                    .tag("event_type", "founding")
                    .tag("citation", "https://en.wikipedia.org/wiki/Albion%2C_Michigan")),

            new ArticleNexusExpectedResult("Paris")
                    .nexus(new ArticleNexusExpectedResult("Paris founded")
                    .location(48.8567, 2.3508)
                    .range("200")
                    .tag("event_type", "founding")
                    .tag("citation", "https://en.wikipedia.org/wiki/Paris")),

            new ArticleNexusExpectedResult("New York City")
                    .nexus(new ArticleNexusExpectedResult("New York City settled")
                    .location(40.7127, north, 74.0059, west)
                    .range("1624")
                    .tag("event_type", "founding")
                    .tag("citation", "https://en.wikipedia.org/wiki/New_York_City"))

                    .nexus(new ArticleNexusExpectedResult("New York City consolidated")
                    .location(40.7127, north, 74.0059, west)
                    .range("1898")
                    .tag("event_type", "founding")
                    .tag("citation", "https://en.wikipedia.org/wiki/New_York_City")),
    };

    @Test public void testNexusCreationFromWiki() throws Exception {
//        validateCorrectNexuses(TESTS[TESTS.length-1]);
//        validateCorrectNexuses(TESTS[9]);
        for (ArticleNexusExpectedResult test : TESTS) {
            validateCorrectNexuses(test);
        }
    }

    public void validateCorrectNexuses(ArticleNexusExpectedResult test) {

        final NexusRequest nexusRequest = wiki.toNexusRequest(test.title);
        assertNotNull(nexusRequest);
        if (!(nexusRequest instanceof MultiNexusRequest)) fail("Expected a "+MultiNexusRequest.class.getName());

        final MultiNexusRequest multi = (MultiNexusRequest) nexusRequest;
        for (NexusRequest request : multi.getRequests()) {
            ArticleNexusExpectedResult expectedRequest = test.getExpectedRequest(request.getName());
            assertNotNull("Result expected for: "+request.getName(), expectedRequest);
            expectedRequest.verify(request);
        }
    }


}
