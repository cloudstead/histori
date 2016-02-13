package atrox.main.wiki;

import atrox.model.support.TimePoint;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.reflect.ReflectionUtil.get;
import static org.cobbzilla.util.reflect.ReflectionUtil.set;
import static org.cobbzilla.util.string.StringUtil.uncapitalize;

public class WikiDateFormat {


    // the second entry in each array is the field mask, tells us which fields we can use from the joda-time
    public static final String[][] FORMATS = {
            {"yyyy-MM-dd", "yMd"},
            {"dd MMM yyyy", "yMd"},
            {"dd MMMM yyyy", "yMd"},
            {"MMM yyyy", "yM"},
            {"MMM yyyy 'BC'", "yM"},
            {"MMM yyyy 'B.C.'", "yM"},
            {"MMM yyyy 'BCE'", "yM"},
            {"MMM yyyy 'B.C.E.'", "yM"},
            {"MMMM yyyy", "yM"},
            {"MMMM dd yyyy", "yMd"},
            {"MMMM dd, yyyy", "yMd"},

            {"'Summer' yyyy", "y"},
            {"'Fall' yyyy", "y"},
            {"'Autumn' yyyy", "y"},
            {"'Winter' yyyy", "y"},
            {"'Spring' yyyy", "y"},

            {"'Summer' yyyy 'BC'", "y"},
            {"'Fall' yyyy 'BC'", "y"},
            {"'Autumn' yyyy 'BC'", "y"},
            {"'Winter' yyyy 'BC'", "y"},
            {"'Spring' yyyy 'BC'", "y"},

            {"'Early' yyyy", "y"},
            {"'Early' yyyy 'BC'", "y"},
            {"'Late' yyyy", "y"},
            {"'Late' yyyy 'BC'", "y"},

            {"yyyy", "y"},
            {"yyyy 'CE'", "y"},
            {"yyyy 'C.E.'", "y"},
            {"yyyy 'AD'", "y"},
            {"yyyy 'A.D.'", "y"}
    };
    public static final String MATCH_DAY = "(\\d{1,2})";
    public static final String MATCH_YEAR= "(\\d{1,4})";

    public static final String MATCH_MONTH
            = "(January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|July|Jul|August|Aug|September|Sep|October|Oct|November|Nov|December|Dec)";

    public static final String SPACE = "\\s+";
    public static final String ANY_SPACES = "\\s*";

    public static final Object[][] RANGE_PATTERNS = {{
            Pattern.compile(MATCH_DAY + ANY_SPACES + "[-–]" + ANY_SPACES + MATCH_DAY + SPACE + MATCH_MONTH + ",?" + SPACE + MATCH_YEAR),
            "startDay", "endDay", "startMonth", "startYear"
    }, {
            Pattern.compile(MATCH_MONTH + SPACE + MATCH_DAY + ANY_SPACES + "[-–]" + ANY_SPACES + MATCH_DAY + ",?" + SPACE + MATCH_YEAR),
            "startMonth", "startDay", "endDay", "startYear"
    }, {
            Pattern.compile(MATCH_MONTH + ANY_SPACES + "or" + ANY_SPACES + MATCH_MONTH + "?," + ANY_SPACES + MATCH_YEAR),
            "startMonth", null, "startYear"
    }};

    private static DateTimeFormatter[] DATE_FORMATTERS;
    static {
        DATE_FORMATTERS = new DateTimeFormatter[FORMATS.length];
        for (int i=0; i<FORMATS.length; i++) {
            DATE_FORMATTERS[i] = DateTimeFormat.forPattern(FORMATS[i][0]);
        }
    }

    public static TimePoint[] parse(String input) {

        final String date = scrub(input);
        final boolean bce = isBce(date);

        final TimePoint[] result = new TimePoint[2];

        for (Object[] rangePattern : RANGE_PATTERNS) {
            final Pattern pattern = (Pattern) rangePattern[0];
            final Matcher matcher = pattern.matcher(date);
            if (!matcher.matches()) continue;

            final TimePoint start = result[0] = new TimePoint();
            final TimePoint end = result[1] = new TimePoint();
            final Set<String> wroteToStart = new HashSet<>();
            final Set<String> wroteToEnd = new HashSet<>();
            for (int i=1; i<rangePattern.length; i++) {
                String field = (String) rangePattern[i];
                if (field == null) continue;
                TimePoint target;
                if (field.startsWith("end")) {
                    target = end;
                    field = uncapitalize(field.substring("end".length()));
                    wroteToEnd.add(field);

                } else {
                    target = start;
                    if (field.startsWith("start")) field = uncapitalize(field.substring("start".length()));
                    wroteToStart.add(field);
                }

                if (field.equals("year")) {
                    set(target, field, Long.parseLong(matcher.group(i)));
                    if (bce) target.invertYear();

                } else if (field.equals("month")) {
                    final Byte month = parseMonth(matcher.group(i));
                    if (month != null) set(target, field, month);

                } else {
                    set(target, field, Byte.parseByte(matcher.group(i)));
                }
            }
            // if we wrote fields to start, but not to end, copy their values from start -> end
            for (String field : wroteToStart) {
                if (!wroteToEnd.contains(field) || get(end, field) == null) {
                    set(end, field, get(start, field));
                }
            }

            start.initInstant();
            end.initInstant();
            return result;
        }

        for (int i=0; i<DATE_FORMATTERS.length; i++) {
            final DateTimeFormatter formatter = DATE_FORMATTERS[i];
            try {
                result[0] = new TimePoint();
                DateTime dateTime = formatter.parseDateTime(date);
                final String fieldMask = FORMATS[i][1];
                for (char c : fieldMask.toCharArray()) {
                    switch (c) {
                        case 'y':
                            result[0].setYear(dateTime.getYear());
                            if (bce) result[0].invertYear();
                            break;
                        case 'M': result[0].setMonth((byte) dateTime.getMonthOfYear()); break;
                        case 'd': result[0].setDay((byte) dateTime.getDayOfMonth()); break;
                        case 'h': result[0].setHour((byte) dateTime.getHourOfDay()); break;
                        case 'm': result[0].setMinute((byte) dateTime.getMinuteOfHour()); break;
                        case 's': result[0].setSecond((byte) dateTime.getSecondOfMinute()); break;
                        default: die("invalid field mask value ("+fieldMask+"): "+c);
                    }
                }
                return result;

            } catch (Exception ignored) {
                // noop
            }
        }
        return die("parse: no formats matched: "+input);
    }

    private static Byte parseMonth(String month) {
        switch (month.trim().toLowerCase()) {
            case "january": case "jan": return 1;
            case "february": case "feb": return 2;
            case "march": case "mar": return 3;
            case "april": case "apr": return 4;
            case "may": return 5;
            case "june": case "jun": return 6;
            case "july": case "jul": return 7;
            case "august": case "aug": return 8;
            case "september": case "sep": return 9;
            case "october": case "oct": return 10;
            case "november": case "nov": return 11;
            case "december": case "dec": return 12;
            default: return null;
        }
    }

    public static boolean isBce(String date) {
        date = date.replace(".", "").trim();
        return date.endsWith("BC") || date.endsWith("BCE") || date.endsWith("B.C.") || date.endsWith("B.C.E.");
    }

    private static String scrub(String date) {
        int pos = date.indexOf("<");
        if (pos != -1) return date.substring(0, pos).trim();
        pos = date.indexOf("{{");
        if (pos != -1) return date.substring(0, pos).trim();
        return date.trim();
    }

}
