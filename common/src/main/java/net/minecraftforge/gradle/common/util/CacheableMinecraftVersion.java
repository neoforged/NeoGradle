package net.minecraftforge.gradle.common.util;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Locale;

/**
 * A reimplementation (basically a carbon copy) of {@link net.minecraftforge.srgutils.MinecraftVersion} that is Gradle compatible.
 * We should consider making that class extendable in the future!
 */
public class CacheableMinecraftVersion implements Comparable<CacheableMinecraftVersion>, Serializable {

    public static CacheableMinecraftVersion NEGATIVE = from("-1");

    public static CacheableMinecraftVersion from(String version) {
        try {
            return CacheableMinecraftVersion.get(version);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) { //This is dirty, TODO, make less exceptioney
            throw new IllegalArgumentException("Unknown version: " + version, e);
        }
    }

    private static String fromSnapshot(int year, int week) {
        int value = (year * 100) + week;
        if (value >= 1147 && value <= 1201) return "1.1";
        if (value >= 1203 && value <= 1208) return "1.2";
        if (value >= 1215 && value <= 1230) return "1.3";
        if (value >= 1232 && value <= 1242) return "1.4";
        if (value >= 1249 && value <= 1250) return "1.4.6";
        if (value >= 1301 && value <= 1310) return "1.5";
        if (value >= 1311 && value <= 1312) return "1.5.1";
        if (value >= 1316 && value <= 1326) return "1.6";
        if (value >= 1336 && value <= 1343) return "1.7";
        if (value >= 1347 && value <= 1349) return "1.7.4";
        if (value >= 1402 && value <= 1434) return "1.8";
        if (value >= 1531 && value <= 1607) return "1.9";
        if (value >= 1614 && value <= 1615) return "1.9.3";
        if (value >= 1620 && value <= 1621) return "1.10";
        if (value >= 1632 && value <= 1644) return "1.11";
        if (value == 1650) return "1.11.1";
        if (value >= 1706 && value <= 1718) return "1.12";
        if (value == 1731) return "1.12.1";
        if (value >= 1743 && value <= 1822) return "1.13";
        if (value >= 1830 && value <= 1833) return "1.13.1";
        if (value >= 1843 && value <= 1914) return "1.14";
        if (value >= 1934 && value <= 1946) return "1.15";
        if (value >= 2006 && value <= 2022) return "1.16";
        if (value >= 2027 && value <= 2030) return "1.16.2";
        if (value >= 2045 && value <= 2120) return "1.17";
        if (value >= 2137 && value <= 2144) return "1.18";
        if (value >= 2203 && value <= 2207) return "1.18.2";
        if (value >= 2211 && value <= 9999) return "1.19";
        throw new IllegalArgumentException("Invalid snapshot date: " + value);
    }

    private static int[] splitDots(String version) {
        String[] pts = version.split("\\.");
        int[] values = new int[pts.length];
        for (int x = 0; x < pts.length; x++)
            values[x] = Integer.parseInt(pts[x]);
        return values;
    }

    private final Type type;
    private final String full;
    private final int[] nearest;
    private final int week;
    private final int year;
    private final int pre;
    private final String revision;

    private CacheableMinecraftVersion(Type type, String full, int week, int year, int pre, @Nullable String revision, int[] nearest) {
        this.type = type;
        this.full = full;
        this.nearest = nearest;
        this.week = week;
        this.year = year;
        this.pre = pre;
        this.revision = revision;
    }

    @Input
    public Type getType() {
        return type;
    }

    @Input
    public String getFull() {
        return full;
    }

    @Input
    public int[] getNearest() {
        return nearest;
    }

    @Input
    public int getWeek() {
        return week;
    }

    @Input
    public int getYear() {
        return year;
    }

    @Input
    public int getPre() {
        return pre;
    }

    @Input
    @Optional
    @Nullable
    public String getRevision() {
        return revision;
    }

    private static CacheableMinecraftVersion get(String version) {
        String lower = version.toLowerCase(Locale.ENGLISH);
        char first = version.charAt(0);
        String preA = Character.toString((char)('a' - 1));

        if ("15w14a".equals(lower))                    // 2015 April Fools
            return new CacheableMinecraftVersion(Type.SNAPSHOT, version, 14, 15, 0, "a", splitDots("1.10"));
        else if ("1.rv-pre1".equals(lower))            // 2016 April Fools
            return new CacheableMinecraftVersion(Type.SNAPSHOT, version, 14, 16, 0, preA, splitDots("1.9.3"));
        else if ("3d shareware v1.34".equals(lower))   // 2019 April Fools
            return new CacheableMinecraftVersion(Type.SNAPSHOT, version, 14, 19, 0, preA, splitDots("1.14"));
        else if ("20w14infinite".equals(lower))        // 2020 April Fools
            return new CacheableMinecraftVersion(Type.SNAPSHOT, version, 14, 20, 0, preA, splitDots("1.16"));
        else if ("22w13oneblockatatime".equals(lower)) // 2022 April Fools
            return new CacheableMinecraftVersion(Type.SNAPSHOT, version, 13, 22, 0, "b", splitDots("1.19"));
        else if ("inf-20100618".equals(lower))
            return new CacheableMinecraftVersion(Type.ALPHA, version, 25, 10, 0, "a", splitDots("1.0.4"));
        else if ("c0.0.13a_03".equals(lower))          // Rather than screw with the logic of the alpha/beta parser, special case this weird one
            return new CacheableMinecraftVersion(Type.ALPHA, version, -1, -1, 0, preA, splitDots("0.0.13"));
        else if (lower.startsWith("rd-")) {
            String rev;
            switch (lower) {
                case "rd-132211":
                    rev = "a";
                    break;
                case "rd-132328":
                    rev = "b";
                    break;
                case "rd-20090515":
                    rev = "c";
                    break;
                case "rd-160052":
                    rev = "d";
                    break;
                case "rd-161348":
                    rev = "e";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown 'rd' version: " + version);
            }

            return new CacheableMinecraftVersion(Type.ALPHA, version, 20, 9, 0, rev, splitDots("0.0.1"));
        } else if (first == 'a' || first == 'b' || first == 'c') {
            String clean = version.substring(1).replace('_', '.');
            String nearest = clean;
            String rev = null;
            char end = clean.charAt(clean.length() - 1);
            if (end < '0' || end > '9') {
                rev = Character.toString(end);
                nearest = clean.substring(0, clean.length() - 1);
            }

            return new CacheableMinecraftVersion(first == 'b' ? Type.BETA : Type.ALPHA, version, -1, -1, 0, rev, splitDots(nearest));
        } else if (version.length() == 6 && version.charAt(2) == 'w') {
            int year = Integer.parseInt(version.substring(0, 2));
            int week = Integer.parseInt(version.substring(3, 5));

            return new CacheableMinecraftVersion(Type.SNAPSHOT, version, week, year, 0, version.substring(5), splitDots(fromSnapshot(year, week)));
        } else {
            int pre = 0;
            String nearest = version;
            if (version.contains("-pre")) {
                String[] pts = version.split("-pre");
                pre = Integer.parseInt(pts[1]);
                nearest = pts[0];
            } else if (version.contains("_Pre-Release_")) {
                String[] pts = version.split("_Pre-Release_");
                pre = Integer.parseInt(pts[1]);
                nearest = pts[0];
            } else if (version.contains(" Pre-Release ")) {
                String[] pts = version.split(" Pre-Release ");
                pre = Integer.parseInt(pts[1]);
                nearest = pts[0];
            } else if (version.contains("-rc")) {
                String[] pts = version.split("-rc");
                pre = -1 * Integer.parseInt(pts[1]);
                nearest = pts[0];
            }
            return new CacheableMinecraftVersion(Type.RELEASE, version, -1, -1, pre, null, splitDots(nearest));
        }
    }

    @Override
    public String toString() {
        return this.full;
    }

    @Override
    public int hashCode() {
        return this.full.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CacheableMinecraftVersion))
            return false;
        return this.full.equals(((CacheableMinecraftVersion)o).full);
    }

    @Override
    public int compareTo(@Nullable CacheableMinecraftVersion o) {
        if (o == null)
            return 1;

        if (this.type != o.type) {
            int ret = 0;
            switch (this.type) {
                case ALPHA: return -1;
                case BETA:  return o.type == Type.ALPHA ? 1 : -1;
                case SNAPSHOT:
                    ret = compareFull(o);
                    return ret == 0 ? -1 : ret;
                case RELEASE:
                    switch (o.type) {
                        case ALPHA:
                        case BETA: return 1;
                        case SNAPSHOT:
                            ret = compareFull(o);
                            return ret == 0 ? 1 : ret;
                    }
            }
        }

        switch (this.type) {
            case ALPHA:
            case BETA:
                int ret = compareFull(o);
                if (ret != 0)                                    return ret;
                if (this.revision == null && o.revision == null) return  0;
                if (this.revision == null) return  1;
                if (o.revision == null) return -1;
                return this.revision.compareTo(o.revision);

            case SNAPSHOT:
                if (this.year != o.year) return this.year - o.year;
                if (this.week != o.week) return this.week - o.week;
                if (this.revision == null && o.revision == null) return  0;
                if (this.revision == null) return  1;
                if (o.revision == null) return -1;
                return this.revision.compareTo(o.revision);

            case RELEASE:
                return compareFull(o);

            default:
                throw new IllegalArgumentException("Invalid type: " + this.type);
        }
    }

    private int compareFull(CacheableMinecraftVersion o) {
        for (int x = 0; x < this.nearest.length; x++) {
            if (x >= o.nearest.length) return 1;
            if (this.nearest[x] != o.nearest[x])
                return this.nearest[x] - o.nearest[x];
        }
        if (this.nearest.length < o.nearest.length)
            return -1;
        if (this.type == Type.RELEASE && o.type != Type.RELEASE) return 1;
        if (this.type != Type.RELEASE && o.type == Type.RELEASE) return -1;
        //Release candidates have negative numbers to make them sort differently then pre releases.
        if (this.pre == 0 && o.pre != 0) return 1;
        if (this.pre != 0 && o.pre == 0) return -1;
        if (this.pre < 0 && o.pre > 0) return 1;
        if (this.pre > 0 && o.pre < 0) return -1;
        return this.pre > 0 ? this.pre - o.pre :
                -this.pre - -o.pre;
    }

    private static enum Type {
        RELEASE,
        SNAPSHOT,
        BETA,
        ALPHA
    }
}
