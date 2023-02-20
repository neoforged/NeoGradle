package net.minecraftforge.gradle.common.runtime.naming.renamer;

import net.minecraftforge.gradle.util.IMappingFileUtils;
import net.minecraftforge.srgutils.IMappingFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.util.List;
import java.util.Objects;

public class IMappingFileTypeRenamer implements ITypeRenamer {

    public static ITypeRenamer from(final File clientFile, final File serverFile) throws IOException {
        return new IMappingFileTypeRenamer(IMappingFile.load(clientFile), IMappingFile.load(serverFile));
    }

    public static ITypeRenamer from(final IMappingFile clientMappings, final IMappingFile serverMappings) {
        Objects.requireNonNull(clientMappings, "clientMappings");
        Objects.requireNonNull(serverMappings, "serverMappings");

        return new IMappingFileTypeRenamer(clientMappings, serverMappings);
    }

    private IMappingFile clientMappings;
    private IMappingFile serverMappings;

    private IMappingFileTypeRenamer(IMappingFile clientMappings, IMappingFile serverMappings) {
        this.clientMappings = clientMappings;
        this.serverMappings = serverMappings;
    }

    @Override
    public String renameType(String type) {
        final String clientType = clientMappings.remapClass(type.replace('.', '/')).replace('.', '/');
        if (Objects.equals(clientType, type))
            return serverMappings.remapClass(type.replace('.', '/')).replace('.', '/');

        return clientType;
    }

    @Override
    public String renameField(String owner, String name) {
        final IMappingFile.IClass clientClass = clientMappings.getClass(owner.replace('.', '/'));
        if (clientClass != null) {
            final String clientName = clientClass.remapField(name);
            if (!Objects.equals(clientName, name))
                return clientName;
        }

        final IMappingFile.IClass serverClass = serverMappings.getClass(owner.replace('.', '/'));
        if (serverClass != null) {
            final String serverName = serverClass.remapField(name);
            if (!Objects.equals(serverName, name))
                return serverName;
        }

        return name;
    }

    @Override
    public String renameMethod(String owner, String name, String desc) {
        final IMappingFile.IClass clientClass = clientMappings.getClass(owner.replace('.', '/'));
        if (clientClass != null) {
            final String clientName = clientClass.remapMethod(name, desc);
            if (!Objects.equals(clientName, name))
                return clientName;
        }

        final IMappingFile.IClass serverClass = serverMappings.getClass(owner.replace('.', '/'));
        if (serverClass != null) {
            final String serverName = serverClass.remapMethod(name, desc);
            if (!Objects.equals(serverName, name))
                return serverName;
        }

        return name;
    }

    @Override
    public String renameDescriptor(String desc) {
        final String clientDescriptor = clientMappings.remapDescriptor(desc);
        if (!Objects.equals(clientDescriptor, desc))
            return clientDescriptor;

        final String serverDescriptor = serverMappings.remapDescriptor(desc);
        if (!Objects.equals(serverDescriptor, desc))
            return serverDescriptor;

        return desc;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        final List<String> clientLines = IMappingFileUtils.writeMappingFile(clientMappings, IMappingFile.Format.TSRG2, false);
        final List<String> serverLines = IMappingFileUtils.writeMappingFile(serverMappings, IMappingFile.Format.TSRG2, false);

        out.writeObject(clientLines);
        out.writeObject(serverLines);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        final List<String> clientLines = (List<String>) in.readObject();
        final List<String> serverLines = (List<String>) in.readObject();

        InputStream clientStream = new ByteArrayInputStream(String.join("\n", clientLines).getBytes());
        InputStream serverStream = new ByteArrayInputStream(String.join("\n", serverLines).getBytes());

        clientMappings = IMappingFile.load(clientStream);
        serverMappings = IMappingFile.load(serverStream);
    }

    private void readObjectNoData() throws ObjectStreamException {
        throw new UnsupportedOperationException();
    }
}
