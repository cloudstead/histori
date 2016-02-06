package atrox.model.tags;

import atrox.model.support.GeoPolygon;
import atrox.model.support.TimePoint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Accessors(chain=true)
public class WorldEventTag extends EntityTag {

    public static final String[] UNIQUES
            = {"worldEvent", "startPoint.instant", "endPoint.instant", "polygon.coordinates"};

    @Override public String[] getUniqueProperties() { return UNIQUES; }

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldEvent;

    @AttributeOverrides({
            @AttributeOverride(name="instant",column=@Column(name="startInstant")),
            @AttributeOverride(name="year",column=@Column(name="startYear")),
            @AttributeOverride(name="month",column=@Column(name="startMonth")),
            @AttributeOverride(name="day",column=@Column(name="startDay")),
            @AttributeOverride(name="hour",column=@Column(name="startHour")),
            @AttributeOverride(name="minute",column=@Column(name="startMinute")),
            @AttributeOverride(name="second",column=@Column(name="startSecond")),
    })
    @Embedded
    @Getter @Setter private TimePoint startPoint;

    @AttributeOverrides({
            @AttributeOverride(name="instant",column=@Column(name="endInstant")),
            @AttributeOverride(name="year",column=@Column(name="endYear")),
            @AttributeOverride(name="month",column=@Column(name="endMonth")),
            @AttributeOverride(name="day",column=@Column(name="endDay")),
            @AttributeOverride(name="hour",column=@Column(name="endHour")),
            @AttributeOverride(name="minute",column=@Column(name="endMinute")),
            @AttributeOverride(name="second",column=@Column(name="endSecond")),
    })
    @Embedded
    @Getter @Setter private TimePoint endPoint;

    @Embedded
    @Getter @Setter private GeoPolygon polygon;

}