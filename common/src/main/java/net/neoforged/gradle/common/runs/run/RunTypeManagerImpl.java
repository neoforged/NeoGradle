package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.common.util.DelegatingDomainObjectContainer;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunTypeManagerImpl extends DelegatingDomainObjectContainer<RunType> implements RunTypeManager {

    private final List<Parser> parsers = new ArrayList<>();

    @Inject
    public RunTypeManagerImpl(Project project) {
        super(project.getObjects().domainObjectContainer(RunType.class));
    }

    @Override
    public Collection<RunType> parse(File file) {
        if (!file.exists())
            return Collections.emptyList();

        return parsers.stream()
                .flatMap(parser -> {
                    try {
                        return parser.parse(file).stream();
                    } catch (Exception exception) {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet());
    }

    @Override
    public void registerParser(Parser parser) {
        parsers.add(parser);
    }
}
