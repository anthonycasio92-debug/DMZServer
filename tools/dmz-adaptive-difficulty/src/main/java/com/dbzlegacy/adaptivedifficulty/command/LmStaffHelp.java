package com.dbzlegacy.adaptivedifficulty.command;

/** Consistent staff command help — one full command per line, shared section headers. */
public final class LmStaffHelp {
    private LmStaffHelp() {}

    public static String build(String title, String rootLabel, String intro, Section... sections) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l").append(title).append(" §8(").append(rootLabel).append(')');
        if (intro != null && !intro.isBlank()) {
            sb.append('\n').append(intro.trim());
        }
        for (Section section : sections) {
            if (section == null) {
                continue;
            }
            sb.append('\n').append('\n').append("§6— ").append(section.name).append(" —");
            for (String line : section.lines) {
                if (line != null && !line.isBlank()) {
                    sb.append('\n').append(line.trim());
                }
            }
        }
        return sb.toString().stripTrailing();
    }

    /** {@code §f/command … §8— §7description} */
    public static String cmd(String command, String description) {
        String text = description == null ? "" : description.trim();
        if (!text.isEmpty() && Character.isLowerCase(text.charAt(0))) {
            text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }
        return "§f" + command + " §8— §7" + text;
    }

    public static String note(String text) {
        return "§8" + text;
    }

    public record Section(String name, String... lines) {}
}
