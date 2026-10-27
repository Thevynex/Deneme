package dev.vynex.thermal;

import java.util.*;

/** Linux counters: CPU is share of all cores, RSS is resident memory (not PSS). */
public final class ProcSnapshot {
    public record Entry(int pid, long start, String name, long ticks, long rssBytes) {}
    public record Row(Entry entry, Double cpu) {}
    public final long total, idle;
    public final Map<Integer, Entry> entries;
    public final List<String> temperatures;
    private ProcSnapshot(long total, long idle, Map<Integer, Entry> entries, List<String> temperatures) {
        this.total = total; this.idle = idle; this.entries = entries; this.temperatures = temperatures;
    }
    public static ProcSnapshot parse(String text) {
        long total = -1, idle = 0, page = 4096;
        Map<Integer, Entry> entries = new HashMap<>();
        List<String> temperatures = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (String line : lines) if (line.startsWith("PAGE|")) {
            try { long n = Long.parseLong(line.substring(5).trim()); if (n > 0) page = n; } catch (NumberFormatException ignored) {}
        }
        boolean thermal = false;
        for (String line : lines) {
            if (line.equals("__THERMAL__")) { thermal = true; continue; }
            if (line.startsWith("PAGE|")) continue;
            try {
                if (line.startsWith("cpu ")) {
                    String[] f = line.trim().split("\\s+");
                    if (f.length < 5) continue;
                    long parsedTotal = 0;
                    // guest and guest_nice are already included in user/nice.
                    for (int i = 1; i < Math.min(f.length, 9); i++) parsedTotal += Long.parseLong(f[i]);
                    long parsedIdle = Long.parseLong(f[4]) + (f.length > 5 ? Long.parseLong(f[5]) : 0);
                    if (parsedTotal >= 0 && parsedIdle >= 0) { total = parsedTotal; idle = parsedIdle; }
                } else if (thermal) {
                    String[] f = line.split("\\|", 2);
                    if (f.length == 2) {
                        double c = Double.parseDouble(f[1]);
                        // Linux thermal-zone ABI specifies millidegrees Celsius.
                        c /= 1000.0;
                        if (Double.isFinite(c) && c > -50 && c < 200)
                            temperatures.add(f[0] + ": " + String.format(Locale.ROOT, "%.1f °C", c));
                    }
                } else {
                    int open = line.indexOf('('), close = line.lastIndexOf(')');
                    if (open < 1 || close <= open) continue;
                    int pid = Integer.parseInt(line.substring(0, open).trim());
                    String[] f = line.substring(close + 1).trim().split("\\s+");
                    if (f.length < 22) continue;
                    long ticks = Long.parseLong(f[11]) + Long.parseLong(f[12]);
                    long start = Long.parseLong(f[19]);
                    long rss = Math.max(0, Long.parseLong(f[21])) * page;
                    entries.put(pid, new Entry(pid, start, line.substring(open + 1, close), ticks, rss));
                }
            } catch (NumberFormatException ignored) { /* exited or inaccessible process */ }
        }
        return new ProcSnapshot(total, idle, entries, temperatures);
    }
    public Double cpuSince(ProcSnapshot before) {
        if (before == null || total <= before.total || before.total < 0) return null;
        long delta = total - before.total, idleDelta = idle - before.idle;
        if (idleDelta < 0) return null;
        return Math.max(0, Math.min(100, 100.0 * (delta - idleDelta) / delta));
    }
    public List<Row> rowsSince(ProcSnapshot before) {
        List<Row> rows = new ArrayList<>();
        for (Entry entry : entries.values()) {
            Entry old = before == null ? null : before.entries.get(entry.pid());
            Double cpu = null;
            if (old != null && old.start() == entry.start() && before.total >= 0 && total > before.total && entry.ticks() >= old.ticks())
                cpu = Math.max(0, Math.min(100, 100.0 * (entry.ticks() - old.ticks()) / (total - before.total)));
            rows.add(new Row(entry, cpu));
        }
        return rows;
    }
}
