package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Replaces the vanilla main-hand attack-damage modifier for listed weapons.
 * Same table and UUID as kubejs/startup_scripts/dmzweaponscale.js.
 * Player base damage is 1, so a modifier of 89 is 90 attack damage.
 */
public final class WeaponBaseDamage {
    private static final UUID BASE_UUID =
            UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf");
    private static final AtomicInteger ERRORS_LEFT = new AtomicInteger(5);
    private static final Map<String, Double> BASE_DAMAGE;

    static {
        Map<String, Double> damage = new HashMap<>();
        damage.put("dragonminez:brave_sword", 17.75d);
        damage.put("dragonminez:dimensional_sword", 61.5d);
        damage.put("dragonminez:gete_axe", 2d);
        damage.put("dragonminez:gete_hoe", -0.25d);
        damage.put("dragonminez:gete_pickaxe", 1d);
        damage.put("dragonminez:gete_shovel", 1.125d);
        damage.put("dragonminez:power_pole", 15d);
        damage.put("dragonminez:yajirobe_katana", 2d);
        damage.put("dragonminez:z_sword", 74d);
        damage.put("simplymore:ascended_idol", 99d);
        damage.put("simplymore:black_pearl", 89d);
        damage.put("simplymore:blade_of_the_grotesque", 119d);
        damage.put("simplymore:boas_fang", 69d);
        damage.put("simplymore:brassturn", 79d);
        damage.put("simplymore:cindergorge", 109d);
        damage.put("simplymore:culterex", 79d);
        damage.put("simplymore:darksent", 99d);
        damage.put("simplymore:deaths_eyrie", 119d);
        damage.put("simplymore:earthshatter", 119d);
        damage.put("simplymore:exedrill", 89d);
        damage.put("simplymore:glimmerstep", 89d);
        damage.put("simplymore:grandfrost", 139d);
        damage.put("simplymore:great_slither", 109d);
        damage.put("simplymore:holylight", 99d);
        damage.put("simplymore:lustrous_moxie", 99d);
        damage.put("simplymore:matterbane", 89d);
        damage.put("simplymore:mimicry_backhand_blade", 79d);
        damage.put("simplymore:mimicry_chakram", 89d);
        damage.put("simplymore:mimicry_claymore", 119d);
        damage.put("simplymore:mimicry_cutlass", 99d);
        damage.put("simplymore:mimicry_dagger", 79d);
        damage.put("simplymore:mimicry_deer_horns", 89d);
        damage.put("simplymore:mimicry_glaive", 99d);
        damage.put("simplymore:mimicry_grandsword", 159d);
        damage.put("simplymore:mimicry_great_katana", 109d);
        damage.put("simplymore:mimicry_great_spear", 129d);
        damage.put("simplymore:mimicry_greataxe", 129d);
        damage.put("simplymore:mimicry_greathammer", 139d);
        damage.put("simplymore:mimicry_halberd", 129d);
        damage.put("simplymore:mimicry_katana", 99d);
        damage.put("simplymore:mimicry_khopesh", 89d);
        damage.put("simplymore:mimicry_lance", 99d);
        damage.put("simplymore:mimicry_longsword", 99d);
        damage.put("simplymore:mimicry_pernach", 109d);
        damage.put("simplymore:mimicry_quarterstaff", 79d);
        damage.put("simplymore:mimicry_rapier", 89d);
        damage.put("simplymore:mimicry_sai", 69d);
        damage.put("simplymore:mimicry_scythe", 109d);
        damage.put("simplymore:mimicry_spear", 99d);
        damage.put("simplymore:mimicry_twinblade", 99d);
        damage.put("simplymore:mimicry_warglaive", 99d);
        damage.put("simplymore:molten_flare", 129d);
        damage.put("simplymore:myrmedge", 79d);
        damage.put("simplymore:perforiscus", 99d);
        damage.put("simplymore:revvengine", 79d);
        damage.put("simplymore:runic_backhand_blade", 69d);
        damage.put("simplymore:runic_dagger", 69d);
        damage.put("simplymore:runic_deer_horns", 79d);
        damage.put("simplymore:runic_grandsword", 149d);
        damage.put("simplymore:runic_great_katana", 99d);
        damage.put("simplymore:runic_great_spear", 119d);
        damage.put("simplymore:runic_khopesh", 79d);
        damage.put("simplymore:runic_lance", 89d);
        damage.put("simplymore:runic_pernach", 99d);
        damage.put("simplymore:runic_quarterstaff", 69d);
        damage.put("simplymore:ruptured_idol", 99d);
        damage.put("simplymore:ruyi_jingu_bang", 89d);
        damage.put("simplymore:serpentine_valour", 109d);
        damage.put("simplymore:smouldering_ruin", 69d);
        damage.put("simplymore:soul_foreseer", 89d);
        damage.put("simplymore:stasis", 79d);
        damage.put("simplymore:tarnished_idol", 99d);
        damage.put("simplymore:the_blood_harvester", 79d);
        damage.put("simplymore:the_vessel_breach", 59d);
        damage.put("simplymore:tidebreaker", 89d);
        damage.put("simplymore:timekeeper", 79d);
        damage.put("simplymore:vipers_call", 59d);
        damage.put("simplyswords:arcanethyst", 129d);
        damage.put("simplyswords:awakened_lichblade", 129d);
        damage.put("simplyswords:bramblethorn", 89d);
        damage.put("simplyswords:brimstone_claymore", 119d);
        damage.put("simplyswords:caelestis", 119d);
        damage.put("simplyswords:decaying_relic", 99d);
        damage.put("simplyswords:dormant_relic", 89d);
        damage.put("simplyswords:dreadtide", 89d);
        damage.put("simplyswords:emberblade", 89d);
        damage.put("simplyswords:emberlash", 59d);
        damage.put("simplyswords:enigma", 129d);
        damage.put("simplyswords:flamewind", 89d);
        damage.put("simplyswords:frostfall", 109d);
        damage.put("simplyswords:harbinger", 89d);
        damage.put("simplyswords:hearthflame", 139d);
        damage.put("simplyswords:hiveheart", 129d);
        damage.put("simplyswords:icewhisper", 129d);
        damage.put("simplyswords:livyatan", 99d);
        damage.put("simplyswords:magiblade", 89d);
        damage.put("simplyswords:magiscythe", 99d);
        damage.put("simplyswords:magispear", 99d);
        damage.put("simplyswords:mjolnir", 89d);
        damage.put("simplyswords:molten_edge", 99d);
        damage.put("simplyswords:ribboncleaver", 129d);
        damage.put("simplyswords:righteous_relic", 89d);
        damage.put("simplyswords:runic_chakram", 79d);
        damage.put("simplyswords:runic_claymore", 109d);
        damage.put("simplyswords:runic_cutlass", 89d);
        damage.put("simplyswords:runic_glaive", 89d);
        damage.put("simplyswords:runic_greataxe", 119d);
        damage.put("simplyswords:runic_greathammer", 129d);
        damage.put("simplyswords:runic_halberd", 119d);
        damage.put("simplyswords:runic_katana", 89d);
        damage.put("simplyswords:runic_longsword", 89d);
        damage.put("simplyswords:runic_rapier", 79d);
        damage.put("simplyswords:runic_sai", 59d);
        damage.put("simplyswords:runic_scythe", 99d);
        damage.put("simplyswords:runic_spear", 89d);
        damage.put("simplyswords:runic_twinblade", 89d);
        damage.put("simplyswords:runic_warglaive", 89d);
        damage.put("simplyswords:shadowsting", 39d);
        damage.put("simplyswords:slumbering_lichblade", 129d);
        damage.put("simplyswords:soulkeeper", 139d);
        damage.put("simplyswords:soulpyre", 129d);
        damage.put("simplyswords:soulrender", 99d);
        damage.put("simplyswords:soulstealer", 59d);
        damage.put("simplyswords:stars_edge", 89d);
        damage.put("simplyswords:stormbringer", 89d);
        damage.put("simplyswords:storms_edge", 89d);
        damage.put("simplyswords:sunfire", 89d);
        damage.put("simplyswords:tainted_relic", 89d);
        damage.put("simplyswords:tempest", 59d);
        damage.put("simplyswords:thunderbrand", 129d);
        damage.put("simplyswords:toxic_longsword", 89d);
        damage.put("simplyswords:twisted_blade", 99d);
        damage.put("simplyswords:waking_lichblade", 129d);
        damage.put("simplyswords:watcher_claymore", 119d);
        damage.put("simplyswords:watching_warglaive", 89d);
        damage.put("simplyswords:waxweaver", 119d);
        damage.put("simplyswords:whisperwind", 89d);
        damage.put("simplyswords:wickpiercer", 99d);
        BASE_DAMAGE = Collections.unmodifiableMap(damage);
    }

    private WeaponBaseDamage() {}

    public static void onAttributes(ItemAttributeModifierEvent event) {
        try {
            if (event.getSlotType() != EquipmentSlot.MAINHAND) {
                return;
            }
            ItemStack stack = event.getItemStack();
            if (stack == null || stack.m_41619_()) {
                return;
            }
            var key = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            if (key == null) {
                return;
            }
            Double damage = BASE_DAMAGE.get(key.toString());
            if (damage == null) {
                return;
            }
            var current = event.getModifiers().get(Attributes.f_22281_);
            if (current != null) {
                AttributeModifier base = null;
                for (AttributeModifier modifier : current) {
                    if (modifier != null && BASE_UUID.equals(modifier.m_22209_())) {
                        base = modifier;
                        break;
                    }
                }
                if (base != null) {
                    event.removeModifier(Attributes.f_22281_, base);
                }
            }
            event.addModifier(
                    Attributes.f_22281_,
                    new AttributeModifier(
                            BASE_UUID,
                            "DMZ Legacy base weapon damage",
                            damage,
                            AttributeModifier.Operation.ADDITION));
        } catch (Throwable t) {
            if (ERRORS_LEFT.getAndDecrement() > 0) {
                HotLog.debug("[DMZ Weapon Damage ERROR] " + t);
            }
        }
    }
}
