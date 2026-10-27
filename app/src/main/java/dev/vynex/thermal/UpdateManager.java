package dev.vynex.thermal;

import android.content.Context;
import android.content.pm.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** User-triggered HTTPS updates; every APK must match both its hash and the installed signer. */
final class UpdateManager {
    private static final String BASE="https://raw.githubusercontent.com/Thevynex/Deneme/main/downloads/";
    static final String FILE="verified-update.apk";
    static File download(Context context) throws Exception {
        JSONObject manifest=new JSONObject(new String(fetch(BASE+"update.json",16384),StandardCharsets.UTF_8));
        long expectedVersion=manifest.getLong("versionCode");
        String expectedHash=manifest.getString("sha256").toLowerCase(Locale.ROOT);
        if(!expectedHash.matches("[0-9a-f]{64}"))throw new IOException("Güncelleme özeti geçersiz");
        PackageManager pm=context.getPackageManager();
        PackageInfo installed=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES);
        if(expectedVersion<=installed.versionCode)return null;
        byte[] apk=fetch(BASE+"Isi-Dedektifi.apk",40*1024*1024);
        UpdatePolicy.checkHash(apk,expectedHash);
        File output=new File(context.getCacheDir(),FILE);
        if(output.exists() && !output.delete())throw new IOException("Eski güncelleme dosyası temizlenemedi");
        try {
            try(FileOutputStream stream=new FileOutputStream(output)){stream.write(apk);}
            PackageInfo candidate=pm.getPackageArchiveInfo(output.getAbsolutePath(),PackageManager.GET_SIGNATURES);
            if(candidate==null)throw new IOException("Geçerli APK bilgisi okunamadı");
            UpdatePolicy.verifyArchive(context.getPackageName(),installed.versionCode,signers(installed.signatures),
                candidate.packageName,candidate.versionCode,signers(candidate.signatures),expectedVersion);
            return output;
        } catch(Exception e) {output.delete();throw e;}
    }
    private static Set<String> signers(Signature[] signatures) throws Exception {
        Set<String> values=new HashSet<>();
        if(signatures!=null)for(Signature s:signatures)values.add(UpdatePolicy.hash(s.toByteArray()));
        return values;
    }
    private static byte[] fetch(String address,int limit) throws IOException {
        HttpsURLConnection connection=(HttpsURLConnection)new URL(address).openConnection();
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
        connection.setRequestProperty("Cache-Control","no-cache");
        try {
            if(connection.getResponseCode()!=200)throw new IOException("Güncelleme sunucusu yanıtı: "+connection.getResponseCode());
            if(connection.getContentLengthLong()>limit)throw new IOException("Güncelleme dosyası boyut sınırını aşıyor");
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            try(InputStream input=connection.getInputStream()) {
                byte[] buffer=new byte[16384];int n;
                while((n=input.read(buffer))!=-1) {
                    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("İndirme durduruldu");
                    if(output.size()+n>limit)throw new IOException("Güncelleme dosyası boyut sınırını aşıyor");
                    output.write(buffer,0,n);
                }
            }
            return output.toByteArray();
        } finally {connection.disconnect();}
    }
}
