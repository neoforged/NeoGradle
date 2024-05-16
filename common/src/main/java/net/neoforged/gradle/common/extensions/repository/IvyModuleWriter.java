/*
 * This file is part of VanillaGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.neoforged.gradle.common.extensions.repository;

import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryEntryLegacy;
import net.neoforged.gradle.util.IndentingXmlStreamWriter;
import net.neoforged.gradle.util.ModuleDependencyUtils;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class IvyModuleWriter implements AutoCloseable {

    private static final String XSI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
    private static final String IVY = "http://ant.apache.org/ivy/schemas/ivy.xsd";
    private static final String NEOGRADLE = "https://neoforged.net/neogradle/ivy-extra";

    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();
    private static final String INDENT = "  ";

    private final boolean managedOutput;
    private final Writer output;
    private final XMLStreamWriter writer;

    public IvyModuleWriter(final Writer output) throws XMLStreamException {
        this.managedOutput = false;
        this.output = output;
        this.writer = new IndentingXmlStreamWriter(IvyModuleWriter.OUTPUT_FACTORY.createXMLStreamWriter(output), INDENT);
    }

    public IvyModuleWriter(final Path target) throws IOException, XMLStreamException {
        this.managedOutput = true;
        this.output = Files.newBufferedWriter(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        this.writer = new IndentingXmlStreamWriter(IvyModuleWriter.OUTPUT_FACTORY.createXMLStreamWriter(this.output), INDENT);
    }

    public void write(final Dependency descriptor, Configuration dependencies) throws XMLStreamException {
        this.writer.writeStartDocument("UTF-8", "1.0");
        this.writer.writeStartElement("ivy-module");
        this.writer.writeNamespace("xsi", IvyModuleWriter.XSI);
        this.writer.writeNamespace("NeoGradle", IvyModuleWriter.NEOGRADLE);
        this.writer.writeAttribute(IvyModuleWriter.XSI, "noNamespaceSchemaLocation", IvyModuleWriter.IVY);
        this.writer.writeAttribute("version", "2.0");

        this.writeInfo(descriptor);
        this.writeDependencies(dependencies);

        this.writer.writeEndElement();
        this.writer.writeEndDocument();
    }

    private void writeInfo(final Dependency entry) throws XMLStreamException {
        this.writer.writeStartElement("info");
        // Common attributes
        if (entry.getGroup() != null)
            this.writer.writeAttribute("organisation", entry.getGroup());

        this.writer.writeAttribute("module", entry.getName());
        this.writer.writeAttribute("revision", entry.getVersion());
        this.writer.writeAttribute("status", "release"); // gradle wants release... we must please the gradle...

        // License
        // TODO: deal with custom projects?
        this.writer.writeEmptyElement("license");
        this.writer.writeAttribute("name", "Minecraft EULA");
        this.writer.writeAttribute("url", "https://www.minecraft.net/en-us/eula");

        // End
        this.writer.writeEndElement();
    }

    private void writeDependencies(final Configuration dependencies) throws XMLStreamException {
        this.writer.writeStartElement("dependencies");

        for (final Dependency extra : dependencies.getDependencies()) {
            this.writeDependency(extra);
        }

        this.writer.writeEndElement();
    }

    private void writeDependency(final Dependency dep) throws XMLStreamException {
        final String classifier = ModuleDependencyUtils.getClassifierOrEmpty(dep);
        final boolean hasClassifier = !classifier.isEmpty();

        if (hasClassifier) {
            this.writer.writeStartElement("dependency");
        } else {
            this.writer.writeEmptyElement("dependency");
        }

        if (dep instanceof RepositoryEntryLegacy) {
            final RepositoryEntryLegacy<?,?> entry = (RepositoryEntryLegacy<?,?>) dep;
            this.writer.writeAttribute("org", entry.getFullGroup());
        } else {
            this.writer.writeAttribute("org", dep.getGroup());
        }
        this.writer.writeAttribute("name", dep.getName());
        this.writer.writeAttribute("rev", dep.getVersion());
        this.writer.writeAttribute("transitive", "false");

        if (hasClassifier) {
            this.writer.writeEmptyElement("artifact");
            this.writer.writeAttribute("name", dep.getName());
            this.writer.writeAttribute("classifier", classifier);
            this.writer.writeAttribute("ext", "jar");
            this.writer.writeEndElement();
        }
    }

    @Override
    public void close() throws IOException, XMLStreamException {
        this.writer.close();

        if (this.managedOutput) {
            this.output.close();
        }
    }
}
