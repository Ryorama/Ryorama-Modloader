package com.ryorama.modloader;

import com.ryorama.modloader.api.ModInit;
import com.ryorama.modloader.modloader.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class ModLoader {
    private final Path MODS_PATH;
    private final ArrayList<Mod> mods = new ArrayList<>();
    private final DynamicClassLoader classLoader = new DynamicClassLoader( new URL[] {}, this.getClass().getClassLoader() );

    public ModLoader() {
        MODS_PATH = Paths.get( System.getProperty("user.dir"), "mods" );
    }

    @SuppressWarnings("unchecked")
    public void loadMods() {
        final ArrayList<URL> urls = new ArrayList<>();

        RyoramaModloader.logger.info("FINDING MODS");
        for ( File file : Objects.requireNonNull( this.MODS_PATH.toFile().listFiles() ) ) {
            if( file.getName().endsWith(".jar") ) {
                try {
                    this.classLoader.addURL( file.toURI().toURL() );
                    this.mods.add( new Mod( Paths.get( file.getPath() ) ) );
                } catch (IOException e) {
                    RyoramaModloader.logger.error("Failed to load possible mod: " + file.getName() );
                }
            }
        }

        RyoramaModloader.logger.info("INSTANTIATING MODS");
        for ( Mod mod : this.mods ) {
            try {
                Class<?> classToLoad = Class.forName( mod.getMainClass(), true, classLoader );
                if (
                        Arrays.stream( classToLoad.getInterfaces() )
                                .anyMatch( iface -> iface.getSimpleName().equals("ContentMod") )
                ) {
                    Class<? extends ModInit> modToLoad = (Class<? extends ModInit>) classToLoad;
                    mod.implementation = modToLoad.newInstance();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                RyoramaModloader.logger.error( "can't load mod file: " + mod.getPath() );
            }
        }

    }

    public ArrayList<Mod> getMods() {
        return this.mods;
    }


    private static class DynamicClassLoader extends URLClassLoader {
        public DynamicClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}
