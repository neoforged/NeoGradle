package net.minecraftforge.gradle.dsl.common.runtime.spec.builder;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

@SuppressWarnings("UnusedReturnValue")
