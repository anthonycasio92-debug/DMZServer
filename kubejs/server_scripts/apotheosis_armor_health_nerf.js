/*
 * DBZ Legacy Reborn
 * Apotheosis Armor Health Nerf
 *
 * Nerfs the Blessed armor affix to 20% of its original values.
 *
 * Original approximate ranges:
 * Common:  2–3 Health
 * Uncommon: 2–3 Health
 * Rare:    3–5 Health
 * Epic:    3–7 Health
 * Mythic:  3–7 Health
 * Ancient: 4–11 Health
 *
 * New approximate ranges:
 * Common:   0.4–0.6 Health
 * Uncommon: 0.4–0.6 Health
 * Rare:     0.6–1.0 Health
 * Epic:     0.6–1.4 Health
 * Mythic:   0.6–1.4 Health
 * Ancient:  0.8–2.2 Health
 */

ServerEvents.highPriorityData(function (event) {

    event.addJson(
        "apotheosis:affixes/armor/attribute/blessed",
        {
            type: "apotheosis:attribute",

            attribute: "minecraft:generic.max_health",

            operation: "ADDITION",

            values: {
                common: {
                    min: 0.4,
                    steps: 2,
                    step: 0.2
                },

                uncommon: {
                    min: 0.4,
                    steps: 2,
                    step: 0.2
                },

                rare: {
                    min: 0.6,
                    steps: 3,
                    step: 0.2
                },

                epic: {
                    min: 0.6,
                    steps: 5,
                    step: 0.2
                },

                mythic: {
                    min: 0.6,
                    steps: 5,
                    step: 0.2
                },

                ancient: {
                    min: 0.8,
                    steps: 8,
                    step: 0.2
                }
            },

            types: [
                "helmet",
                "chestplate",
                "leggings",
                "boots"
            ]
        }
    );

    console.log(
        "[DBZ Legacy Reborn] Blessed armor health affix reduced to 20%."
    );
});