package com.eiu.capstone.backend.utility;

/**
 * Aligned timing console output when {@code app.grading.timing-log=true}.
 */
public final class TimingLog {

    private TimingLog() {}

    public static void line(boolean enabled, String title, long ms) {
        if (!enabled) {
            return;
        }
        System.out.printf("%n  [timing] %-24s %6d ms%n", title, ms);
    }

    public static void block(boolean enabled, String title, Object... nameThenMs) {
        if (!enabled || nameThenMs == null || nameThenMs.length < 2) {
            return;
        }
        int width = 12;
        for (int i = 0; i + 1 < nameThenMs.length; i += 2) {
            width = Math.max(width, String.valueOf(nameThenMs[i]).length());
        }
        String divider = "-".repeat(width + 10);
        StringBuilder out = new StringBuilder(160);
        out.append('\n');
        out.append("  [timing] ").append(title).append('\n');
        for (int i = 0; i + 1 < nameThenMs.length; i += 2) {
            String name = String.valueOf(nameThenMs[i]);
            long ms = ((Number) nameThenMs[i + 1]).longValue();
            if ("total".equalsIgnoreCase(name)) {
                out.append("           ").append(divider).append('\n');
            }
            out.append(String.format("           %-" + width + "s %6d ms%n", name, ms));
        }
        System.out.print(out);
    }
}
