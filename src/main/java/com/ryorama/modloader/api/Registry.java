package com.ryorama.modloader.api;

import com.ryorama.modloader.api.block.Block;
import com.ryorama.modloader.api.item.Item;
import com.ryorama.modloader.api.utils.RegistryKey;
import com.ryorama.modloader.api.utils.ResourceId;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public interface Registry {

    void register(Block block, ResourceId identifier);
    void register(Block block, boolean registerItem, ResourceId identifier);
    void register(Item Item, ResourceId identifier);

    boolean isRegistered(RegistryKey key, String identifier ) throws OperationNotSupportedException;
    boolean isRegistered(RegistryKey key,ResourceId identifier ) throws OperationNotSupportedException;

    @Nullable Object getVanillaRegistry(RegistryKey key);

}
