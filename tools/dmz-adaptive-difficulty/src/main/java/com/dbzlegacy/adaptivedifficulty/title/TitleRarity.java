package com.dbzlegacy.adaptivedifficulty.title;

/** Visual rarity for the Titles GUI. */
public enum TitleRarity {
    COMMON("Common", "a"),
    RARE("Rare", "9"),
    EPIC("Epic", "5"),
    LEGENDARY("Legendary", "6"),
    MYTHIC("Mythic", "d");

    public final String display;
    /** Minecraft color code without §. */
    public final String color;

    TitleRarity(String display, String color) {
        this.display = display;
        this.color = color;
    }

    public String coloredLabel() {
        return "§" + color + display;
    }
}
