package net.minecraftforge.gradle.util;

import net.minecraftforge.srgutils.IMappingFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.minecraftforge.gradle.util.IMappingFileUtils.Element.CLASS;
import static net.minecraftforge.gradle.util.IMappingFileUtils.Element.FIELD;
import static net.minecraftforge.gradle.util.IMappingFileUtils.Element.METHOD;
import static net.minecraftforge.gradle.util.IMappingFileUtils.Element.PARAMETER;


public final class IMappingFileUtils {

    private IMappingFileUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: IMappingFileUtils. This is a utility class");
    }

    public static IMappingFile load(final File file) {
        try {
            return IMappingFile.load(file);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to load mappings file: %s", file.getAbsolutePath()), e);
        }
    }

    enum Element{ PACKAGE, CLASS, FIELD, METHOD, PARAMETER }
    public static List<String> writeMappingFile(final IMappingFile file, final IMappingFile.Format format, final boolean reversed) {
        List<String> lines = new ArrayList<>();
        Comparator<IMappingFile.INode> sort = reversed ? Comparator.comparing(IMappingFile.INode::getMapped) : Comparator.comparing(IMappingFile.INode::getOriginal);

        file.getPackages().stream().sorted(sort).forEachOrdered(pkg -> {
            lines.add(pkg.write(format, reversed));
            writeMeta(format, lines, Element.PACKAGE, pkg.getMetadata());
        });
        file.getClasses().stream().sorted(sort).forEachOrdered(cls -> {
            lines.add(cls.write(format, reversed));
            writeMeta(format, lines, CLASS, cls.getMetadata());

            cls.getFields().stream().sorted(sort).forEachOrdered(fld -> {
                lines.add(fld.write(format, reversed));
                writeMeta(format, lines, FIELD, fld.getMetadata());
            });

            cls.getMethods().stream().sorted(sort).forEachOrdered(mtd -> {
                lines.add(mtd.write(format, reversed));
                writeMeta(format, lines, METHOD, mtd.getMetadata());

                mtd.getParameters().stream().sorted((a,b) -> a.getIndex() - b.getIndex()).forEachOrdered(par -> {
                    lines.add(par.write(format, reversed));
                    writeMeta(format, lines, PARAMETER, par.getMetadata());
                });
            });
        });

        lines.removeIf(Objects::isNull);

        if (!format.isOrdered()) {
            Comparator<String> linesort = (format == IMappingFile.Format.SRG || format == IMappingFile.Format.XSRG) ? IMappingFileUtils::compareLines : String::compareTo;
            lines.sort(linesort);
        }

        if (format == IMappingFile.Format.TINY1) {
            lines.add(0, "v1\tleft\tright");
        } else if (format == IMappingFile.Format.TINY) {
            lines.add(0, "tiny\t2\t0\tleft\tright");
        } else if (format == IMappingFile.Format.TSRG2) {
            lines.add(0, "tsrg2 left right");
        }

        return lines;
    }

    private static void writeMeta(IMappingFile.Format format, List<String> lines, Element element, Map<String, String> meta) {
        int indent = 0;
        switch (element) {
            case PACKAGE:
            case CLASS:
                indent = 1;
                break;
            case FIELD:
            case METHOD:
                indent = 2;
                break;
            case PARAMETER:
                indent = 3;
                break;
        }

        switch (format) {
            case CSRG:
            case PG:
            case SRG:
            case TINY1:
            case TSRG:
            case XSRG:
                break;
            case TINY:
                String comment = meta.get("comment");
                if (comment != null) {
                    char[] prefix = new char[indent];
                    Arrays.fill(prefix, '\t');
                    lines.add(new String(prefix) + "c\t" + escapeTinyString(comment));
                }
                break;
            case TSRG2:
                if (meta.containsKey("is_static")) {
                    char[] prefix = new char[indent];
                    Arrays.fill(prefix, '\t');
                    lines.add(new String(prefix) + "static");
                }
                break;
        }
    }

    private static String escapeTinyString(String value) {
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\0", "\\0");
    }

    private static final List<String> ORDER = Arrays.asList("PK:", "CL:", "FD:", "MD:");

    private static int compareLines(String o1, String o2) {
        String[] pt1 = o1.split(" ");
        String[] pt2 = o2.split(" ");
        if (!pt1[0].equals(pt2[0]))
            return ORDER.indexOf(pt1[0]) - ORDER.lastIndexOf(pt2[0]);

        if ("PK:".equals(pt1[0]))
            return o1.compareTo(o2);
        if ("CL:".equals(pt1[0]))
            return compareClasses(pt1[1], pt2[1]);
        if ("FD:".equals(pt1[0]) || "MD:".equals(pt1[0]))
        {
            String[][] y = {
                    {pt1[1].substring(0, pt1[1].lastIndexOf('/')), pt1[1].substring(pt1[1].lastIndexOf('/') + 1)},
                    {pt2[1].substring(0, pt2[1].lastIndexOf('/')), pt2[1].substring(pt2[1].lastIndexOf('/') + 1)}
            };
            int ret = compareClasses(y[0][0], y[1][0]);
            if (ret != 0)
                return ret;
            return y[0][1].compareTo(y[1][1]);
        }
        return o1.compareTo(o2);
    }

    private static int compareClasses(String cls1, String cls2) {
        if (cls1.indexOf('/') > 0 && cls2.indexOf('/') > 0)
            return cls1.compareTo(cls2);
        String[][] t = { cls1.split("\\$"), cls2.split("\\$") };
        int max = Math.min(t[0].length, t[1].length);
        for (int i = 0; i < max; i++)
        {
            if (!t[0][i].equals(t[1][i]))
            {
                if (t[0][i].length() != t[1][i].length())
                    return t[0][i].length() - t[1][i].length();
                return t[0][i].compareTo(t[1][i]);
            }
        }
        return Integer.compare(t[0].length, t[1].length);
    }
}
