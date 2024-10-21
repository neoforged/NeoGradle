package net.neoforged.gradle.common.extensions.problems;

import net.neoforged.gradle.common.util.NeoGradleUtils;
import org.gradle.api.logging.Logger;

public final class NeoGradleProblemSpec {
    private String category;
    private String id;
    private String contextualLabel;
    private String solution;
    private String details;
    private String section;

    public NeoGradleProblemSpec id(String category, String id) {
        this.category = category;
        this.id = id;
        return this;
    }

    public NeoGradleProblemSpec contextualLabel(String contextualLabel) {
        this.contextualLabel = contextualLabel;
        return this;
    }

    public NeoGradleProblemSpec solution(String solution) {
        this.solution = solution;
        return this;
    }

    public NeoGradleProblemSpec details(String details) {
        this.details = details;
        return this;
    }

    public NeoGradleProblemSpec section(String section) {
        this.section = section;
        return this;
    }

    String category() {
        return category;
    }

    String id() {
        return id;
    }

    String contextualLabel() {
        return contextualLabel;
    }

    String solution() {
        return solution;
    }

    String details() {
        return details;
    }

    String getSection() {
        return section;
    }

    String documentedAt() {
        return readMeUrl(section);
    }

    void log(Logger logger) {
        logger.warn("-------------------------------------");
        logger.warn("NeoGradle detected a problem with your project: %s".formatted(contextualLabel));
        logger.warn("Details: %s".formatted(details));
        logger.warn("Potential Solution: %s".formatted(solution));
        logger.warn("More information: %s".formatted(documentedAt()));
        logger.warn("-------------------------------------");
    }

    private static String readMeUrl(String section) {
        final String neogradleVersion = NeoGradleUtils.getNeogradleVersion();
        final String branchMajorVersion = neogradleVersion.split("\\.")[0];
        final String branchMinorVersion = neogradleVersion.split("\\.")[1];
        final String branch = "NG_%s.%s".formatted(branchMajorVersion, branchMinorVersion);

        return "https://github.com/neoforged/NeoGradle/blob/%s/README.md#%s".formatted(branch, section);
    }
}
