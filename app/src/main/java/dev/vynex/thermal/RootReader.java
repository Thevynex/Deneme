package dev.vynex.thermal;

import android.system.Os;
import android.system.OsConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

final class RootReader {
    // Fixed read-only command. No user input, killing apps, or system changes.
    private static final String COMMAND = "id -u; cat /proc/stat; "
        + "cat /proc/[0-9]*/stat 2>/dev/null; echo __THERMAL__; "
        + "for d in /sys/class/thermal/thermal_zone*; do "
        + "[ -r \"$d/type\" ] && [ -r \"$d/temp\" ] && "
        + "printf '%s|%s\\n' \"$(cat \"$d/type\")\" \"$(cat \"$d/temp\")\"; done; true";
    static ProcSnapshot read(boolean first) throws Exception {
        java.lang.Process process = new ProcessBuilder("su", "-c", COMMAND).redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream input = process.getInputStream()) {
                byte[] bytes = new byte[4096]; int n;
                while ((n = input.read(bytes)) != -1) {
                    if (output.size() + n > 2 * 1024 * 1024) { process.destroyForcibly(); break; }
                    output.write(bytes, 0, n);
                }
            } catch (IOException ignored) {}
        }, "root-output");
        reader.setDaemon(true); reader.start();
        try {
            if (!process.waitFor(first ? 35 : 8, TimeUnit.SECONDS)) throw new IOException("Root yanıt süresi aşıldı");
            reader.join(1500);
            if (reader.isAlive()) throw new IOException("Root çıktısı tamamlanmadı");
            String text = output.toString(StandardCharsets.UTF_8.name());
            if (process.exitValue() != 0 || !(text.startsWith("0\n") || text.startsWith("0\r\n")))
                throw new IOException("Root erişimi verilmedi");
            long pageSize = Os.sysconf(OsConstants._SC_PAGESIZE);
            if (pageSize <= 0) throw new IOException("Bellek sayfa boyutu okunamadı");
            ProcSnapshot snapshot = ProcSnapshot.parse(text + "\nPAGE|" + pageSize);
            if (snapshot.total < 0 || snapshot.entries.isEmpty()) throw new IOException("Süreç sayaçları okunamadı");
            return snapshot;
        } finally { process.destroyForcibly(); }
    }
}
