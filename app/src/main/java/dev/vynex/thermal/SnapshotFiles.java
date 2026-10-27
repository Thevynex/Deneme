package dev.vynex.thermal;

import java.io.*;
import java.util.Arrays;

/** Fixed read-only paths. Bounded output keeps the Binder reply below its transaction limit. */
public final class SnapshotFiles {
    private static final int LIMIT = 256_000;
    public static String read(File proc, File thermal, long pageSize) throws IOException {
        if(pageSize <= 0) throw new IOException("Invalid page size");
        StringBuilder out = new StringBuilder("PAGE|" + pageSize + "\n");
        String cpu = first(new File(proc,"stat"));
        if(cpu != null && cpu.startsWith("cpu ")) out.append(cpu).append('\n');
        File[] dirs = proc.listFiles(file -> file.getName().matches("[0-9]+"));
        if(dirs != null) {
            Arrays.sort(dirs, (a,b) -> a.getName().compareTo(b.getName()));
            for(File dir:dirs) {
                String stat = first(new File(dir,"stat"));
                if(stat == null) continue;
                if(out.length() + stat.length() + 64 > LIMIT) { out.append("LIMIT|1\n"); break; }
                out.append(stat).append('\n');
            }
        }
        out.append("__THERMAL__\n");
        File[] zones = thermal.listFiles(file -> file.getName().startsWith("thermal_zone"));
        if(zones != null) for(File zone:zones) {
            String name=first(new File(zone,"type")), value=first(new File(zone,"temp"));
            if(name == null || value == null) continue;
            if(out.length()+name.length()+value.length()+4 > LIMIT) {out.append("LIMIT|1\n");break;}
            out.append(name).append('|').append(value).append('\n');
        }
        return out.toString();
    }
    private static String first(File file) {
        try (Reader r=new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
            StringBuilder line=new StringBuilder();
            for(int i=0;i<8192;i++) {int c=r.read();if(c<0 || c=='\n') return line.toString();line.append((char)c);}
            return null; // An oversized record is not a valid kernel counter line.
        } catch(IOException|SecurityException ignored) {return null;}
    }
}
