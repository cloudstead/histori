package histori.wiki.finder.impl;

import histori.model.NexusTag;
import histori.model.base.NexusTags;
import histori.model.support.NexusRequest;
import histori.model.support.RelationshipType;
import histori.model.support.RoleType;
import histori.wiki.WikiNode;
import histori.wiki.WikiNodeType;
import histori.wiki.finder.FinderBase;
import histori.wiki.finder.InfoboxNames;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.SingletonSet;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static histori.model.TagType.EVENT_TYPE;
import static histori.wiki.finder.impl.ConflictFinder.CommanderParseState.seeking_commander;
import static histori.wiki.finder.impl.ConflictFinder.CommanderParseState.seeking_flag;
import static histori.wiki.finder.impl.ConflictParticipant.commander;

@Slf4j
public class ConflictFinder extends FinderBase<NexusRequest> {

    public static final String HTML_TAG_REGEX = "(<|&lt;)\\s*\\w+\\s*/?\\s*(>|&gt;)";
    public static final String HTML_BR_REGEX = "(<|&lt;)\\s*([Bb][Rr])\\s*/?\\s*(>|&gt;)";
    public static final Pattern HTML_BR_PATTERN = Pattern.compile(".*"+HTML_BR_REGEX+".*");
    public static final Pattern HTML_CENTER_PATTERN = Pattern.compile(".*(<|&lt;)\\s*center\\s*\\s*(>|&gt;).+?(<|&lt;)\\s*/center\\s*\\s*(>|&gt;).*", Pattern.MULTILINE|Pattern.DOTALL);

    public static final int MIN_VALID_NAME_LENGTH = 3;

    @Override public NexusRequest find() {

        final NexusRequest request = new NexusRequest();
        if (!addStandardTags(wiki, request)) return null;

        final List<NexusTag> tags = new ArrayList<>();
        final WikiNode infobox = article.findFirstInfoboxWithName(InfoboxNames.INFOBOX_MILITARY_CONFLICT);
        if (infobox == null) return request;

        NexusTag tag;
        tags.add(newTag("battle", EVENT_TYPE));

        if (infobox.hasChildNamed("partof")) {
            WikiNode partof = infobox.findChildNamed("partof");
            List<WikiNode> links = partof.getLinks();
            String tagName;
            if (links.isEmpty()) {
                tagName = partof.findAllChildTextButNotLinkDescriptions();
            } else {
                tagName = links.get(0).getName();
            }
            if (tagName != null && tagName.trim().length() > 0) {
                tags.add(newTag(tagName, "event", "relationship", RelationshipType.part_of.name()));
            }
        }

        if (infobox.hasChildNamed("result")) {
            final String result = trimToFirstLine(infobox.findFirstAttributeWithName("result").findAllChildTextButNotLinkDescriptions());
            if (result != null) tags.add(newTag(result, "result"));
        }

        for (WikiNode child : infobox.getChildren()) {
            if (child.getType() == WikiNodeType.attribute) {
                if (child.getName().startsWith("combatant")) {
                    final Set<String> combatants = parseCombatants(child);
                    for (String combatant : combatants) {
                        tags.add(newTag(combatant.trim(), "world_actor", "role", RoleType.combatant.name()));
                    }

                    final WikiNode commanderNode = infobox.findFirstAttributeWithName(child.getName().replace("combatant", "commander"));
                    if (commanderNode != null) {
                        final Set<ConflictParticipant> commanders = parseCommanders(commanderNode);
                        for (ConflictParticipant commander : commanders) {
                            if (commander.isValidName()) {
                                tag = newTag(commander.name, "person", "role", RoleType.commander.name());
                                if (commander.side != null) {
                                    tag.setValue("world_actor", commander.side);
                                } else {
                                    addCombatants(tag, combatants);
                                }
                                tags.add(tag);
                            }
                        }
                    }

                    final WikiNode casualtiesNode = infobox.findFirstAttributeWithName(child.getName().replace("combatant", "casualties"));
                    if (casualtiesNode != null) {
                        if (casualtiesNode.hasSinglePlainlistChild()) {
                            final List<WikiNode> entries = casualtiesNode.findFirstWithType(WikiNodeType.plainlist).getChildren();
                            String activeFlag = null;
                            for (WikiNode plEntry : entries) {
                                if (plEntry.getType().isPlainlistHeader()) {
                                    activeFlag = getFlagName(plEntry);

                                } else if (plEntry.getType().isPlainlistEntry()) {
                                    final String casualty = plEntry.firstChildName();
                                    WikiNode ref = plEntry.findFirstInfoboxWithName("efn");
                                    if (ref == null) ref = plEntry.findFirstInfoboxWithName("sfn");
                                    if (ref != null && hasCasualties(ref)) {
                                        Long[] estimate1 = extractEstimate(casualty);
                                        if (estimate1 != null) {
                                            String continuationAfterRef = plEntry.getChildren().get(2).getName().trim();
                                            if (continuationAfterRef.startsWith("}}")) continuationAfterRef = continuationAfterRef.substring(2).trim();
                                            if (plEntry.getChildren().size() >= 3
                                                    && (continuationAfterRef.startsWith("-")|| continuationAfterRef.startsWith("–"))) {
                                                if (extractEstimate(continuationAfterRef) != null) {
                                                    addImpactTag(tags, impactTag(activeFlag, estimate1[0].toString() + continuationAfterRef, null));
                                                }
                                            }
                                        }
                                        for (WikiNode refAttr : ref.getChildren()) {
                                            String refName = refAttr.getName();
                                            if (isValidCasualty(refName)) {
                                                addImpactTag(tags, impactTag(activeFlag, refName));
                                            }
                                        }

                                    } else if (casualty.contains("(") && casualty.contains(")") && casualty.indexOf("(") < casualty.indexOf(")")) {
                                        addImpactTag(tags, impactTag(activeFlag, casualty, "casualties")); // main casualty tag
                                        String[] parts = casualty.substring(casualty.indexOf('(') + 1, casualty.indexOf(')')).split(",");
                                        for (String part : parts) {
                                            addImpactTag(tags, impactTag(activeFlag, part.trim()));
                                        }

                                    } else {
                                        addImpactTag(tags, impactTag(activeFlag, casualty));
                                    }
                                }
                            }
                        } else {
                            final String childTextWithoutLinkDescriptions = casualtiesNode.findAllChildTextButNotLinkDescriptions();
                            if (childTextWithoutLinkDescriptions != null) {
                                final String[] casualties = childTextWithoutLinkDescriptions.split(HTML_BR_REGEX);
                                int validCasualties = 0;
                                for (String casualty : casualties) if (isValidCasualty(casualty)) validCasualties++;
                                final String defaultCasualtyType = validCasualties <= 1 ? "dead" : null;
                                String lastCasualty = null;
                                boolean first = true;
                                for (String casualty : casualties) {
                                    String ctype = getCasualtyType(casualty, lastCasualty);
                                    if (ctype == null) {
                                        if (first) {
                                            ctype = validCasualties <= 1 ? "dead" : "casualties";
                                        } else if (validCasualties <= 1) {
                                            ctype = defaultCasualtyType;
                                        }
                                    }
                                    final NexusTag impactTag = impactTag(combatants, casualty, ctype);
                                    if (impactTag != null) {
                                        lastCasualty = casualty;
                                        tags.add(impactTag);
                                    }
                                    first = false;
                                }
                            }
                        }
                    }
                }
            }
        }

        request.setTags(new NexusTags(tags));
        return request;
    }

    private void addImpactTag(List<NexusTag> tags, NexusTag nexusTag) { if (nexusTag != null) tags.add(nexusTag); }

    private boolean hasCasualties(WikiNode ref) {
        if (!ref.hasChildren()) return false;
        for (WikiNode child : ref.getChildren()) {
            final String casualty = child.getName();
            if (isValidCasualty(casualty)) return true;
        }
        return false;
    }

    private boolean isValidCasualty(String casualty) {
        return extractEstimate(casualty) != null && getCasualtyType(casualty, null) != null;
    }

    public NexusTag impactTag(String activeFlag, String casualty) {
        return impactTag(activeFlag, casualty, getCasualtyType(casualty));
    }

    public NexusTag impactTag(String activeFlag, String casualty, String casualtyType) {
        final Long[] estimate = extractEstimate(casualty);
        if (casualtyType == null) casualtyType = getCasualtyType(casualty);
        return newImpactTag(new SingletonSet<>(activeFlag), estimate, casualtyType);
    }

    public NexusTag impactTag(Set<String> activeFlags, String casualty, String casualtyType) {
        final Long[] estimate = extractEstimate(casualty);
        if (estimate == null) return null;
        return newImpactTag(activeFlags, estimate, casualtyType);
    }

    private String getCasualtyType(String casualty) { return getCasualtyType(casualty, "dead"); }

    private String getCasualtyType(String casualty, String last) {

        final String c = removeTags(casualty).toLowerCase().trim();

        if (c.contains("killed and wounded") || c.contains("wounded and killed")) return "dead and wounded";
        if (c.contains("dead and wounded") || c.contains("wounded and dead")) return "dead and wounded";
        if (c.contains("killed") || c.contains("dead")) return "dead";
        if (c.contains("wounded") || c.contains("injured")) return "wounded";
        if (c.contains("deserted")) return "deserted";
        if (c.contains("ships sunk or captured") || c.contains("ships captured or sunk")) return "ships sunk or captured";
        if (c.contains("ships sunk")) return "ships sunk";
        if (c.contains("ships captured")) return "ships captured";
        if (c.contains("aircraft lost") || c.contains("aircraft destroyed") || c.contains("aircraft")) return "aircraft destroyed";
        if (c.contains("captured or missing") || c.contains("missing or captured")) return "captured or missing";
        if (c.contains("guns captured")) return "guns captured";
        if (c.contains("captured")) return "captured";
        if (c.contains("missing")) return "missing";
        if (c.contains("tanks and assault guns destroyed") || c.contains("assault guns and tanks destroyed")) return "tanks/assault guns destroyed";
        if (c.contains("tanks destroyed")) return "tanks destroyed";
        if (c.contains("assault guns destroyed")) return "assault guns destroyed";
        if (c.contains("light cruiser")) return "light cruisers sunk";
        if (c.contains("destroyer")) return "destroyers sunk";
        if (c.contains("battleship")) return "battleships sunk";
        if (c.contains("transport")) return "transports lost";
        if (c.contains("casualties") || c.endsWith("total")) return "casualties";
        if (c.contains("damaged") && last != null) return getFirstNonNumberWord(removeTags(last)) + " damaged";
        return null;
    }

    private String getFirstNonNumberWord(String line) {
        Matcher matcher = Pattern.compile("\\.*?[\\d,]+\\s+([-–][\\d,]+\\s+)?(\\w+)").matcher(line);
        if (matcher.find()) {
            String group = matcher.group(2);
            return group;
        }
        return null;
    }

    public String getFlagName(WikiNode node) {
        WikiNode flag;

        flag = node.findFirstWithName(WikiNodeType.infobox, "flag");
        if (flag != null) return flag.firstChildName();

        flag = node.findFirstWithName(WikiNodeType.infobox, "flagcountry");
        if (flag != null) return flag.firstChildName();

        flag = node.findFirstWithName(WikiNodeType.infobox, "flagicon");
        if (flag != null) return flag.firstChildName();

        flag = node.findFirstWithName(WikiNodeType.infobox, "flagicon image");
        if (flag != null) {
            boolean found = false;
            final List<WikiNode> siblings = article.getParent(node).getChildren();
            for (WikiNode c : siblings) {
                if (found) {
                    if (c.getType().isLink()) return c.firstChildName();
                    return c.findAllChildText();

                } else if (c != flag) continue;
                found = true;
            }
        }
        if (flag != null) return flag.firstChildName();

        return node.findAllChildText();
    }

    public String trimToFirstLine(String result) {
        if (result == null) return null;
        int pos = result.indexOf("\n");
        if (pos != -1) result = result.substring(0, pos);
        pos = result.toLowerCase().indexOf("<");
        if (pos != -1) result = result.substring(0, pos);
        pos = result.toLowerCase().indexOf("&lt;");
        if (pos != -1) result = result.substring(0, pos);
        return result;
    }

    public Set<String> parseCombatants(WikiNode targetNode) {

        final Set<String> found = new LinkedHashSet<>();
        if (targetNode == null || !targetNode.hasChildren()) return found;

        // group tags separated by a <br>
        final List<List<WikiNode>> nodeGroups = new ArrayList<>();
        List<WikiNode> activeGroup = new ArrayList<>();
        nodeGroups.add(activeGroup);

        for (WikiNode child : targetNode.getChildren()) {
            if (child.getType().isString()
                    && hasLineTerminator(child)
                    && !activeGroup.isEmpty()) {
                activeGroup = new ArrayList<>();
                nodeGroups.add(activeGroup);
            }
            activeGroup.add(child);
            if (child.getType().isString() && (child.getName().trim().endsWith("\n*") || HTML_BR_PATTERN.matcher(child.getName().trim().toLowerCase()).matches())) {
                activeGroup = new ArrayList<>();
                nodeGroups.add(activeGroup);
            }
        }
        // search over node groups, max 1 world_actor per group
        for (List<WikiNode> nodeGroup : nodeGroups) {
            final Set<String> foundInGroup = new HashSet<>();
            for (WikiNode child : nodeGroup) {
                switch (child.getType()) {
                    case string:
                        // detect and skip HTML comments
                        String trimmed = child.getName().trim();
                        String trimmedLower = trimmed.toLowerCase();
                        if (HTML_CENTER_PATTERN.matcher(trimmedLower).matches()) continue;
                        for (String combatant : child.getName().split(HTML_TAG_REGEX)) {
                            if (trimName(combatant).length() > 0) foundInGroup.add(combatant.trim());
                        }
                        break;

                    case link:
                        String name = child.getName().toLowerCase().trim();
                        if (!name.startsWith("file:") && !name.startsWith("image:")) {
                            for (String combatant : child.getName().split(HTML_TAG_REGEX)) {
                                if (trimName(combatant).length() > 0) foundInGroup.add(combatant.trim());
                            }
                        }
                        break;

                    case plainlist:
                        if (!child.hasChildren()) continue;
                        for (WikiNode entry : child.getChildren()) {
                            if (entry.getType() == WikiNodeType.plainlist_entry) {
                                String flagName = getFlagName(entry);
                                addCombatant(foundInGroup, flagName);
                            }
                        }
                        break;

                    case infobox:
                        if (child.getName().equalsIgnoreCase("plainlist")) {
                            for (WikiNode nestedChild : child.getChildren()) {
                                if (nestedChild.getName().equals("flag")) continue;
                                addCombatant(foundInGroup, getFlagName(nestedChild));
                                break;
                            }
                        } else if (child.hasChildren()
                                && (child.getName().equalsIgnoreCase("flag")
                                || child.getName().equalsIgnoreCase("flagcountry")
                                || child.getName().equalsIgnoreCase("flagicon")
                                || child.getName().equalsIgnoreCase("flagicon image"))) {
                            addCombatant(foundInGroup, getFlagName(child));
                        }
                }
                if (!foundInGroup.isEmpty()) break;
            }
            found.addAll(foundInGroup);
        }
        return found;
    }

    public boolean hasLineTerminator(WikiNode child) {
        return HTML_BR_PATTERN.matcher(child.getName()).matches() || child.getName().trim().matches("\\*+");
    }

    public String addCombatant(Set<String> found, String flagText) {
        if (flagText == null) return null;
        flagText = flagText.trim();
        if (flagText.toLowerCase().startsWith("flag of")) flagText = flagText.substring("flag of".length());
        int dotPos = flagText.indexOf('.');
        if (dotPos != -1 && dotPos > flagText.length() - 6) flagText = flagText.substring(0, dotPos);
        if (flagText.length() < 2) return null; // todo: log this?
        flagText = flagText.trim();
        found.add(flagText);
        return flagText;
    }

    enum CommanderParseState {
        seeking_commander, seeking_flag
    }
    public Set<ConflictParticipant> parseCommanders(WikiNode targetNode) {
        final Set<ConflictParticipant> found = new LinkedHashSet<>();
        if (targetNode == null || !targetNode.hasChildren()) return found;
        boolean skippingComment = false;
        String side = null;
        boolean foundFlags = false;
        CommanderParseState state = seeking_commander;
        for (WikiNode child : targetNode.getChildren()) {
            switch (child.getType()) {
                case string:
                    // detect and skip HTML comments
                    if (child.getName().trim().startsWith("&lt;!--")) {
                        skippingComment = true; continue;
                    }
                    if (child.getName().trim().endsWith("--&gt;")) {
                        skippingComment = false; continue;
                    }
                    if (skippingComment) continue;
                    if (foundFlags && state != seeking_commander) continue;
                    for (String name : child.getName().split(HTML_TAG_REGEX)) {
                        if (trimName(name).length() > 0 && !(name.startsWith("(") && name.endsWith(")"))) {
                            found.add(commander(name, side));
                            if (foundFlags) state = seeking_flag;
                        }
                    }
                    break;

                case link:
                    if (foundFlags && state != seeking_commander) continue;
                    String name = child.getName().toLowerCase().trim();

                    // If the link is all-chars, this is not a person
                    if (child.hasChildren() && isAllNonWordChars(child.firstChildName())) continue;

                    if (!name.startsWith("file:") && !name.startsWith("image:")) {
                        for (String subname : child.getName().split(HTML_TAG_REGEX)) {
                            if (trimName(subname).length() > 0) {
                                found.add(commander(subname, side));
                                if (foundFlags) state = seeking_flag;
                            }
                        }
                    }
                    break;

                case plainlist:
                    log.info("in plainlist"); break;
                case plainlist_entry:
                    log.info("in plainlist_entry"); break;

                case infobox:
                    if (child.getName().equalsIgnoreCase("plainlist")) {
                        for (WikiNode nestedChild : child.getChildren()) {
                            if (nestedChild.getName().equals("flag")) {
                                foundFlags = true;
                                state = seeking_commander;
                                side = nestedChild.getName().trim();

                            } else {
                                found.add(commander(nestedChild.getName(), side));
                                if (foundFlags) state = seeking_flag;
                            }
                            break;
                        }

                    } else if ((child.getName().equalsIgnoreCase("flag") || child.getName().equalsIgnoreCase("flagicon")) && child.hasChildren()) {
                        foundFlags = true;
                        state = seeking_commander;
                        side = child.firstChildName().trim();
                    }

                default: continue;
            }
        }
        return found;
    }

    private boolean isAllNonWordChars(String name) {
        return trimName(name).length() == 0 && name.trim().length() > 0;
    }

    private String trimName(String name) {
        return name == null ? "" : name.replaceAll("\\W", "").toLowerCase().trim();
    }

    public NexusTag newImpactTag(Set<String> combatants, Long[] estimate, String tagName) {
        if (estimate == null || estimate.length == 0 || tagName == null) return null; // todo: log this?
        final NexusTag tag;
        switch (estimate.length) {
            case 1:
                tag = newTag(tagName, "impact", "estimate", estimate[0].toString());
                break;
            case 2:
                tag = newTag(tagName, "impact", "low_estimate", estimate[0].toString());
                tag.setValue("high_estimate", estimate[1].toString());
                tag.setValue("estimate", String.valueOf(((estimate[0] + estimate[1]) / 2L)));
                break;
            default:
                log.warn("newTagImpact: invalid estimate: "+estimate);
                return null;
        }
        return addCombatants(tag, combatants);
    }

    public NexusTag addCombatants(NexusTag tag, Set<String> combatants) {
        for (String combatant : combatants) {
            if (combatant == null) continue;
            tag.setValue("world_actor", combatant.trim());
        }
        return tag;
    }

    public static final Pattern FIND_NUMBER_PATTERN = Pattern.compile("\\s*([,\\d]+)(\\s*[-–]\\s*([,\\d]+))?\\s*");

    private Long[] extractEstimate(String val) {
        val = removeTags(val);
        final Matcher m = FIND_NUMBER_PATTERN.matcher(val);
        if (!m.find()) return null;
        try {
            if (m.groupCount() >= 3) {
                if (m.group(2) == null) {
                    return new Long[]{estimateToLong(m.group(1))};
                } else {
                    return new Long[]{estimateToLong(m.group(1)), estimateToLong(m.group(3))};
                }
            } else {
                log.warn("extractEstimate: error handling '"+val+"'");
                return null;
            }
        } catch (Exception e) {
            log.warn("Error parsing ("+val+"): "+e);
            return null;
        }
    }

    private String removeTags(String val) {
        String s1 = val.replaceAll("&lt;\\s*.+?\\s*&gt;.*?&lt;/.+?\\s*&gt;", " ");
        String s2 = s1.replaceAll("&lt;.+?&gt;", " ");
        return s2;
    }

    private long estimateToLong(String group) {
        return Long.parseLong(group.trim().replace(",", ""));
    }
}
