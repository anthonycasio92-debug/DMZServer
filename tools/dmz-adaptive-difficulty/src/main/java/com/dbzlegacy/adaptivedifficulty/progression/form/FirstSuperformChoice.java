package com.dbzlegacy.adaptivedifficulty.progression.form;

import java.util.Locale;

/**
 * Picks the one form a mastery grant writes.
 *
 * <p>The first superform is the superform whose unlock level is 0. That is
 * Oozaru for Saiyans, and the same rule for every other race that has a
 * level 0 superform. A race with no level 0 superform uses its earliest
 * superform. The first legendary form is the earliest legendary form, and
 * it is a separate grant.
 */
public final class FirstSuperformChoice {
    private FirstSuperformChoice() {}

    public enum Kind {
        SUPER,
        LEGENDARY
    }

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

    public static Candidate select(java.util.List<Candidate> forms, Kind kind) {
        if (forms == null || forms.isEmpty() || kind == null) {
            return null;
        }
        if (kind == Kind.LEGENDARY) {
            return earliest(forms, false, false);
        }
        if (kind != Kind.SUPER) {
            return null;
        }
        Candidate levelZero = earliest(forms, true, true);
        if (levelZero != null) {
            return levelZero;
        }
        return earliest(forms, true, false);
    }

    public static boolean isSuperform(Candidate candidate) {
        return candidate != null && contains(candidate.formType, "superform");
    }

    public static boolean isLegendary(Candidate candidate) {
        return candidate != null && contains(candidate.formType, "legendaryform");
    }

    /**
     * @param levelZeroOnly when selecting superforms, keep unlock level 0 only
     */
    private static Candidate earliest(java.util.List<Candidate> forms, boolean superform, boolean levelZeroOnly) {
        Candidate best = null;
        for (Candidate candidate : forms) {
            if (candidate == null || candidate.stack || candidate.form.isBlank() || candidate.group.isBlank()) {
                continue;
            }
            if (superform) {
                if (!isSuperform(candidate)) {
                    continue;
                }
                if (levelZeroOnly && candidate.unlockLevel != 0) {
                    continue;
                }
            } else if (!isLegendary(candidate)) {
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
