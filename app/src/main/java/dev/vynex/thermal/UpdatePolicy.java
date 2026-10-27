package dev.vynex.thermal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class UpdatePolicy {
    public static void checkHash(byte[] apk,String expected) throws Exception {
        if(expected==null || !expected.matches("[0-9a-f]{64}"))throw new IOException("Güncelleme özeti geçersiz");
        String actual=hash(apk);
        if(!MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII),expected.getBytes(StandardCharsets.US_ASCII)))
            throw new IOException("APK özeti uyuşmuyor; dosyalar güncelleniyor olabilir. Tekrar dene.");
    }
    public static String hash(byte[] data) throws Exception {
        StringBuilder hash=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(data))hash.append(String.format(Locale.ROOT,"%02x",b&255));
        return hash.toString();
    }
    public static void verifyArchive(String installedPackage,long currentVersion,Set<String> installedSigners,
            String candidatePackage,long candidateVersion,Set<String> candidateSigners,long expectedVersion) throws IOException {
        if(!installedPackage.equals(candidatePackage) || candidateVersion!=expectedVersion || candidateVersion<=currentVersion || candidateVersion>Integer.MAX_VALUE)
            throw new IOException("APK paket adı veya sürümü beklenen değerle uyuşmuyor");
        if(installedSigners==null || installedSigners.isEmpty() || !installedSigners.equals(candidateSigners))
            throw new IOException("APK imzası kurulu sürümle uyuşmuyor. Farklı anahtarlı APK ile otomatik yükseltme yapılmadı.");
    }
}
