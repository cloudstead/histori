package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.base.NexusTags;
import histori.model.tag_schema.TagSchemaValue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.mappy.MappySortedSet;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

@NoArgsConstructor @Accessors(chain=true)
public class NexusTag extends IdentifiableBase implements Comparable<NexusTag> {

    public static final Comparator<NexusTag> SORT_NAME = new Comparator<NexusTag>() {
        @Override public int compare(NexusTag t1, NexusTag t2) {
            int diff = t1.getCanonicalType().compareTo(t2.getCanonicalType());
            return diff != 0 ? diff : t1.getCanonicalName().compareTo(t2.getCanonicalName());
        }
    };

    public static final String[] VALUE_FIELDS = {"tagName", "tagType", "values"};

    public NexusTag(NexusTag other) { copy(this, other, VALUE_FIELDS); }

    @Size(max=NAME_MAXLEN, message="err.tagName.tooLong")
    @Getter @Setter private String tagName;
    public String getCanonicalName() { return canonicalize(getTagName()); }
    public void setCanonicalName(String name) {} // noop

    // denormalized
    @Size(max=NAME_MAXLEN, message="err.tagType.tooLong")
    @Getter @Setter private String tagType;
    public boolean hasTagType () { return !empty(tagType); }
    @Transient @JsonIgnore public String getCanonicalType () { return canonicalize(getTagType()); }

    @Getter @Setter private TagSchemaValue[] values;
    public boolean hasSchemaValues () { return !empty(values); }

    public NexusTag setValue(String field, String value) {
        if (values == null) {
            values = new TagSchemaValue[] { new TagSchemaValue(field, value) };
        } else {
            values = ArrayUtil.append(values, new TagSchemaValue(field, value));
        }
        return this;
    }

    @Override public String toString() {
        if (hasSchemaValues()) {
            return getTagType() + "/" + getTagName() + "(schema:"+getSchemaValueMap()+")";
        } else {
            return getTagType() + "/" + getTagName();
        }
    }

    @JsonIgnore @Transient public SchemaValueMap getSchemaValueMap() {
        final SchemaValueMap map = new SchemaValueMap();
        if (!hasSchemaValues()) return map;
        map.addAll(values);
        return map;
    }

    @JsonIgnore @Transient public String getSchemaHash() { return getSchemaValueMap().getHash(); }

    public static List<NexusTag> filterByType(NexusTags tags, String type) {
        final List<NexusTag> found = new ArrayList<>();
        if (!empty(tags)) {
            for (NexusTag tag : tags) {
                if (tag.getTagType().equalsIgnoreCase(type)) found.add(tag);
            }
        }
        return found;
    }

    public static boolean containsTypeTag(List<NexusTag> tags, String type) {
        if (!empty(tags)) {
            for (NexusTag tag : tags) if (tag.getTagType().equalsIgnoreCase(type)) return true;
        }
        return false;
    }

    public boolean isSameTag(NexusTag tag) {
        if (!canonicalize(getTagName()).equals(canonicalize(tag.getTagName()))) return false;
        if (!canonicalize(getTagType()).equals(canonicalize(tag.getTagType()))) return false;
        // blank schema values matches any other tag
        return !hasSchemaValues() || !tag.hasSchemaValues() || getSchemaHash().equals(tag.getSchemaHash());
    }

    public class SchemaValueMap extends MappySortedSet<String, String> {

        public void addAll(TagSchemaValue[] values) {
            for (TagSchemaValue val : values) {
                put(val.getField(), val.getValue());
            }
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof SchemaValueMap)) return false;

            final SchemaValueMap other = (SchemaValueMap) o;

            if (other.size() != this.size()) return false;

            for (String field : this.keySet()) {
                final Set<String> values = this.getAll(field);
                final Set<String> otherVals = other.getAll(field);

                if (values.size() != otherVals.size()) return false;
                for (String val : values) if (!otherVals.contains(val)) return false;
                for (String val : otherVals) if (!values.contains(val)) return false;
            }

            for (String field : other.keySet()) {
                final Set<String> values = this.getAll(field);
                final Set<String> otherVals = other.getAll(field);

                if (values.size() != otherVals.size()) return false;
                for (String val : values) if (!otherVals.contains(val)) return false;
                for (String val : otherVals) if (!values.contains(val)) return false;
            }

            return true;
        }

        private static final String KSEP = "|||";
        private static final String VSEP = "@@@";

        @Override public int hashCode() { return getHash().hashCode(); }

        public String getHash() {
            final StringBuilder hash = new StringBuilder();
            for (String key : keySet()) {
                hash.append(KSEP).append(key);
                for (String value : getAll(key)) {
                    hash.append(VSEP).append(value);
                }
            }
            return sha256_hex(hash.toString());
        }
    }

    @Override public int compareTo(NexusTag o) {
        int diff = getCanonicalName().compareTo(o.getCanonicalName());
        if (diff != 0) return diff;
        diff = hasTagType() ? (o.hasTagType() ? getTagType().compareTo(o.getTagType()) : -1) : 1;
        if (diff != 0) return diff;
        return getSchemaHash().compareTo(o.getSchemaHash());
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NexusTag nexusTag = (NexusTag) o;

        if (!getCanonicalName().equals(nexusTag.getCanonicalName())) return false;
        if (tagType != null ? !tagType.equals(nexusTag.tagType) : nexusTag.tagType != null) return false;
        return getSchemaValueMap().equals(nexusTag.getSchemaValueMap());

    }

}
