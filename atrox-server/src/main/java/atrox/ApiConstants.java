package atrox;

import atrox.model.*;
import atrox.model.internal.EntityPointer;
import atrox.model.tags.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.string.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class ApiConstants {

    // must match what is in api.js
    public static final String API_TOKEN = "x-atrox-api-key";

    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String MAP_ENTITIES_ENDPOINT = "/map";

    public static String entityEndpoint(Class clazz) { return MAP_ENTITIES_ENDPOINT+"/"+clazz.getSimpleName(); }

    public static final String ERR_ALREADY_LOGGED_IN = "err.alreadyLoggedIn";

    public static final String PLACEHOLDER_MOBILE_PHONE = "8005550100";
    public static final String ANONYMOUS_EMAIL = "anonymous-#STAMP#@example.com";

    // auth endpoints
    public static final String EP_REGISTER = "/register";
    public static final String EP_LOGIN = "/login";

    // world data model sub-endpoints
    public static final String EP_BY_DATE = "/find/by_date";
    public static final String EP_AUTOCOMPLETE = "/autocomplete";

    // bound used when searching by date
    public static final String BOUND_RANGE = "range";
    public static final String RANGE_SEP = "|";

    public static String anonymousEmail() {
        return ANONYMOUS_EMAIL.replace("#STAMP#", RandomStringUtils.randomAlphanumeric(10)+"-"+System.currentTimeMillis());
    }


    public static final Class[] NAMED_ENTITIES = {
            Citation.class, EffectType.class, EventType.class,
            Ideology.class, WorldActor.class, WorldEvent.class
    };

    public static final Class[] TAG_ENTITIES = {
            CitationTag.class, EventActorTag.class, EventEffectTag.class, EventTypeTag.class,
            IdeologyTag.class, WorldActorTag.class, WorldEventTag.class
    };

    public static final Class[] ALL_MODELS = ArrayUtil.concat(NAMED_ENTITIES, TAG_ENTITIES, new Class[]{EntityPointer.class} );

    public static final Map<String, Class> ENTITY_CLASS_MAP = new HashMap<>();
    static {
        for (Class c : ALL_MODELS) {
            ENTITY_CLASS_MAP.put(c.getSimpleName(), c);
            ENTITY_CLASS_MAP.put(StringUtil.uncapitalize(c.getSimpleName()), c);
            ENTITY_CLASS_MAP.put(c.getName(), c);
        }
        ENTITY_CLASS_MAP.put("any", EntityPointer.class);
    }

}
