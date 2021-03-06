package com.bgsoftware.superiorprison.plugin.controller;

import com.bgsoftware.superiorprison.api.controller.BlockController;
import com.bgsoftware.superiorprison.api.data.mine.SuperiorMine;
import com.bgsoftware.superiorprison.api.data.mine.area.AreaEnum;
import com.bgsoftware.superiorprison.api.data.mine.locks.Lock;
import com.bgsoftware.superiorprison.api.data.mine.type.NormalMine;
import com.bgsoftware.superiorprison.api.data.player.Prisoner;
import com.bgsoftware.superiorprison.api.data.player.booster.DropsBooster;
import com.bgsoftware.superiorprison.api.data.player.booster.MoneyBooster;
import com.bgsoftware.superiorprison.api.data.player.booster.XPBooster;
import com.bgsoftware.superiorprison.api.event.mine.MultiBlockBreakEvent;
import com.bgsoftware.superiorprison.api.util.Pair;
import com.bgsoftware.superiorprison.plugin.SuperiorPrisonPlugin;
import com.bgsoftware.superiorprison.plugin.block.DropsHandler;
import com.bgsoftware.superiorprison.plugin.block.XpHandler;
import com.bgsoftware.superiorprison.plugin.hook.impl.VaultHook;
import com.bgsoftware.superiorprison.plugin.object.mine.SMineBlockData;
import com.bgsoftware.superiorprison.plugin.object.mine.SNormalMine;
import com.bgsoftware.superiorprison.plugin.object.mine.locks.SBLocksLock;
import com.bgsoftware.superiorprison.plugin.object.player.SPrisoner;
import com.bgsoftware.superiorprison.plugin.util.SPair;
import com.bgsoftware.superiorprison.plugin.util.XPUtil;
import com.google.common.base.Preconditions;
import com.oop.orangeengine.item.custom.OItem;
import com.oop.orangeengine.main.util.data.pair.OPair;
import com.oop.orangeengine.material.OMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.oop.orangeengine.main.events.AsyncEvents.async;

public class SBlockController implements BlockController {

    @Override
    public MultiBlockBreakEvent handleBlockBreak(Prisoner prisoner, SuperiorMine mine, ItemStack tool, Lock lock, Location... locations) {
        return handleBlockBreak(prisoner, mine, tool, lock, null, locations);
    }

    public MultiBlockBreakEvent handleBlockBreak(Prisoner prisoner, SuperiorMine mine, ItemStack tool, Lock lock, Consumer<MultiBlockBreakEvent> beforeCall, Location... locations) {
        boolean silkTouch = false;
        boolean hasFortune = false;
        int fortuneLevel = -1;
        OItem item;

        if (tool != null) {
            item = new OItem(tool);
            silkTouch = item.hasEnchant(Enchantment.SILK_TOUCH);
            hasFortune = item.hasEnchant(Enchantment.LOOT_BONUS_BLOCKS);
            fortuneLevel = item.getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS);
        }

        boolean addToLock = false;
        if (lock == null) {
            lock = mine.getGenerator().getBlockData().newBlockDataLock();
            addToLock = true;
        }

        Set<DropsBooster> dropsBoosters = prisoner.getBoosters().findBoostersBy(DropsBooster.class);
        double[] dropRate = new double[]{0};
        dropsBoosters.forEach(booster -> dropRate[0] = dropRate[0] + booster.getRate());

        Set<XPBooster> xpBoosters = prisoner.getBoosters().findBoostersBy(XPBooster.class);
        double[] xpRate = new double[]{0};
        xpBoosters.forEach(booster -> xpRate[0] = xpRate[0] + booster.getRate());

        // Get the drops
        Map<Location, Pair<Material, List<ItemStack>>> blockData = new HashMap<>();

        boolean finalSilkTouch = silkTouch;
        boolean finalHasFortune = hasFortune;
        int finalFortuneLevel = fortuneLevel;
        int[] experience = new int[]{0};

        SMineBlockData mineBlockData = (SMineBlockData) mine.getGenerator().getBlockData();
        Map<OMaterial, Integer> materialsAmount = new HashMap<>();

        boolean finalAddToLock = addToLock;
        Lock finalLock = lock;
        for (Location location : locations) {
            if (!mine.getArea(AreaEnum.MINE).isInsideWithoutY(location)) continue;
            Optional<OMaterial> oMaterialAt = mineBlockData
                    .getOMaterialAt(location);

            oMaterialAt.ifPresent(mat -> {
                if (finalAddToLock) {
                    if (mine.getGenerator().getBlockData().isLocked(location))
                        return;
                    else
                        ((SBLocksLock) finalLock).getLockedLocations().add(location);
                }
                ((SBLocksLock) finalLock).getLockedLocations().add(location);

                List<ItemStack> drops = new ArrayList<>();

                // Handle auto burn
                if (prisoner.isAutoBurn()) {
                    if (mat == OMaterial.GOLD_ORE)
                        drops.add(OMaterial.GOLD_INGOT.parseItem());
                    else if (mat == OMaterial.IRON_ORE)
                        drops.add(OMaterial.IRON_INGOT.parseItem());
                    else
                        drops.add(finalSilkTouch ? mat.parseItem() : DropsHandler.getDrop(mat));
                } else
                    drops.add(finalSilkTouch ? mat.parseItem() : DropsHandler.getDrop(mat));

                // Handle fortune
                if (finalHasFortune)
                    new HashSet<>(drops).forEach(itemStack -> {
                        drops.remove(itemStack);
                        drops.addAll(getItems(itemStack, getItemCountWithFortune(itemStack.getType(), finalFortuneLevel)));
                    });

                // Handle dropsBooster
                if (dropRate[0] != 0)
                    drops.forEach(itemStack -> itemStack.setAmount((int) Math.round(dropRate[0] * itemStack.getAmount())));

                // Get the experience
                experience[0] = experience[0] + XpHandler.getEXP(mat, tool);

                if (xpRate[0] != 0)
                    experience[0] = (int) Math.ceil(experience[0] * xpRate[0]);

                blockData.put(location, new SPair<>(
                        mat.parseMaterial(),
                        drops));

                materialsAmount.merge(mat, 1, Integer::sum);
            });
        }

        // Call block break event for the blocks
        MultiBlockBreakEvent event = new MultiBlockBreakEvent(mine, prisoner, tool, blockData, lock, experience[0]);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            event.setCancelled(true);
            return event;
        }

        materialsAmount.forEach((mat, amount) -> SuperiorPrisonPlugin.getInstance().getStatisticsController().getContainer(prisoner.getUUID()).getBlocksStatistic().update(mat, amount));

        // Handle auto selling
        if (prisoner.isAutoSell()) {
            BigDecimal[] deposit = new BigDecimal[]{new BigDecimal(0)};

            // Get prices of the drops
            blockData.values()
                    .forEach(c -> {
                        c.getValue().removeIf(itemStack -> {
                            BigDecimal price = prisoner.getPrice(itemStack);
                            if (price.intValue() == 0) return false;

                            deposit[0] = deposit[0].add(price);
                            return true;
                        });
                    });

            // Apply money dropsBooster
            prisoner.getBoosters()
                    .findBoostersBy(MoneyBooster.class)
                    .forEach(booster -> deposit[0] = deposit[0].multiply(BigDecimal.valueOf(booster.getRate())));

            // Finally deposit
            SuperiorPrisonPlugin.getInstance().getHookController().executeIfFound(() -> VaultHook.class, hook -> hook.depositPlayer((SPrisoner) prisoner, deposit[0]));
        }

        // Handle auto pickup
        if (prisoner.isAutoPickup()) {
            blockData.values()
                    .forEach(d -> {
                        HashMap<Integer, ItemStack> left = prisoner.getPlayer().getInventory().addItem(d.getValue().toArray(new ItemStack[0]));
                        d.getValue().clear();
                        d.getValue().addAll(left.values());
                    });

            XPUtil.setTotalExperience(prisoner.getPlayer(), XPUtil.getTotalExperience(prisoner.getPlayer()) + event.getExperience());
            event.setExperience(0);
        }

        if (addToLock)
            mine.getGenerator().getBlockData().unlock(lock);
        else
            for (Location location : event.getBlockData().keySet())
                mineBlockData.remove(location);

        if (!mine.getSettings().getResetSettings().isTimed()) {
            async(() -> {
                int percentageOfFullBlocks = mineBlockData.getPercentageLeft();
                long percentageRequired = mine.getSettings().getResetSettings().asPercentage().getValue();

                if (percentageOfFullBlocks <= percentageRequired) {
                    mine.getGenerator().reset();
                }
            });
        }

        return event;
    }

    @Override
    public MultiBlockBreakEvent handleBlockBreak(Prisoner prisoner, SuperiorMine mine, BlockBreakEvent event) {
        return handleBlockBreak(prisoner, mine, event.getPlayer().getItemInHand(), null, event.getBlock().getLocation());
    }

    @Override
    public MultiBlockBreakEvent breakBlock(Prisoner prisoner, SuperiorMine mine, ItemStack tool, Location... locations) {
        return breakBlock(prisoner, mine, tool, null, locations);
    }

    @Override
    public MultiBlockBreakEvent breakBlock(Prisoner prisoner, SuperiorMine mine, ItemStack tool, Consumer<MultiBlockBreakEvent> beforeCall, Location... locations) {
        Preconditions.checkArgument(Bukkit.isPrimaryThread(), "Cannot call break block in non main thread!");

        MultiBlockBreakEvent multiBlockBreakEvent = handleBlockBreak(prisoner, mine, tool, null, locations);
        if (multiBlockBreakEvent.isCancelled())
            return multiBlockBreakEvent;

        if (locations.length == 1)
            SuperiorPrisonPlugin.getInstance().getNms().setBlockAndUpdate(locations[0].getChunk(), locations[0], OMaterial.AIR, mine.getWorld().getPlayers());

        else {
            Map<OPair<Integer, Integer>, Chunk> chunkMap = new HashMap<>();
            for (Location location : multiBlockBreakEvent.getBlockData().keySet()) {
                Chunk chunk = chunkMap.get(new OPair<>(location.getBlockX() >> 4, location.getBlockZ() >> 4));
                if (chunk == null) {
                    chunk = location.getChunk();
                    chunkMap.put(new OPair<>(location.getBlockX() >> 4, location.getBlockZ() >> 4), chunk);
                }

                SuperiorPrisonPlugin.getInstance().getNms().setBlock(chunk, location, OMaterial.AIR);
            }

            SuperiorPrisonPlugin.getInstance().getNms().refreshChunks(mine.getWorld(), multiBlockBreakEvent.getBlockData().keySet(), mine.getWorld().getPlayers());
        }

        if (tool != null) {
            Player player = prisoner.getPlayer();
            OItem item = new OItem(tool);

            if (tool.getType().getMaxDurability() > 0) {
                if (!player.hasPermission("prison.prisoner.ignoredurability")) {
                    int enchantmentLevel = item.getEnchantLevel(Enchantment.DURABILITY);
                    if (enchantmentLevel != 0) {
                        double chance = (100 / enchantmentLevel + 1);
                        double generatedChance = ThreadLocalRandom.current().nextDouble(0, 100);

                        if (chance > generatedChance)
                            tool.setDurability((short) (tool.getDurability() + 1));
                    } else
                        tool.setDurability((short) (tool.getDurability() + 1));

                    if (tool.getDurability() == item.getMaterial().getMaxDurability())
                        player.setItemInHand(null);

                } else
                    tool.setDurability((short) 0);

                player.updateInventory();
            }
        }

        List<ItemStack> drop = new ArrayList<>();
        multiBlockBreakEvent.getBlockData().forEach((location, data) -> {
            if (data.getValue().isEmpty()) return;

            drop.addAll(data.getValue());
            data.getValue().clear();
        });

        drop.forEach(item -> {
            Location location = locations[0].clone().add(0.5, 0.5, 0.5);
            location.getWorld().dropItem(location, item);
        });

        if (multiBlockBreakEvent.getExperience() != 0) {
            ((ExperienceOrb) locations[0].getWorld().spawnEntity(locations[0], EntityType.EXPERIENCE_ORB)).setExperience(multiBlockBreakEvent.getExperience());
            multiBlockBreakEvent.setExperience(0);
        }

        return multiBlockBreakEvent;
    }

    private long getItemCountWithFortune(Material material, int enchant_level) {
        long drops = ThreadLocalRandom.current().nextLong(enchant_level + 2) - 1;
        if (drops < 0)
            drops = 0;

        long i = material == Material.LAPIS_BLOCK ? 4 + ThreadLocalRandom.current().nextLong(5) : 1;
        return i * (drops + 1);
    }

    private List<ItemStack> getItems(ItemStack itemStack, long amount) {
        List<ItemStack> itemStacks = new ArrayList<>();
        itemStack = itemStack.clone();
        while (amount != 0) {
            if (amount > itemStack.getMaxStackSize()) {
                itemStack.setAmount(itemStack.getMaxStackSize());
                amount -= itemStack.getMaxStackSize();

            } else {
                itemStack.setAmount((int) amount);
                amount = 0;
            }

            itemStacks.add(itemStack);
            itemStack = itemStack.clone();
        }

        return itemStacks;
    }

}
