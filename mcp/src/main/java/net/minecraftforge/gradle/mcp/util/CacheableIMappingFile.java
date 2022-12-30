package net.minecraftforge.gradle.mcp.util;

import net.minecraftforge.gradle.common.util.IMappingFileUtils;
import net.minecraftforge.gradle.dsl.common.tasks.ForgeGradleBase;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class CacheableIMappingFile implements IMappingFile, Serializable {

    private IMappingFile delegate;

    public CacheableIMappingFile(IMappingFile delegate) {
        this.delegate = delegate;
    }

    @Override
    public Collection<? extends IPackage> getPackages() {
        return this.delegate.getPackages();
    }

    @Override
    public IPackage getPackage(String original) {
        return this.delegate.getPackage(original);
    }

    @Override
    public Collection<? extends IClass> getClasses() {
        return this.delegate.getClasses();
    }

    @Override
    public IClass getClass(String original) {
        return this.delegate.getClass(original);
    }

    @Override
    public String remapPackage(String pkg) {
        return this.delegate.remapClass(pkg);
    }

    @Override
    public String remapClass(String desc) {
        return this.delegate.remapClass(desc);
    }

    @Override
    public String remapDescriptor(String desc) {
        return this.delegate.remapDescriptor(desc);
    }

    @Override
    public void write(Path path, Format format, boolean reversed) throws IOException {
        this.delegate.write(path, format, reversed);
    }

    @Override
    public IMappingFile reverse() {
        return new CacheableIMappingFile(this.delegate.reverse());
    }

    @Override
    public IMappingFile rename(IRenamer renamer) {
        return new CacheableIMappingFile(this.delegate.rename(renamer));
    }

    @Override
    public IMappingFile chain(IMappingFile other) {
        return new CacheableIMappingFile(this.delegate.chain(other));
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        final List<String> clientLines = IMappingFileUtils.writeMappingFile(delegate, IMappingFile.Format.TSRG2, false);

        out.writeObject(clientLines);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        final List<String> clientLines = (List<String>) in.readObject();

        InputStream clientStream = new ByteArrayInputStream(String.join("\n", clientLines).getBytes());

        delegate = IMappingFile.load(clientStream);
    }

    private void readObjectNoData() throws ObjectStreamException {
        throw new UnsupportedOperationException();
    }
}
