package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.Nexus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.geojson.LineString;
import org.geojson.MultiPolygon;
import org.geojson.Point;
import org.geojson.Polygon;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@NoArgsConstructor @Accessors(chain=true)
public class NexusRequest extends Nexus {

    // exactly one of these should be filled out, or super.geoJson
    @Getter @Setter private Point point;
    @Getter @Setter private LineString line;
    @Getter @Setter private Polygon polygon;
    @Getter @Setter private MultiPolygon multiPolygon;

    @Override @JsonIgnore public String getGeoJson() {
        if (super.getGeoJson() != null) return super.getGeoJson();
        if (point != null) return toJsonOrDie(point);
        if (line != null) return toJsonOrDie(line);
        if (polygon != null) return toJsonOrDie(polygon);
        if (multiPolygon != null) return toJsonOrDie(multiPolygon);
        return die("getGeoJson: no geometry defined");
    }

    public boolean isDirty () { return false; }
    public void setDirty (boolean dirty) {} // noop

}
