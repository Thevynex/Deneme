package dev.vynex.thermal;

import java.io.*;
import java.util.*;

/** A fixed read-only diagnostic plan. It cannot set properties or override feature flags. */
public final class DesktopDiagnostics {
    public static final String PROPERTY="persist.wm.debug.desktop_experience_devopts";
    public static Map<String,String[]> commands() {
        Map<String,String[]> commands=new LinkedHashMap<>();
        commands.put("global."+DesktopSetting.KEY,DesktopSetting.readCommand());
        commands.put(PROPERTY,new String[]{"/system/bin/getprop",PROPERTY});
        commands.put("global.override_desktop_mode_features",new String[]{"/system/bin/settings","get","global","override_desktop_mode_features"});
        commands.put("global.force_desktop_mode_on_external_displays",new String[]{"/system/bin/settings","get","global","force_desktop_mode_on_external_displays"});
        commands.put("ro.build.type",new String[]{"/system/bin/getprop","ro.build.type"});
        commands.put("ro.debuggable",new String[]{"/system/bin/getprop","ro.debuggable"});
        commands.put("aconfig masaüstü bayrakları",new String[]{"/system/bin/aflags","list"});
        return commands;
    }
    public static String desktopFlags(Reader input) throws IOException {
        StringBuilder result=new StringBuilder(),line=new StringBuilder();
        int read=0,c;boolean oversized=false,cut=false;
        while((c=input.read())!=-1) {
            if(++read>2_000_000){cut=true;break;}
            if(c=='\n') {
                if(!oversized && relevant(line.toString()))result.append(line).append('\n');
                line.setLength(0);oversized=false;
                if(result.length()>12000){cut=true;break;}
            } else if(line.length()<4096)line.append((char)c);else oversized=true;
        }
        if(!oversized && line.length()>0 && relevant(line.toString()) && result.length()<12000)result.append(line).append('\n');
        if(result.length()==0)result.append("Okunabilir masaüstü bayrağı bulunamadı; destek/izin yokluğu da neden olabilir.\n");
        if(cut)result.append("[Tanılama çıktı sınırında kesildi]\n");
        return result.toString();
    }
    private static boolean relevant(String line) {
        String s=line.toLowerCase(Locale.ROOT);
        return s.contains("desktop_window") || s.contains("desktop_experience");
    }
}
