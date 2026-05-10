package net.neoforged.gradle.common.services.caching;

import com.google.common.collect.Sets;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.io.File;
import java.io.IOException;

public abstract class CachedExecutionService implements BuildService<CachedExecutionService.Parameters>
{

    public static final String NAME = "CachedExecutionService";

    public static final String DIRECTORY_NAME = "ng_execute";

    public static final String CACHING_PROPERTY_PREFIX    = "net.neoforged.gradle.caching.";
    public static final String CACHE_DIRECTORY_PROPERTY   = CACHING_PROPERTY_PREFIX + "cacheDirectory";
    public static final String LOG_CACHE_HITS_PROPERTY    = CACHING_PROPERTY_PREFIX + "logCacheHits";
    public static final String MAX_CACHE_SIZE_PROPERTY    = CACHING_PROPERTY_PREFIX + "maxCacheSize";
    public static final String DEBUG_CACHE_PROPERTY       = CACHING_PROPERTY_PREFIX + "debug";
    public static final String IS_ENABLED_PROPERTY        = CACHING_PROPERTY_PREFIX + "enabled";
    public static final String TASK_PATH_FILTERS_PROPERTY = CACHING_PROPERTY_PREFIX + "taskPathFilters";

    public interface Parameters extends BuildServiceParameters
    {

        Property<String> getCacheDirectory();

        Property<Boolean> getLogCacheHits();

        Property<Integer> getMaxCacheSize();

        Property<Boolean> getDebugCache();

        Property<Boolean> getIsEnabled();

        SetProperty<String> getTaskPathFiltersForLogging();
    }

    public static void register(Project project)
    {
        project.getGradle().getSharedServices().registerIfAbsent(
            NAME,
            CachedExecutionService.class,
            spec -> {
                spec.getParameters().getCacheDirectory()
                    .set(project.getProviders().gradleProperty(CACHE_DIRECTORY_PROPERTY)
                        .orElse(new File(new File(project.getGradle().getGradleUserHomeDir(), "caches"), DIRECTORY_NAME).getAbsolutePath()));
                spec.getParameters().getLogCacheHits().set(project.getProviders().gradleProperty(LOG_CACHE_HITS_PROPERTY).map(Boolean::parseBoolean).orElse(false));
                spec.getParameters().getMaxCacheSize().set(project.getProviders().gradleProperty(MAX_CACHE_SIZE_PROPERTY).map(Integer::parseInt).orElse(100));
                spec.getParameters().getDebugCache().set(project.getProviders().gradleProperty(DEBUG_CACHE_PROPERTY).map(Boolean::parseBoolean).orElse(false));
                spec.getParameters().getIsEnabled().set(project.getProviders().gradleProperty(IS_ENABLED_PROPERTY).map(Boolean::parseBoolean).orElse(true));
                spec.getParameters().getTaskPathFiltersForLogging().set(
                    project.getProviders().gradleProperty(TASK_PATH_FILTERS_PROPERTY)
                        .map(value -> Sets.newHashSet(value.split(";")))
                        .orElse(Sets.newHashSet())
                );
            }
        );
    }

    public void clean() throws IOException
    {
        FileUtils.cleanDirectory(getCacheDirectory());
    }

    private File getCacheDirectory()
    {
        return new File(getParameters().getCacheDirectory().get());
    }

    public <T> CachedExecutionBuilder<T> cached(
        Task task,
        ICacheableJob<Void, T> initial
    )
    {
        return new CachedExecutionBuilder<>(
            new CachedExecutionBuilder.Options(
                getParameters().getIsEnabled().get(),
                getCacheDirectory(),
                new CachedExecutionBuilder.LoggingOptions(
                    getParameters().getLogCacheHits().get(),
                    getParameters().getDebugCache().get(),
                    getParameters().getTaskPathFiltersForLogging().get()
                )
            ),
            task,
            initial
        );
    }
}
