package net.neoforged.gradle.common.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public final class BundledServerUtils {

    private BundledServerUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: BundledServerUtils. This is a utility class");
    }

    public static boolean isBundledServer(final File serverJar) {
        try(final ZipFile file = new ZipFile(serverJar)) {
            return file.getEntry("META-INF/classpath-joined") != null &&
                    file.getEntry("META-INF/libraries.list") != null &&
                    file.getEntry("META-INF/versions.list") != null &&
                    file.getEntry("META-INF/main-class") != null &&
                    file.getEntry("versions.json") != null;
        } catch (IOException e) {
            return false;
        }
    }

    public static List<String> getBundledDependencies(final File serverJar) {
        try(final ZipFile file = new ZipFile(serverJar)) {
            final InputStream inputStream = file.getInputStream(file.getEntry("META-INF/libraries.list"));
            final List<String> dependencies = IOUtils.readLines(inputStream, Charset.defaultCharset());
            inputStream.close();

            return dependencies.stream()
                    .map(l-> l.split("\\s+"))
                    .filter(l-> l.length >= 2)
                    .map(l-> l[1])
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read libraries.list from server jar", e);
        }
    }

    public static String getBundledMainClass(final File serverJar) {
        try(final ZipFile file = new ZipFile(serverJar)) {
            final InputStream inputStream = file.getInputStream(file.getEntry("META-INF/main-class"));
            final String mainClass = IOUtils.readLines(inputStream, Charset.defaultCharset()).get(0);
            inputStream.close();

            return mainClass;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read main-class from server jar", e);
        }
    }

    public static String getBundledVersion(final File serverJar) {
        try(final ZipFile file = new ZipFile(serverJar)) {
            final InputStream inputStream = file.getInputStream(file.getEntry("META-INF/versions.list"));
            final List<String> dependencies = IOUtils.readLines(inputStream, Charset.defaultCharset());
            inputStream.close();

            return dependencies.stream()
                    .map(l-> l.split("\\s+"))
                    .filter(l-> l.length >= 2)
                    .map(l-> l[1])
                    .findFirst().orElseThrow(() -> new RuntimeException("Failed to find version in versions.list"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read libraries.list from server jar", e);
        }
    }

    public static void extractBundledVersion(final File serverJar, final File outputFile) {
        try(final ZipFile file = new ZipFile(serverJar);
            final FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            final String versionName = getBundledVersion(serverJar);
            final String jarPath = "META-INF/versions/" + versionName + "/" + versionName + ".jar";
            final InputStream jarStream = file.getInputStream(file.getEntry(jarPath));
            IOUtils.copy(jarStream, fileOutputStream);
            jarStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read libraries.list from server jar", e);
        }
    }
}
