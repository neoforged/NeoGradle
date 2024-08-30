package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.util.AdaptingZipBuildingFileTreeVisitor;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class DistOnlyInjector extends DefaultRuntime {

    public DistOnlyInjector() {
        getDistEnumClass().convention("net.neoforged.api.distmarker.Dist");
        getDistMarkerClass().convention("net.neoforged.api.distmarker.OnlyIn");
        getDistEnumValue().convention("CLIENT");
        getDummy().convention("something8");
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    protected void run() throws Throwable {
        getCacheService().get().cached(
                this,
                ICacheableJob.Default.file(getOutput(), this::doRun)
        ).execute();
    }

    private void doRun() throws Exception {
        final File outputFile = ensureFileWorkspaceReady(getOutput());

        final FileTree inputFiles = getArchiveOperations().zipTree(getInputFile());
        try (final FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
             final ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            final AdaptingZipBuildingFileTreeVisitor visitor = new AdaptingZipBuildingFileTreeVisitor(
                    zipOutputStream,
                    new InjectingFileAdapter(getDistEnumClass().get(), getDistMarkerClass().get(), getDistEnumValue().get())
            );
            inputFiles.visit(visitor);
        }
    }

        @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputFile();

    @Input
    public abstract Property<String> getDistEnumClass();

    @Input
    public abstract Property<String> getDistEnumValue();

    @Input
    public abstract Property<String> getDistMarkerClass();

    @Input
    public abstract Property<String> getDummy();

    private static final class InjectingFileAdapter implements BiConsumer<FileVisitDetails, OutputStream> {

        private final String distEnumClass;
        private final String distMarkerClass;

        private final String distEnumClassName;
        private final String distMarkerClassName;

        private final String distEnumValue;

        private InjectingFileAdapter(String distEnumClass, String distMarkerClass, String distEnumValue) {
            this.distEnumClass = distEnumClass;
            this.distMarkerClass = distMarkerClass;
            this.distEnumClassName = distEnumClass.substring(distEnumClass.lastIndexOf('.') + 1);
            this.distMarkerClassName = distMarkerClass.substring(distMarkerClass.lastIndexOf('.') + 1);
            this.distEnumValue = distEnumValue;
        }

        @Override
        public void accept(FileVisitDetails fileVisitDetails, OutputStream outputStream) {
            try {
                inject(fileVisitDetails.open(), outputStream);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to inject into file: " + fileVisitDetails.getName(), e);
            }
        }

        private void inject(final InputStream inputStream, final OutputStream outputStream) throws IOException {
            final InputStreamReader reader = new InputStreamReader(inputStream);
            final BufferedReader bufferedReader = new BufferedReader(reader);
            final List<String> lines = bufferedReader.lines().toList();
            final List<String> injectedLines = inject(lines);

            final OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            final BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for (String line : injectedLines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
            //Close is not needed due to how zip processing works.
        }

        private List<String> inject(final List<String> lines) {
            final List<String> result = new ArrayList<>();

            boolean firstDeclarationFound = false;
            for (final String line : lines) {
                final boolean isDeclarationLine = isDeclarationLine(line);
                if (!firstDeclarationFound) {
                    if (isDeclarationLine) {
                        firstDeclarationFound = true;
                        injectBeforeLastMatching(result, "import " + distEnumClass + ";", InjectingFileAdapter::isInjectionPoint);
                        injectBeforeLastMatching(result, "import " + distMarkerClass + ";", InjectingFileAdapter::isInjectionPoint);
                    }
                }

                if (isDeclarationLine) {
                    result.add("%s@%s(%s.%s)".formatted(
                            getIndent(line),
                            distMarkerClassName,
                            distEnumClassName,
                            distEnumValue
                    ));
                }

                result.add(line);
            }

            return result;
        }

        private static String getIndent(String line) {
            final StringBuilder indent = new StringBuilder();
            for (char c : line.toCharArray()) {
                if (Character.isWhitespace(c)) {
                    indent.append(c);
                } else {
                    break;
                }
            }

            return indent.toString();
        }

        private static void injectBeforeLastMatching(final List<String> lines, final String lineToInject, Predicate<String> matcher) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                final String line = lines.get(i);
                if (matcher.test(line)) {
                    lines.add(i + 1, lineToInject);
                    return;
                }
            }

            lines.add(lineToInject);
        }

        private static boolean isInjectionPoint(final String line) {
            return !line.isBlank() && !line.trim().startsWith("@") && !line.trim().startsWith("import org");
        }

        private static boolean isDeclarationLine(final String line) {
            return line.contains(" class ") || line.contains(" interface ") || line.contains(" enum ") || line.contains(" record ") ||
                    line.startsWith("class ") || line.startsWith("interface ") || line.startsWith("enum ") || line.startsWith("record ");
        }
    }
}
