package com.bgsoftware.superiorprison.plugin.controller;

import com.bgsoftware.superiorprison.api.controller.BackPackController;
import com.bgsoftware.superiorprison.api.data.backpack.BackPack;
import com.bgsoftware.superiorprison.plugin.SuperiorPrisonPlugin;
import com.bgsoftware.superiorprison.plugin.config.backpack.AdvancedBackPackConfig;
import com.bgsoftware.superiorprison.plugin.config.backpack.BackPackConfig;
import com.bgsoftware.superiorprison.plugin.config.backpack.SimpleBackPackConfig;
import com.bgsoftware.superiorprison.plugin.module.BackPacksModule;
import com.bgsoftware.superiorprison.plugin.object.backpack.SBackPack;
import com.bgsoftware.superiorprison.plugin.object.inventory.PatchedInventory;
import com.bgsoftware.superiorprison.plugin.object.inventory.SPlayerInventory;
import com.oop.orangeengine.main.Engine;
import com.oop.orangeengine.main.plugin.OComponent;
import com.oop.orangeengine.nbt.NBTItem;
import com.oop.orangeengine.yaml.Config;
import com.oop.orangeengine.yaml.ConfigSection;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SBackPackController implements BackPackController, OComponent<SuperiorPrisonPlugin> {

    public static final String NBT_KEY = "BACKPACK_DATA";
    public static final String UUID_KEY = "BACKPACK_UUID";

    private final Map<String, BackPackConfig<?>> backpackConfigs = new HashMap<>();
    private boolean playerBound = false;

    @Override
    public boolean isBackPack(@NonNull ItemStack itemStack) {
        if (BackPacksModule.isDisabled()) return false;
        return new NBTItem(itemStack).getKeys()
                .stream()
                .anyMatch(in -> in.startsWith(NBT_KEY));
    }

    @Override
    public BackPack getBackPack(@NonNull ItemStack itemStack, Player player)  {
        if (BackPacksModule.isDisabled()) return null;

        if (!(player.getInventory() instanceof PatchedInventory)) {
            try {
                return new SBackPack(itemStack, player);
            } catch (Throwable t) {
                throw new IllegalStateException("Invalid backpack of " + player.getName() + ", item: " + itemStack, t);
            }
        }

        SPlayerInventory owner = ((PatchedInventory) player.getInventory()).getOwner();
        SBackPack backPack =  owner.findBackPackBy(itemStack);
        if (backPack == null) {
            try {
                return new SBackPack(itemStack, player);
            } catch (Throwable t) {
                throw new IllegalStateException("Invalid backpack of " + player.getName() + ", item: " + itemStack, t);
            }
        }
        return backPack;
    }

    @Override
    public BackPack getBackPack(int slot, @NonNull Player player) {
        if (BackPacksModule.isDisabled()) return null;

        if (!(player.getInventory() instanceof PatchedInventory)) {
            ItemStack item = player.getInventory().getItem(slot);
            return getBackPack(item, player);
        }
        SPlayerInventory owner = ((PatchedInventory) player.getInventory()).getOwner();
        return owner.getBackPackMap().get(slot);
    }

    public Optional<BackPackConfig<?>> getConfig(String name) {
        return Optional.ofNullable(backpackConfigs.get(name));
    }

    @Override
    public boolean isPlayerBound() {
        return playerBound;
    }

    @Override
    public List<BackPack> findBackPacks(Player player) {
        List<BackPack> backPacks = new LinkedList<>();
        if (BackPacksModule.isDisabled()) return backPacks;

        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null || content.getType() == Material.AIR) continue;
            if (isBackPack(content)) backPacks.add(getBackPack(content, player));
        }
        return backPacks;
    }

    @Override
    public boolean load() {
        if (BackPacksModule.isDisabled()) return true;

        backpackConfigs.clear();
        Config backPacksConfig = SuperiorPrisonPlugin.getInstance().getConfigController().getBackPacksConfig();

        for (ConfigSection section : backPacksConfig.getSections().values()) {
            if (section.getKey().contentEquals("global options")) {
                this.playerBound = section.getAs("player bound");
                continue;
            }

            try {
                if (section.get("type").map(o -> o.getAs(String.class)).orElse("advanced").equalsIgnoreCase("advanced"))
                    backpackConfigs.put(section.getKey(), new AdvancedBackPackConfig(section));
                else
                    backpackConfigs.put(section.getKey(), new SimpleBackPackConfig(section));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                getEngine().getLogger().printWarning("Failed to initialize backpack by id {}, check for mistakes!", section.getKey());
            }
        }
        backPacksConfig.save();

        Engine.getInstance().getLogger().print("Loaded {} backpacks", backpackConfigs.size());
        return true;
    }

    public Map<String, BackPackConfig<?>> getConfigs() {
        return backpackConfigs;
    }

    public UUID getUUID(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return null;
        NBTItem nbtItem = new NBTItem(itemStack);

        String serializedUUID = nbtItem.getString(UUID_KEY);
        if (serializedUUID.trim().length() == 0) return null;

        return UUID.fromString(serializedUUID);
    }
}
