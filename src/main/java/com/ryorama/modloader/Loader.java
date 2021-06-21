package com.ryorama.modloader;

import com.ryorama.modloader.api.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface Loader {

    Registry getRegistry();
    String getMinecraftVersion();
    String getLoaderVersion();
    boolean isModLoaded(String id);
    default Logger getLogger(String modid) {
        return LogManager.getLogger(modid);
    }

}
