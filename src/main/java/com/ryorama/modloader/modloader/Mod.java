package com.ryorama.modloader.modloader;

import com.ryorama.modloader.api.ModInit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class Mod {

    private final String mainClass;
    private final Path file;
    private final JarFile jarMod;
    private final String ID;
    private final String version;
    public ModInit implementation;

    public Mod(Path file) throws IOException {
        this.file = file;
        this.jarMod = new JarFile( file.toFile(), false );
        Attributes attributes = this.jarMod.getManifest().getMainAttributes();
        this.version = attributes.getValue("Implementation-Version");
        this.mainClass = attributes.getValue("Main-Class");
        this.ID = attributes.getValue("Modid");
    }

    public String getName() {
        return this.file.getFileName().toString().replace(".jar", "");
    }

    public String getVersion() {
        return version;
    }

    public String getID() {
        return ID;
    }

    public JarFile getJarMod() {
        return jarMod;
    }

    public Path getPath() {
        return file;
    }

    public String getMainClass() {
        return mainClass;
    }
}
