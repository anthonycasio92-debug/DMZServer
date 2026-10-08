package com.dbzlegacy.adaptivedifficulty.noea;

/**
 * Prints a wipe failure on stdout. The server log keeps the stack next to the
 * other {@code [LM]} lines. {@code printStackTrace} still goes to stderr.
 *
 * <p>This class stays outside the mixin package. The dedicated-server
 * classloader does not resolve ordinary classes that live next to mixins.
 */
public final class AbsorptionClearLog {
    private AbsorptionClearLog() {}

    public static void failure(Throwable thrown) {
        System.out.println("[LM] clear() threw: " + thrown);
        Throwable cause = thrown;
        int depth = 0;
        while (cause != null && depth < 6) {
            System.out.println("[LM] clear() stack: " + cause);
            StackTraceElement[] frames = cause.getStackTrace();
            int limit = frames == null ? 0 : Math.min(frames.length, 24);
            for (int i = 0; i < limit; i++) {
                System.out.println("[LM]   at " + frames[i]);
            }
            Throwable next = cause.getCause();
            if (next == cause) {
                break;
            }
            cause = next;
            depth++;
        }
        if (thrown != null) {
            thrown.printStackTrace();
        }
    }
}
