package com.dbzlegacy.adaptivedifficulty.progression.form;

import java.util.List;
import java.util.Locale;

/**
 * Picks one form to receive a quest mastery grant.
 *
 * <p>Superform groups come first. Oozaru is a tail form that shares the
 * superform type, so it is skipped. A race with no superform uses its first
 * legendary form instead (Frost Demon golden, and the same shape as Legendary
 * Super Saiyan). A mutant whose legendary group is configured uses that
 * group's earliest form, so Legendary Super Saiyan is the grant target
 * instead of the regular first superform.
 */
public final class FirstSuperformChoice {
    private FirstSuperformChoice() {}

    /** One configured form. {@code stack} marks a stack-form fallback. */
    public static final class Candidate {
        public final String group;
        public final String form;
        public final String formType;
        public final int unlockLevel;
        public final boolean stack;

        public Candidate(String group, String form, String formType, int unlockLevel, boolean stack) {
            this.group = group == null ? "" : group;
            this.form = form == null ? "" : form;
            this.formType = formType == null ? "" : formType;
            this.unlockLevel = unlockLevel;
            this.stack = stack;
        }

        public String label() {
            return group + "." + form;
        }
    }

    /**
     * @param legendaryGroup mutant legendary group name, usually {@code legendaryforms}
     */
    public static Candidate select(List<Candidate> forms, boolean mutant, String legendaryGroup) {
        if (forms == null || forms.isEmpty()) {
            return null;
        }
        if (mutant) {
            Candidate legendary = earliest(forms, legendaryGroup, false, false, true);
            if (legendary != null) {
                return legendary;
            }
        }
        Candidate superform = earliest(forms, null, true, true, false);
        if (superform != null) {
            return superform;
        }
        Candidate legendary = earliest(forms, null, false, false, true);
        if (legendary != null) {
            return legendary;
        }
        return earliest(forms, null, false, false, false);
    }

    public static boolean isSuperform(Candidate candidate) {
        return candidate != null && contains(candidate.formType, "superform");
    }

    public static boolean isLegendary(Candidate candidate) {
        return candidate != null && contains(candidate.formType, "legendaryform");
    }

    /** Tail form. Its group type is often {@code superforms}, which is not the superform ladder. */
    public static boolean isOozaru(Candidate candidate) {
        if (candidate == null) {
            return false;
        }
        return contains(candidate.group, "oozaru") || contains(candidate.form, "oozaru");
    }

    public static boolean isQuestForm(Candidate candidate) {
        return candidate != null && !candidate.stack && !isOozaru(candidate)
                && (isSuperform(candidate) || isLegendary(candidate));
    }

    private static Candidate earliest(
            List<Candidate> forms,
            String groupName,
            boolean skipOozaru,
            boolean superOnly,
            boolean legendaryOnly) {
        Candidate best = null;
        for (Candidate candidate : forms) {
            if (candidate == null || candidate.form.isBlank() || candidate.group.isBlank()) {
                continue;
            }
            if (candidate.stack) {
                if (superOnly || legendaryOnly || groupName != null) {
                    continue;
                }
            } else if (!superOnly && !legendaryOnly && groupName == null) {
                continue;
            }
            if (skipOozaru && isOozaru(candidate)) {
                continue;
            }
            if (superOnly && (!isSuperform(candidate) || candidate.stack)) {
                continue;
            }
            if (legendaryOnly && (!isLegendary(candidate) || candidate.stack)) {
                continue;
            }
            if (groupName != null && !groupName.isBlank()
                    && !candidate.group.equalsIgnoreCase(groupName)) {
                continue;
            }
            if (better(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean better(Candidate candidate, Candidate best) {
        if (best == null) {
            return true;
        }
        if (candidate.unlockLevel != best.unlockLevel) {
            return candidate.unlockLevel < best.unlockLevel;
        }
        int group = candidate.group.compareToIgnoreCase(best.group);
        if (group != 0) {
            return group < 0;
        }
        return candidate.form.compareToIgnoreCase(best.form) < 0;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
