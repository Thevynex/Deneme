package dev.vynex.thermal;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

/** Package targets are exact argument values; operations are limited to installed third-party apps. */
public final class AppManagementPolicy {
    private static final Set<String> PROTECTED=Set.of("dev.vynex.thermal","moe.shizuku.privileged.api");
    public static String validatePackage(String pkg)throws IOException {
        if(pkg==null || pkg.length()>255 || !pkg.matches("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)+") || PROTECTED.contains(pkg))
            throw new IOException("Bu uygulama hedef olarak seçilemez");
        return pkg;
    }
    public static void validateUser(int userId,int callingUid)throws IOException {
        // Android allocates 100000 Linux UIDs per Android user/profile.
        if(callingUid<0 || userId<0 || userId!=callingUid/100000)throw new IOException("Yalnızca bu uygulamanın çalıştığı profil yönetilebilir");
    }
    public static Set<String> userPackages(String output) {
        Set<String> packages=new TreeSet<>();
        for(String line:output.split("\\R"))if(line.startsWith("package:")) {
            String pkg=line.substring(8).trim();
            try {packages.add(validatePackage(pkg));}catch(IOException ignored){}
        }
        return packages;
    }
    public static String[] listCommand(int userId){return new String[]{"/system/bin/pm","list","packages","-3","--user",String.valueOf(userId)};}
    public static String[] stopCommand(String pkg,int userId,Set<String> eligible)throws IOException {
        validatePackage(pkg);
        if(eligible==null || !eligible.contains(pkg))throw new IOException("Hedef bu profilde yüklü bir kullanıcı uygulaması değil");
        return new String[]{"/system/bin/am","force-stop","--user",String.valueOf(userId),pkg};
    }
    public static String[] stateCommand(String pkg)throws IOException {return new String[]{"/system/bin/dumpsys","package",validatePackage(pkg)};}
    public static Boolean stoppedForUser(String output,int userId) {
        Matcher section=Pattern.compile("(?m)^\\s*User\\s+(\\d+)\\s*:").matcher(output);
        while(section.find()) {
            if(!section.group(1).equals(String.valueOf(userId)))continue;
            int start=section.end(),end=output.length();
            if(section.find())end=section.start();
            Matcher state=Pattern.compile("(?:^|\\s)stopped=(true|false)(?=\\s|$)").matcher(output.substring(start,end));
            return state.find()?Boolean.valueOf(state.group(1)):null;
        }
        return null;
    }
}
