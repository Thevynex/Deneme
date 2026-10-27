package dev.vynex.thermal;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded subprocess output for a fixed application-management command plan. */
final class AppShell {
    static String run(String[] command)throws Exception {
        java.lang.Process process=new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        AtomicReference<IOException> failure=new AtomicReference<>();
        Thread reader=new Thread(()->{
            try(InputStream input=process.getInputStream()) {
                byte[] bytes=new byte[4096];int n;
                while((n=input.read(bytes))!=-1) {
                    if(output.size()+n>256000)throw new IOException("Komut çıktısı boyut sınırını aşıyor");
                    output.write(bytes,0,n);
                }
            } catch(IOException e){failure.set(e);process.destroyForcibly();}
        },"app-management-output");
        reader.setDaemon(true);reader.start();
        try {
            if(!process.waitFor(8,TimeUnit.SECONDS))throw new IOException("Android komutu zaman aşımına uğradı");
            reader.join(1000);
            if(reader.isAlive())throw new IOException("Komut çıktısı tamamlanmadı");
            if(failure.get()!=null)throw failure.get();
            String result=output.toString("UTF-8");
            if(process.exitValue()!=0 || result.trim().startsWith("Error:") || result.contains("java.lang.SecurityException"))
                throw new IOException("Android isteği reddetti; uygulama veya cihaz politikası engelliyor olabilir");
            return result;
        } finally {process.destroyForcibly();}
    }
}
