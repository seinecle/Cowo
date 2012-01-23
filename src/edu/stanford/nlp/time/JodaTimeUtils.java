package edu.stanford.nlp.time;

import org.joda.time.*;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.field.DividedDateTimeField;
import org.joda.time.field.OffsetDateTimeField;
import org.joda.time.field.RemainderDateTimeField;
import org.joda.time.field.ScaledDurationField;

import java.util.HashSet;
import java.util.Set;

/**
 * Extensions to joda time
 *
 * @author Angel Chang
 */
public class JodaTimeUtils {
  // Standard ISO fields
  public static final Chronology isoUTCChronology = ISOChronology.getInstanceUTC();
  public static final DateTimeFieldType[] standardISOFields = new DateTimeFieldType[] {
          DateTimeFieldType.year(),
          DateTimeFieldType.monthOfYear(),
          DateTimeFieldType.dayOfMonth(),
          DateTimeFieldType.hourOfDay(),
          DateTimeFieldType.minuteOfHour(),
          DateTimeFieldType.secondOfMinute(),
          DateTimeFieldType.millisOfSecond()
  };
  public static final DateTimeFieldType[] standardISOWeekFields = new DateTimeFieldType[] {
          DateTimeFieldType.year(),
          DateTimeFieldType.weekOfWeekyear(),
          DateTimeFieldType.dayOfWeek(),
          DateTimeFieldType.hourOfDay(),
          DateTimeFieldType.minuteOfHour(),
          DateTimeFieldType.secondOfMinute(),
          DateTimeFieldType.millisOfSecond()
  };
  public static final DateTimeFieldType[] standardISODateFields = new DateTimeFieldType[] {
          DateTimeFieldType.year(),
          DateTimeFieldType.monthOfYear(),
          DateTimeFieldType.dayOfMonth(),
  };
  public static final DateTimeFieldType[] standardISOTimeFields = new DateTimeFieldType[] {
          DateTimeFieldType.hourOfDay(),
          DateTimeFieldType.minuteOfHour(),
          DateTimeFieldType.secondOfMinute(),
          DateTimeFieldType.millisOfSecond()
  };
  public static final Partial EMPTY_ISO_PARTIAL = new Partial(standardISOFields, new int[]{0,1,1,0,0,0,0});
  public static final Partial EMPTY_ISO_WEEK_PARTIAL = new Partial(standardISOWeekFields, new int[]{0,1,1,0,0,0,0});
  public static final Partial EMPTY_ISO_DATE_PARTIAL = new Partial(standardISODateFields, new int[]{0,1,1});
  public static final Partial EMPTY_ISO_TIME_PARTIAL = new Partial(standardISOTimeFields, new int[]{0,0,0,0});
  public static final Instant INSTANT_ZERO = new Instant(0);


  // Extensions to Joda time fields
  // Duration Fields
  public static final DurationFieldType Quarters = new DurationFieldType("quarters") {
    private static final long serialVersionUID = -8167713675442491871L;

    public DurationField getField(Chronology chronology) {
      return new ScaledDurationField(chronology.months(), Quarters, 3);
    }
  };

  public static final DurationFieldType Decades = new DurationFieldType("decades") {
    private static final long serialVersionUID = -4594189766036833410L;

    public DurationField getField(Chronology chronology) {
      return new ScaledDurationField(chronology.years(), Decades, 10);
    }
  };

  public static final DurationFieldType Centuries = new DurationFieldType("centuries") {
    private static final long serialVersionUID = -7268694266711862790L;

    public DurationField getField(Chronology chronology) {
      return new ScaledDurationField(chronology.years(), Centuries, 100);
    }
  };

  // DateTimeFields
  public static final DateTimeFieldType QuarterOfYear = new DateTimeFieldType("quarterOfYear") {
    private static final long serialVersionUID = -5677872459807379123L;

    public DurationFieldType getDurationType() {
      return Quarters;
    }

    public DurationFieldType getRangeDurationType() {
      return DurationFieldType.years();
    }

    public DateTimeField getField(Chronology chronology) {
      return new OffsetDateTimeField(new DividedDateTimeField(new OffsetDateTimeField(chronology.monthOfYear(), -1), QuarterOfYear, 3), 1);
    }
  };
  
  public static final DateTimeFieldType MonthOfQuarter = new DateTimeFieldType("monthOfQuarter") {
    private static final long serialVersionUID = -5677872459807379123L;

    public DurationFieldType getDurationType() {
      return DurationFieldType.months();
    }

    public DurationFieldType getRangeDurationType() {
      return Quarters;
    }

    public DateTimeField getField(Chronology chronology) {
      return new OffsetDateTimeField(new RemainderDateTimeField(new OffsetDateTimeField(chronology.monthOfYear(), -1), MonthOfQuarter, 3), 1);
    }
  };

  public static final DateTimeFieldType weekOfMonth = new DateTimeFieldType("weekOfMonth") {
    private static final long serialVersionUID = 8676056306203579438L;

    public DurationFieldType getDurationType() {
      return DurationFieldType.weeks();
    }

    public DurationFieldType getRangeDurationType() {
      return DurationFieldType.months();
    }

    public DateTimeField getField(Chronology chronology) {
      // TODO: specify
     return null;
    }
  };

  public static final DateTimeFieldType DecadeOfCentury = new DateTimeFieldType("decadeOfCentury") {
    private static final long serialVersionUID = 4301444712229535664L;

    public DurationFieldType getDurationType() {
      return Decades;
    }

    public DurationFieldType getRangeDurationType() {
      return DurationFieldType.centuries();
    }

    public DateTimeField getField(Chronology chronology) {
      return new DividedDateTimeField(chronology.yearOfCentury(), DecadeOfCentury, 10);
    }
  };

  // Helper functions for working with joda time type
  protected static boolean hasField(ReadablePartial base, DateTimeFieldType field)
  {
    if (base == null) {
      return false;
    } else {
      return base.isSupported(field);
    }
  }

  protected static boolean hasField(ReadablePeriod base, DurationFieldType field)
  {
    if (base == null) {
      return false;
    } else {
      return base.isSupported(field);
    }
  }

  protected static Partial setField(Partial base, DateTimeFieldType field, int value) {
    if (base == null) {
      return new Partial(field, value);
    } else {
      return base.with(field, value);
    }
  }

  public static Set<DurationFieldType> getSupportedDurationFields(Partial p)
  {
    Set<DurationFieldType> supportedDurations = new HashSet<DurationFieldType>();
    for (int i = 0; i < p.size(); i++) {
      supportedDurations.add(p.getFieldType(i).getDurationType());
    }
    return supportedDurations;
  }
  public static Period getUnsupportedDurationPeriod(Partial p, Period offset)
  {
    if (offset == null) { return null; }
    Set<DurationFieldType> supported = getSupportedDurationFields(p);
    Period res = null;
    for (int i = 0; i < offset.size(); i++) {
      if (!supported.contains(offset.getFieldType(i))) {
        if (offset.getValue(i) != 0) {
          if (res == null) { res = new Period(); }
          res = res.withField(offset.getFieldType(i), offset.getValue(i));
        }
      }
    }
    return res;
  }
  public static Partial combine(Partial p1, Partial p2) {
    if (p1 == null) return p2;
    if (p2 == null) return p1;
    Partial p = p1;
    for (int i = 0; i < p2.size(); i++) {
      DateTimeFieldType fieldType = p2.getFieldType(i);
      if (fieldType == DateTimeFieldType.year()) {
        if (p.isSupported(DateTimeFieldType.yearOfCentury())) {
          if (!p.isSupported(DateTimeFieldType.centuryOfEra())) {
            int yoc = p.get(DateTimeFieldType.yearOfCentury());
            int refYear = p2.getValue(i);
            int century = refYear / 100;
            int y2 = yoc + century*100;
            // TODO: Figure out which way to go
            if (refYear < y2) {
              y2 -= 100;
            }
            p = p.without(DateTimeFieldType.yearOfCentury());
            p = p.with(DateTimeFieldType.year(), y2);
          }
          continue;
        } else if (p.isSupported(DateTimeFieldType.centuryOfEra())) {
          continue;
        }
      } else if (fieldType == DateTimeFieldType.yearOfCentury()) {
        if (p.isSupported(DateTimeFieldType.year())) {
          continue;
        }
      } else if (fieldType == DateTimeFieldType.centuryOfEra()) {
        if (p.isSupported(DateTimeFieldType.year())) {
          continue;
        }
      }
      if (!p.isSupported(fieldType)) {
        p = p.with(fieldType, p2.getValue(i));
      }
    }
    if (!p.isSupported(DateTimeFieldType.year())) {
      if (p.isSupported(DateTimeFieldType.yearOfCentury()) && p.isSupported(DateTimeFieldType.centuryOfEra())) {
        int year = p.get(DateTimeFieldType.yearOfCentury()) + p.get(DateTimeFieldType.centuryOfEra())*100;
        p = p.with(DateTimeFieldType.year(), year);
        p = p.without(DateTimeFieldType.yearOfCentury());
        p = p.without(DateTimeFieldType.centuryOfEra());
      }
    }
    return p;
  }
  protected static DateTimeFieldType getMostGeneral(Partial p)
  {
    if (p.size() > 0) { return p.getFieldType(0); }
    return null;
  }
  protected static DateTimeFieldType getMostSpecific(Partial p)
  {
    if (p.size() > 0) { return p.getFieldType(p.size()-1); }
    return null;
  }
  protected static DurationFieldType getMostGeneral(Period p)
  {
    for (int i = 0; i < p.size(); i++) {
      if (p.getValue(i) > 0) {
        return p.getFieldType(i); 
      }
    }
    return null;
  }
  protected static DurationFieldType getMostSpecific(Period p)
  {
    for (int i = p.size()-1; i >= 0; i--) {
      if (p.getValue(i) > 0) {
        return p.getFieldType(i);
      }
    }
    return null;
  }
  protected static Period getJodaTimePeriod(Partial p)
  {
    if (p.size() > 0) {
      DateTimeFieldType dtType = p.getFieldType(p.size()-1);
      DurationFieldType dType = dtType.getDurationType();
      Period period = new Period();
      if (period.isSupported(dType)) {
       return period.withField(dType, 1);
      } else {
        DurationField df = dType.getField(p.getChronology());
        if (df instanceof ScaledDurationField) {
          ScaledDurationField sdf = (ScaledDurationField) df;
          return period.withField(sdf.getWrappedField().getType(), sdf.getScalar());
        }
       // PeriodType.forFields(new DurationFieldType[]{dType});
       // return new Period(df.getUnitMillis(), PeriodType.forFields(new DurationFieldType[]{dType}));

      }
    }
    return null;
  }
  public static Partial combineMoreGeneralFields(Partial p1, Partial p2) {
    return combineMoreGeneralFields(p1, p2, null);
  }

  // Combines more general fields from p2 to p1
  public static Partial combineMoreGeneralFields(Partial p1, Partial p2, DateTimeFieldType mgf) {
    Partial p = p1;
    Chronology c1 = p1.getChronology();
    Chronology c2 = p2.getChronology();
    if (!c1.equals(c2)) {
      throw new RuntimeException("Different chronology: c1=" + c1 + ", c2=" + c2);
    }
    DateTimeFieldType p1MostGeneralField = null;
    if (p1.size() > 0) {
      p1MostGeneralField = p1.getFieldType(0);    // Assume fields ordered from most general to least....
    }
    if (mgf == null || (p1MostGeneralField != null && isMoreGeneral(p1MostGeneralField, mgf, c1))) {
      mgf = p1MostGeneralField;
    }
    for (int i = 0; i < p2.size(); i++) {
      DateTimeFieldType fieldType = p2.getFieldType(i);
      if (fieldType == DateTimeFieldType.year()) {
        if (p.isSupported(DateTimeFieldType.yearOfCentury())) {
          if (!p.isSupported(DateTimeFieldType.centuryOfEra())) {
            int yoc = p.get(DateTimeFieldType.yearOfCentury());
            int refYear = p2.getValue(i);
            int century = refYear / 100;
            int y2 = yoc + century*100;
            // TODO: Figure out which way to go
            if (refYear < y2) {
              y2 -= 100;
            }
            p = p.without(DateTimeFieldType.yearOfCentury());
            p = p.with(DateTimeFieldType.year(), y2);
          }
          continue;
        } else if (p.isSupported(JodaTimeUtils.DecadeOfCentury)) {
          if (!p.isSupported(DateTimeFieldType.centuryOfEra())) {
            int decade = p.get(JodaTimeUtils.DecadeOfCentury);
            int refYear = p2.getValue(i);
            int century = refYear / 100;
            int y2 = decade*10 + century*100;
            // TODO: Figure out which way to go
            if (refYear < y2) {
              century--;
            }
            p = p.with(DateTimeFieldType.centuryOfEra(), century);
          }
          continue;
        }
      }
      if (mgf == null || isMoreGeneral(fieldType, mgf, c1)) {
        if (!p.isSupported(fieldType)) {
          p = p.with(fieldType, p2.getValue(i));
        }
      } else {
        break;
      }
    }
    if (!p.isSupported(DateTimeFieldType.year())) {
      if (p.isSupported(DateTimeFieldType.yearOfCentury()) && p.isSupported(DateTimeFieldType.centuryOfEra())) {
        int year = p.get(DateTimeFieldType.yearOfCentury()) + p.get(DateTimeFieldType.centuryOfEra())*100;
        p = p.with(DateTimeFieldType.year(), year);
        p = p.without(DateTimeFieldType.yearOfCentury());
        p = p.without(DateTimeFieldType.centuryOfEra());
      }
    }
    return p;
  }

  public static Partial discardMoreSpecificFields(Partial p, DateTimeFieldType d)
  {
    Partial res = new Partial();
    for (int i = 0; i < p.size(); i++) {
      DateTimeFieldType fieldType = p.getFieldType(i);
      if (fieldType.equals(d) || isMoreGeneral(fieldType, d, p.getChronology())) {
        res = res.with(fieldType, p.getValue(i));
      } 
    }
    if (res.isSupported(JodaTimeUtils.DecadeOfCentury) && !res.isSupported(DateTimeFieldType.centuryOfEra())) {
      if (p.isSupported(DateTimeFieldType.year())) {
        res = res.with(DateTimeFieldType.centuryOfEra(), p.get(DateTimeFieldType.year()) / 100);
      }
    }
    return res;
  }

  public static Partial discardMoreSpecificFields(Partial p, DurationFieldType dft)
  {
    DurationField df = dft.getField(p.getChronology());
    Partial res = new Partial();
    for (int i = 0; i < p.size(); i++) {
      DateTimeFieldType fieldType = p.getFieldType(i);
      DurationField f = fieldType.getDurationType().getField(p.getChronology());
      int cmp = df.compareTo(f);
      if (cmp <= 0) {
        res = res.with(fieldType, p.getValue(i));
      }
    }
    return res;
  }

  public static Period discardMoreSpecificFields(Period p, DurationFieldType dft, Chronology chronology)
  {
    DurationField df = dft.getField(chronology);
    Period res = new Period();
    for (int i = 0; i < p.size(); i++) {
      DurationFieldType fieldType = p.getFieldType(i);
      DurationField f = fieldType.getField(chronology);
      int cmp = df.compareTo(f);
      if (cmp <= 0) {
        res = res.withField(fieldType, p.getValue(i));
      }
    }
    return res;
  }

  public static Partial padMoreSpecificFields(Partial p, Period granularity)
  {
    DateTimeFieldType msf = getMostSpecific(p);
    if (isMoreGeneral(msf, DateTimeFieldType.year(), p.getChronology()) ||
            isMoreGeneral(msf, DateTimeFieldType.yearOfCentury(), p.getChronology())) {
      if (p.isSupported(DateTimeFieldType.yearOfCentury())) {
        // OKAY
      } else {
        if (p.isSupported(JodaTimeUtils.DecadeOfCentury)) {
          if (p.isSupported(DateTimeFieldType.centuryOfEra())) {
            int year = p.get(DateTimeFieldType.centuryOfEra()) * 100 + p.get(JodaTimeUtils.DecadeOfCentury)*10;
            p = p.without(JodaTimeUtils.DecadeOfCentury);
            p = p.without(DateTimeFieldType.centuryOfEra());
            p = p.with(DateTimeFieldType.year(), year);
          } else {
            int year = p.get(JodaTimeUtils.DecadeOfCentury)*10;
            p = p.without(JodaTimeUtils.DecadeOfCentury);
            p = p.with(DateTimeFieldType.yearOfCentury(), year);
          }
        } else {
          if (p.isSupported(DateTimeFieldType.centuryOfEra())) {
            int year = p.get(DateTimeFieldType.centuryOfEra()) * 100;
            p = p.without(DateTimeFieldType.centuryOfEra());
            p = p.with(DateTimeFieldType.year(), year);
          }
        }
      }
    }
    boolean useWeek = false;
    if (p.isSupported(DateTimeFieldType.weekOfWeekyear())) {
      if (!p.isSupported(DateTimeFieldType.dayOfMonth()) && !p.isSupported(DateTimeFieldType.dayOfWeek())) {
        p = p.with(DateTimeFieldType.dayOfWeek(), 1);
        if (p.isSupported(DateTimeFieldType.monthOfYear())) {
          p = p.without(DateTimeFieldType.monthOfYear());
        }
      }
      useWeek = true;
    }
    Partial p2 = useWeek? EMPTY_ISO_WEEK_PARTIAL:EMPTY_ISO_PARTIAL;
    for (int i = 0; i < p2.size(); i++) {
      DateTimeFieldType fieldType = p2.getFieldType(i);
      if (msf == null || isMoreSpecific(fieldType, msf, p.getChronology())) {
        if (!p.isSupported(fieldType)) {
          p = p.with(fieldType, p2.getValue(i));
        }
      }
    }
    if (granularity != null) {
      DurationFieldType mostSpecific = getMostSpecific(granularity);
      p = discardMoreSpecificFields(p, mostSpecific);
    }
    return p;
  }

  public static boolean isCompatible(Partial p1, Partial p2) {
    if (p1 == null) return true;
    if (p2 == null) return true;
    for (int i = 0; i < p1.size(); i++) {
      DateTimeFieldType type = p1.getFieldType(i);
      int v = p1.getValue(i);
      if (JodaTimeUtils.hasField(p2,type)) {
        if (v != p2.get(type)) {
          return false;
        }
      }
    }
    return true;
  }
  // Uses p2 to resolve dow for p1
  public static Partial resolveDowToDay(Partial p1, Partial p2)
  {
    if (isCompatible(p1,p2)) {
      if (p1.isSupported(DateTimeFieldType.dayOfWeek())) {
        if (!p1.isSupported(DateTimeFieldType.dayOfMonth())) {
          if (p2.isSupported(DateTimeFieldType.dayOfMonth()) && p2.isSupported(DateTimeFieldType.monthOfYear()) && p2.isSupported(DateTimeFieldType.year())) {
            Instant t2 = getInstant(p2);
            DateTime t1 = p1.toDateTime(t2);
            return getPartial(t1.toInstant(), p1.with(DateTimeFieldType.dayOfMonth(), 1)/*.with(DateTimeFieldType.weekOfWeekyear(), 1) */);
          }
        }
      }
    }
    return p1;
  }
  // Resolve dow for p1
  public static Partial resolveDowToDay(Partial p)
  {
    if (p.isSupported(DateTimeFieldType.dayOfWeek())) {
      if (!p.isSupported(DateTimeFieldType.dayOfMonth())) {
        if (p.isSupported(DateTimeFieldType.weekOfWeekyear()) && p.isSupported(DateTimeFieldType.year())) {
          Instant t2 = getInstant(p);
          DateTime t1 = p.toDateTime(t2);
          Partial res = getPartial(t1.toInstant(), EMPTY_ISO_PARTIAL);
          DateTimeFieldType mostSpecific = getMostSpecific(p);
          res = discardMoreSpecificFields(res, mostSpecific.getDurationType());
          return res;
        }
      }
    }
    return p;
  }
  // Uses p2 to resolve week for p1
  public static Partial resolveWeek(Partial p1, Partial p2)
  {
    if (isCompatible(p1,p2)) {
        if (!p1.isSupported(DateTimeFieldType.dayOfMonth())) {
          if (p2.isSupported(DateTimeFieldType.dayOfMonth()) && p2.isSupported(DateTimeFieldType.monthOfYear()) && p2.isSupported(DateTimeFieldType.year())) {
            Instant t2 = getInstant(p2);
            DateTime t1 = p1.toDateTime(t2);
            return getPartial(t1.toInstant(), p1.without(DateTimeFieldType.dayOfMonth()).without(DateTimeFieldType.monthOfYear()).with(DateTimeFieldType.weekOfWeekyear(), 1));
          }
      }
    }
    return p1;
  }
  public static Instant getInstant(Partial p)
  {
    if (p == null) return null;
    int year = p.isSupported(DateTimeFieldType.year())? p.get(DateTimeFieldType.year()):0;
    if (!p.isSupported(DateTimeFieldType.year())) {
      if (p.isSupported(DateTimeFieldType.centuryOfEra())) {
        year += 100*p.get(DateTimeFieldType.centuryOfEra());
      }
      if (p.isSupported(DateTimeFieldType.yearOfCentury())) {
        year += p.get(DateTimeFieldType.yearOfCentury());
      } else if (p.isSupported(DecadeOfCentury)) {
        year += 10*p.get(DecadeOfCentury);
      }
    }
    int moy = p.isSupported(DateTimeFieldType.monthOfYear())? p.get(DateTimeFieldType.monthOfYear()):1;
    if (!p.isSupported(DateTimeFieldType.monthOfYear())) {
      if (p.isSupported(QuarterOfYear)) {
        moy += 3*(p.get(QuarterOfYear)-1);
      }
    }
    int dom = p.isSupported(DateTimeFieldType.dayOfMonth())? p.get(DateTimeFieldType.dayOfMonth()):1;
    int hod = p.isSupported(DateTimeFieldType.hourOfDay())? p.get(DateTimeFieldType.hourOfDay()):0;
    int moh = p.isSupported(DateTimeFieldType.minuteOfHour())? p.get(DateTimeFieldType.minuteOfHour()):0;
    int som = p.isSupported(DateTimeFieldType.secondOfMinute())? p.get(DateTimeFieldType.secondOfMinute()):0;
    int msos = p.isSupported(DateTimeFieldType.millisOfSecond())? p.get(DateTimeFieldType.millisOfSecond()):0;
    return new DateTime(year, moy, dom, hod, moh, som, msos, isoUTCChronology).toInstant();
  }

  public static Partial getPartial(Instant t, Partial p)
  {
    Partial res = new Partial(p);
    for (int i = 0; i < p.size(); i++) {
      res = res.withField(p.getFieldType(i), t.get(p.getFieldType(i)));
    }
    return res;
  }

  // Add duration to partial
  public static Partial addForce(Partial p, Period d, int scalar)
  {
    Instant t = getInstant(p);
    t = t.withDurationAdded(d.toDurationFrom(INSTANT_ZERO), scalar);
    return getPartial(t, p);
  }
  
  // Returns if df1 is more general than df2
  public static boolean isMoreGeneral(DateTimeFieldType df1, DateTimeFieldType df2, Chronology chronology)
  {
    DurationFieldType df1DurationFieldType = df1.getDurationType();
    DurationFieldType df2DurationFieldType = df2.getDurationType();
    if (!df2DurationFieldType.equals(df1DurationFieldType)) {
      DurationField df1Unit = df1DurationFieldType.getField(chronology);
      DurationFieldType p = df2.getRangeDurationType();
      if (p != null) {
        DurationField df2Unit = df2DurationFieldType.getField(chronology);
        int cmp = df1Unit.compareTo(df2Unit);
        if (cmp > 0) {
          return true;
        }
      }
    }
    return false;
  }

  // Returns if df1 is more specific than df2
  public static boolean isMoreSpecific(DateTimeFieldType df1, DateTimeFieldType df2, Chronology chronology)
  {
    DurationFieldType df1DurationFieldType = df1.getDurationType();
    DurationFieldType df2DurationFieldType = df2.getDurationType();
    if (!df2DurationFieldType.equals(df1DurationFieldType)) {
      DurationField df2Unit = df2DurationFieldType.getField(chronology);
      DurationFieldType p = df1.getRangeDurationType();
      if (p != null) {
        DurationField df1Unit = df1DurationFieldType.getField(chronology);
        int cmp = df1Unit.compareTo(df2Unit);
        if (cmp < 0) {
          return true;
        }
      }
    }
    return false;
  }


}
