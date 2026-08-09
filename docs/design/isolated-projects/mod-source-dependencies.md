# Mod Source Dependencies Design for Isolated Projects Support

## Problem Statement

Current implementation allows users to specify mod sources from other projects using direct source set references:

```groovy
runs {
    server {
        modSource project(':api').sourceSets.main
    }
}
```

This violates Gradle's isolated projects constraints (Gradle 9.7.0+), which prohibit accessing extensions on other projects during configuration time. The error manifests as:

> Project ':mod' cannot access 'sourceSets' extension on another project ':api'

## Current Mechanism Analysis

### How modSource Works Today

When a user specifies `modSource project(':api').sourceSets.main`:

1. **Direct cross-project access**: The build script evaluates `project(':api')` then accesses `.sourceSets.main`, which is an extension on another project
2. **Stored as SourceSet object**: RunSourceSetsImpl stores the actual SourceSet reference in a Multimap<String, SourceSet>
3. **Used for multiple purposes**:
   - Build dependency wiring: `RunsUtil.addRunSourcesDependenciesToTask()` uses `SourceSetUtils.getProject(sourceSet).getTasks().named(...)` to wire compileJava/processResources dependencies
   - IDE path generation: `RunsUtil.buildGradleModClasses()` uses `sourceSet.getOutput().getResourcesDir()` and `getClassesDirs()` for Gradle run config classpath entries
   - IntelliJ module naming: `RunsUtil.getIntellijModuleName(sourceSet)` generates module names like `api.main` from project name + source set name
   - Environment variable generation: Source set names are used in FML mod list paths and other runtime properties

### Why Direct Access Fails Under Isolated Projects

Gradle's isolated projects mode enforces strict boundaries between projects during configuration time. The rule is simple: **a project cannot access extensions, tasks, or configurations of another project directly**. Dependencies must be expressed through Gradle's variant-aware dependency mechanism instead.

The current approach fails because `project(':api').sourceSets.main` accesses the SourceSetContainer extension on `:api`, which is forbidden even though it happens in a build script (not plugin code).

## Proposed Solution: Variant-Based Mod Source Dependencies

### Core Idea

Use Gradle's variant-aware dependency system as the mechanism for declaring mod sources from other projects. Each source set publishes a consumable "runSource" variant containing metadata about itself. Consumers declare mod sources via standard dependency notation, which provides early validation and IDE support while respecting isolated projects boundaries.

This approach is consistent with how NeoGradle already uses variants internally (see `variant-aware-configurations.md` for runCompileClasspath/runResourcesOnlyClasspath variants).

### Design Reference: Gradle Documentation

This design leverages three Gradle mechanisms documented at:

1. **Variant Attributes**: https://docs.gradle.org/current/userguide/variant_attributes.html#sec:custom-attributes
   - Custom attributes allow expressing metadata about artifacts (e.g., source set name, project path)
2. **Project Dependencies with Configurations**: https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_plugin_and_dependency_management
   - Standard mechanism for consuming specific variants from other projects via `project(path: ':api', configuration: 'someConfig')`
3. **Isolated Projects Requirements**: https://docs.gradle.org/current/userguide/isolated_projects.html
   - Dependencies are the intended cross-project communication channel under isolated projects mode

### Attribute Definition

Define custom attributes to carry mod source metadata through variant resolution:

```java
public final class ModSourceAttributes {
    /**
     * Attribute key identifying the project path of a mod source.
     */
    public static final Attribute<String> MOD_SOURCE_PROJECT_PATH = 
        Attribute.of("net.neoforged.gradle.mod-source-project-path", String.class);

    /**
     * Attribute key identifying the source set name of a mod source.
     */
    public static final Attribute<String> MOD_SOURCE_NAME = 
        Attribute.of("net.neoforged.gradle.mod-source-name", String.class);
}
```

### Producer Side: Source Set Variant Declaration

For each source set, create a consumable configuration that exposes metadata about the source set as an artifact. This is declared in `CommonProjectPlugin` during source set setup (similar to existing runCompileClasspath variants):

```java
private void configureModSourceVariant(SourceSet sourceSet) {
    Project project = SourceSetUtils.getProject(sourceSet);
    
    // Create a consumable configuration for this source set as a mod source
    Configuration modSourceConfig = project.getConfigurations().create(
        "modSource" + sourceSet.getCapitalizedName(), 
        conf -> {
            conf.setVisible(false);
            conf.setCanBeConsumed(true);
            conf.setCanBeResolved(false); // Only for dependency resolution, not file collection
            
            // Declare attributes carrying metadata about this mod source
            conf.attributes(attrs -> {
                attrs.attribute(ModSourceAttributes.MOD_SOURCE_PROJECT_PATH, project.getPath());
                attrs.attribute(ModSourceAttributes.MOD_SOURCE_NAME, sourceSet.getName());
            });
            
            // Declare outgoing artifacts: the source set output (classes + resources)
            conf.outgoing(outgoing -> {
                outgoing.artifact(sourceSet.getOutput());
                
                // Wire preparation tasks as build dependencies so consumers get them automatically
                outgoing.buildDependencies(deps -> 
                    deps.projectDependency(project.getTasks().named(sourceSet.getCompileJavaTaskName())));
                outgoing.buildDependencies(deps -> 
                    deps.projectDependency(project.getTasks().named(sourceSet.getProcessResourcesTaskName())));
            });
        }
    );
}
```

### Consumer Side: Run Dependency Handler Integration

Add a new dependency type to the run's dependency handler that accepts project dependencies targeting mod source configurations. This integrates with Gradle's standard dependency resolution, providing early validation and IDE support.

**New DSL Usage:**

```groovy
runs {
    server {
        // NEW: Variant-based syntax — validated immediately by Gradle!
        dependencies {
            modSource project(path: ':api', configuration: 'modSourceMain')
            
            // With explicit group ID override for FML mod list organization:
            modSource('customGroup') project(path: ':libs:core', configuration: 'modSourceMain')
        }
        
        // OLD: Still works for backward compatibility (same-project or non-isolated mode)
        modSource sourceSets.main
    }
}
```

**Implementation outline:**

1. Add new methods to `Run` interface's dependency handler:
   ```groovy
   /**
    * Declares a project dependency as a mod source for this run.
    * The referenced configuration must be a consumable modSource variant.
    */
   void modSource(Object dependencyNotation)
   
   /**
    * Declares a project dependency as a mod source with an explicit group ID.
    */
   void modSource(String groupId, Object dependencyNotation)
   ```

2. Store these dependencies in a dedicated configuration on the run (e.g., `runModSources`) that Gradle resolves lazily

3. At execution time, resolve the configuration and extract metadata from resolved variants:
   - Use attribute values to determine project path and source set name
   - Look up actual SourceSet objects via `rootProject.findProject(path).sourceSets[name]` (safe at execution time)
   - Wire build dependencies through variant resolution (Gradle handles this automatically)

### Metadata Extraction from Resolved Variants

When the run needs to use mod sources (for IDE path generation, environment variables, etc.), it resolves the dependency configuration and extracts metadata:

```java
public static void extractModSourcesFromDependencies(Run run, Task task) {
    Configuration modSourceConfig = task.getProject().getConfigurations()
        .findByName("run" + run.getName() + "ModSources");
    
    if (modSourceConfig == null || modSourceConfig.getAllDependencies().isEmpty()) {
        return; // No variant-based mod sources declared
    }
    
    // Resolve the configuration to get incoming variants with metadata
    Set<ResolvedComponentResult> resolved = modSourceConfig.getResolvedConfiguration()
        .getLenientConfiguration().getResolvedArtifacts();
    
    for (ResolvedArtifactResult artifact : resolved) {
        ResolvedVariantResult variant = artifact.getVariant();
        
        // Extract metadata from attributes
        String projectPath = variant.getAttributeValue(ModSourceAttributes.MOD_SOURCE_PROJECT_PATH);
        String sourceSetName = variant.getAttributeValue(ModSourceAttributes.MOD_SOURCE_NAME);
        
        // Look up actual SourceSet at execution time (safe — all projects configured)
        Project targetProject = task.getProject().getRootProject().findProject(projectPath);
        SourceSet sourceSet = targetProject.getExtensions()
            .getByType(SourceSetContainer.class)
            .findByName(sourceSetName);
        
        if (sourceSet != null) {
            // Add to run's mod sources using existing logic
            run.getModSources().add(sourceSet);
        }
    }
}
```

### Why This Provides Early Validation

The key advantage over string-based approaches: Gradle validates dependencies during configuration time, not execution time. If a user writes:

```groovy
dependencies {
    modSource project(path: ':api', configuration: 'modSourceMain')
}
```

Gradle will fail immediately with a clear error if:
- Project `:api` doesn't exist → "Project ':api' not found in root project"
- Configuration `modSourceMain` doesn't exist on `:api` → "Configuration 'modSourceMain' not found in project ':api'"

The error points directly to the line in the build script, providing excellent developer experience. IDEs also provide autocomplete for project paths and configuration names via standard dependency notation support.

### Backward Compatibility

The existing SourceSet-based API remains fully functional:

```groovy
runs {
    server {
        // Still works — same-project access is allowed under isolated projects
        modSource sourceSets.main
        
        // Also still works for non-isolated-projects mode builds
        modSource project(':api').sourceSets.main
    }
}
```

The variant-based approach is additive, not replacement. Users can migrate incrementally:
1. Start using `dependencies { modSource ... }` syntax in isolated projects mode
2. Keep existing `modSource sourceSet` syntax for same-project sourcesets (still valid)
3. Eventually deprecate direct cross-project SourceSet access when isolated projects becomes default

### Handling Group IDs and Mod Identifiers

The current system uses group IDs to organize mod sources in FML mod lists. The variant-based approach preserves this:

1. **Default behavior**: If no explicit group ID is provided, derive it from the source set's `modIdentifier` extension (existing mechanism) or fall back to project path
2. **Explicit override**: Allow users to specify a custom group ID via `modSource('customGroup') notation` syntax

This maps directly onto existing RunSourceSets.add(groupId, sourceSet) semantics — after variant resolution extracts the SourceSet, we use the same grouping logic.

## Migration Path

### Phase 1: Add Producer Variants (Backward Compatible)

Add modSource configurations to each source set in CommonProjectPlugin.configureRunVariants(). No consumer changes yet — existing code continues working unchanged.

### Phase 2: Add Consumer Dependency Handler Methods

Extend Run interface and implementation with `modSource(Object)` methods that accept standard Gradle dependency notation. Store dependencies in a dedicated configuration per run. Implement metadata extraction at execution time.

### Phase 3: Update Isolated Projects Test

Modify the failing test to use new variant-based syntax instead of direct SourceSet access, verifying it works under isolated projects mode.

### Phase 4: Documentation and Deprecation Guidance

Document the new syntax as the recommended approach for multi-project mod sources. Add deprecation warnings when users use `project(':x').sourceSets.y` in isolated projects mode, pointing them to the variant-based alternative.

## Implementation Status (2026-08-09)

### Completed Changes

1. **Cross-project plugin access fixed**: Removed redundant rootProject.pluginManager access from CommonProjectPlugin for IdeaExtPlugin
2. **IdeManagementExtension refactored**: Made rootProject reference lazy, restricted IdeaExtPlugin to root-only application
3. **IdeRunIntegrationManager.setup() guarded**: Added root-project check so IDEA model config only runs on root project

### Remaining Work

1. Define ModSourceAttributes class with custom attributes for metadata carrying
2. Add modSource configurations per source set in CommonProjectPlugin (producer side)
3. Extend Run interface with variant-based dependency methods (consumer side)
4. Implement metadata extraction from resolved variants at execution time
5. Update isolated projects test to use new syntax and verify it passes

## Testing Strategy

1. **Isolated projects multi-project test**: Verify build succeeds with `org.gradle.isolated-projects=true` using variant-based modSource syntax — currently fails, should pass after implementation ✅
2. **Early validation test**: Write invalid dependency notation (non-existent project/config) → verify Gradle fails during configuration with clear error message pointing to script line
3. **Backward compatibility test**: Existing `modSource sourceSets.main` and `modSource project(':api').sourceSets.main` syntax still works in non-isolated mode
4. **Group ID override test**: Verify explicit group IDs are respected when specified via `modSource('group') notation`

## References

- Gradle Variant Attributes: https://docs.gradle.org/current/userguide/variant_attributes.html#sec:custom-attributes
- Gradle Project Dependencies: https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_plugin_and_dependency_management  
- Gradle Isolated Projects: https://docs.gradle.org/current/userguide/isolated_projects.html
- NeoGradle Variant-Aware Configurations Design: `variant-aware-configurations.md`
