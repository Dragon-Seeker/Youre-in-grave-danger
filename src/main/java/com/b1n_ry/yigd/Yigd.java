package com.b1n_ry.yigd;

import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.compat.InvModCompat;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.enchantment.DeathSightEnchantment;
import com.b1n_ry.yigd.enchantment.SoulboundEnchantment;
import com.b1n_ry.yigd.events.ServerEventHandler;
import com.b1n_ry.yigd.events.YigdServerEventHandler;
import com.b1n_ry.yigd.item.DeathScrollItem;
import com.b1n_ry.yigd.item.GraveKeyItem;
import com.b1n_ry.yigd.util.YigdCommands;
import com.b1n_ry.yigd.packets.ServerPacketHandler;
import com.b1n_ry.yigd.util.YigdResourceHandler;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Yigd implements ModInitializer {
    public static final String MOD_ID = "yigd";

    public static Logger LOGGER = LoggerFactory.getLogger("YIGD");

    public static GraveBlock GRAVE_BLOCK = new GraveBlock(FabricBlockSettings.create().strength(0.8f, 3600000.0f).nonOpaque());
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;


    // Optional registries
    public static DeathScrollItem DEATH_SCROLL_ITEM = new DeathScrollItem(new FabricItemSettings());
    public static GraveKeyItem GRAVE_KEY_ITEM = new GraveKeyItem(new FabricItemSettings());
    public static SoulboundEnchantment SOULBOUND_ENCHANTMENT;
    public static DeathSightEnchantment DEATH_SIGHT_ENCHANTMENT;

    /**
     * Any runnable added to this list will be executed on the end of the current server tick.
     * Use if runnable is required to run before some other event that would have otherwise ran before.
     */
    public static List<Runnable> END_OF_TICK = new ArrayList<>();

    public static Map<UUID, List<String>> NOT_NOTIFIED_ROBBERIES = new HashMap<>();
    public static Map<UUID, ClaimPriority> CLAIM_PRIORITIES = new HashMap<>();
    public static Map<UUID, ClaimPriority> ROB_PRIORITIES = new HashMap<>();

    @Override
    public void onInitialize() {
        AutoConfig.register(YigdConfig.class, GsonConfigSerializer::new);

        GRAVE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "grave_block_entity"), FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GRAVE_BLOCK).build());

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "grave"), GRAVE_BLOCK);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "grave"), new BlockItem(GRAVE_BLOCK, new FabricItemSettings()));

        YigdConfig config = YigdConfig.getConfig();
        if (config.extraFeatures.soulboundEnchant.enabled) {
            SOULBOUND_ENCHANTMENT = new SoulboundEnchantment(Enchantment.Rarity.VERY_RARE);
            Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "soulbound"), SOULBOUND_ENCHANTMENT);
        }
        if (config.extraFeatures.deathSightEnchant.enabled) {
            DEATH_SIGHT_ENCHANTMENT = new DeathSightEnchantment(Enchantment.Rarity.RARE, EquipmentSlot.HEAD);
            Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "death_sight"), DEATH_SIGHT_ENCHANTMENT);
        }
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "death_scroll"), DEATH_SCROLL_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "grave_key"), GRAVE_KEY_ITEM);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(GRAVE_BLOCK.asItem());

            entries.add(DEATH_SCROLL_ITEM.getDefaultStack());
            entries.add(GRAVE_KEY_ITEM.getDefaultStack());
        });

        // Makes sure proper mod compatibilities are loaded (on world load to check mods' config)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> InvModCompat.initModCompat());

        YigdServerEventHandler.registerEventCallbacks();
        ServerEventHandler.registerEvents();
        ServerPacketHandler.registerReceivers();
        YigdResourceHandler.init();

        YigdCommands.register();
    }
}
