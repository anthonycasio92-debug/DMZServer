// ============================================================================
// DMZ Dino Meat Food Balance
// Minecraft 1.20.1 Forge / KubeJS 2001.6.x
//
// FILE:
// kubejs/startup_scripts/dmz_food_balance.js
//
// Requires a FULL server restart (startup script).
// ============================================================================

/**
 * Apply food stats via setFoodProperties (maps to kjs$setFoodProperties).
 * Assigning `item.foodProperties = function (...)` is flaky on Rhino 1.20.1
 * and can silently clear food instead of applying the builder.
 */
function applyFood(item, hunger, saturation) {
    item.setFoodProperties(function (food) {
        food.hunger(hunger);
        food.saturation(saturation);
        food.meat(true);
        food.alwaysEdible(true);
    });
}

ItemEvents.modification(function (event) {
    // RAW BABY DINO MEAT
    event.modify("dragonminez:raw_baby_dino_meat", function (item) {
        applyFood(item, 2, 0.2);
    });

    // RAW DINO MEAT
    event.modify("dragonminez:raw_dino_meat", function (item) {
        applyFood(item, 3, 0.3);
    });

    // RAW DINO TAIL
    event.modify("dragonminez:dino_tail_raw", function (item) {
        applyFood(item, 4, 0.3);
    });

    // COOKED BABY DINO MEAT
    event.modify("dragonminez:cooked_baby_dino_meat", function (item) {
        applyFood(item, 5, 0.5);
    });

    // COOKED DINO MEAT
    event.modify("dragonminez:cooked_dino_meat", function (item) {
        applyFood(item, 6, 0.6);
    });

    // COOKED DINO TAIL
    event.modify("dragonminez:dino_tail_cooked", function (item) {
        applyFood(item, 7, 0.7);
    });

    console.info("[DMZ Food Balance] Dino meat hunger/saturation overrides registered.");
});
