/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.common.tasks;

import com.google.common.collect.Maps;
import groovy.lang.Closure;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.DefaultTask;
import org.gradle.api.NonNullApi;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.io.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipOutputStream;

@CacheableTask
@NonNullApi
public abstract class PotentiallySignJar extends DefaultTask implements PatternFilterable, WithOutput, WithWorkspace {
    private final PatternSet patternSet = new PatternSet();
    
    public PotentiallySignJar() {
        getOutputFileName().convention(getInput().map(RegularFile::getAsFile).map(file -> FileUtils.postFixClassifier(file, "signed")));
        getOutput().convention(getOutputFileName().flatMap(fileName -> getLayout().getBuildDirectory().dir("libs").map(libsDir -> libsDir.file(fileName))));
    }
    
    @TaskAction
    public void doTask() throws IOException {
        final Map<String, Entry<byte[], Long>> ignoredStuff = Maps.newHashMap();
        
        
        File input = getInput().get().getAsFile();
        File output = ensureFileWorkspaceReady(getOutput());
        
        if (!getAlias().isPresent() || !getStorePass().isPresent()) {
            org.apache.commons.io.FileUtils.copyFile(input, output);
            return;
        }
        
        File toSign = ensureFileWorkspaceReady(new File(getTemporaryDir(), input.getName() + ".unsigned.tmp"));
        File signed = ensureFileWorkspaceReady(new File(getTemporaryDir(), input.getName() + ".signed.tmp"));
        
        // load in input jar, and create temp jar
        processInputJar(input, toSign);
        
        // SIGN!
        Map<String, Object> map = Maps.newHashMap();
        map.put("alias", getAlias().get());
        map.put("storePass", getStorePass().get());
        map.put("jar", toSign.getAbsolutePath());
        map.put("signedJar", signed.getAbsolutePath());
        
        if (getKeyPass().isPresent()) map.put("keypass", getKeyPass().get());
        if (getKeyStore().isPresent()) map.put("keyStore", getKeyStore().get());
        
        getAnt().invokeMethod("signjar", map);
        
        // write out
        writeOutputJar(signed, input, output);
    }
    
    private void processInputJar(File inputJar, File toSign) throws IOException {
        final FileTree inputFileTree = getArchiveOperations().zipTree(inputJar);
        try (OutputStream toSignOutputStream = new FileOutputStream(toSign); ZipOutputStream toSignZipOutputStream = new ZipOutputStream(toSignOutputStream)) {
            
            ZipBuildingFileTreeVisitor toSignBuilder = new ZipBuildingFileTreeVisitor(toSignZipOutputStream);
            inputFileTree.matching(patternSet).visit(toSignBuilder);
        }
    }
    
    private void writeOutputJar(File signedJar, File inputJar, File outputJar) throws IOException {
        final FileTree inputFileTree = getArchiveOperations().zipTree(inputJar);
        final FileTree signedFileTree = getArchiveOperations().zipTree(signedJar);
        try (OutputStream outputStream = new FileOutputStream(outputJar); ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            
            ZipBuildingFileTreeVisitor builder = new ZipBuildingFileTreeVisitor(zipOutputStream);
            signedFileTree.visit(builder);
            inputFileTree.minus(inputFileTree.matching(patternSet)).getAsFileTree().visit(builder);
        }
    }
    
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();
    
    @Input
    @Optional
    public abstract Property<String> getAlias();
    
    @Input
    @Optional
    public abstract Property<String> getStorePass();
    
    @Input
    @Optional
    public abstract Property<String> getKeyPass();
    
    @Input
    @Optional
    public abstract Property<String> getKeyStore();
    
    @Override
    public PatternFilterable exclude(String... arg0) {
        return patternSet.exclude(arg0);
    }
    
    @Override
    public PatternFilterable exclude(Iterable<String> arg0) {
        return patternSet.exclude(arg0);
    }
    
    @Override
    public PatternFilterable exclude(Spec<FileTreeElement> arg0) {
        return patternSet.exclude(arg0);
    }
    
    @Override
    public PatternFilterable exclude(Closure arg0) {
        return patternSet.exclude(arg0);
    }
    
    @Internal
    @Override
    public Set<String> getExcludes() {
        return patternSet.getExcludes();
    }
    
    @Internal
    @Override
    public Set<String> getIncludes() {
        return patternSet.getIncludes();
    }
    
    @Override
    public PatternFilterable include(String... arg0) {
        return patternSet.include(arg0);
    }
    
    @Override
    public PatternFilterable include(Iterable<String> arg0) {
        return patternSet.include(arg0);
    }
    
    @Override
    public PatternFilterable include(Spec<FileTreeElement> arg0) {
        return patternSet.include(arg0);
    }
    
    @Override
    public PatternFilterable include(Closure arg0) {
        return patternSet.include(arg0);
    }
    
    @Override
    public PatternFilterable setExcludes(Iterable<String> arg0) {
        return patternSet.setExcludes(arg0);
    }
    
    @Override
    public PatternFilterable setIncludes(Iterable<String> arg0) {
        return patternSet.setIncludes(arg0);
    }
}
