package dev.vynex.thermal;

import android.system.Os;
import android.system.OsConstants;
import android.os.RemoteException;
import java.io.File;
import java.io.*;
import java.util.concurrent.TimeUnit;

/** Instantiated by Shizuku as shell/root, never registered as an exported Android service. */
public final class StatsUserService extends IStatsService.Stub {
    public StatsUserService() {}
    @Override public String readSnapshot() throws RemoteException {
        try {
            return SnapshotFiles.read(new File("/proc"), new File("/sys/class/thermal"), Os.sysconf(OsConstants._SC_PAGESIZE));
        } catch(Exception e) {throw new RemoteException("Sayaçlar okunamadı: " + e.getClass().getSimpleName());}
    }
    @Override public String readDesktopSetting() throws RemoteException {
        try {supported();return DesktopSetting.valid(command(DesktopSetting.readCommand()).trim());}
        catch(Exception e){throw new RemoteException("Masaüstü ayarı okunamadı: "+e.getMessage());}
    }
    @Override public void writeDesktopSetting(String value) throws RemoteException {
        try {
            supported();command(DesktopSetting.writeCommand(value));
            if(!value.equals(readDesktopSetting()))throw new IOException("Yazılan ayar geri okunarak doğrulanamadı");
        } catch(Exception e){throw new RemoteException("Masaüstü ayarı değiştirilemedi: "+e.getMessage());}
    }
    private static void supported() throws IOException {
        if(android.os.Build.VERSION.SDK_INT<36)throw new IOException("Bu seçenek Android 16+ içindir");
    }
    @Override public String desktopDiagnostics() {
        StringBuilder report=new StringBuilder("Masaüstü tanılaması • yalnızca okuma\nServis UID: "+android.os.Process.myUid()+"\n");
        for(java.util.Map.Entry<String,String[]> item:DesktopDiagnostics.commands().entrySet()) {
            report.append('\n').append(item.getKey()).append(":\n");
            try {
                String value=command(item.getValue(),item.getValue()[0].endsWith("aflags"),3).trim();
                report.append(value.isEmpty()?"(atanmamış/boş)":value).append('\n');
            } catch(Exception e){report.append("Okunamadı: ").append(e.getClass().getSimpleName()).append(" • ").append(e.getMessage()).append('\n');}
        }
        report.append("\nBu rapor gerçek masaüstü davranışının kapandığını doğrulamaz. read-only bayraklar shell yetkisiyle değiştirilemeyebilir.\n");
        return report.toString();
    }
    private static String command(String[] args) throws Exception {
        return command(args,false,6);
    }
    private static String command(String[] args,boolean filterFlags,int seconds) throws Exception {
        java.lang.Process p=new ProcessBuilder(args).redirectErrorStream(true).start();
        // settings get/put produces only a short line. Drain concurrently to bound both time and output.
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        Thread reader=new Thread(()->{
            try(InputStream input=p.getInputStream()) {
                if(filterFlags){out.write(DesktopDiagnostics.desktopFlags(new InputStreamReader(input,"UTF-8")).getBytes("UTF-8"));return;}
                byte[] buffer=new byte[1024];int n;
                while((n=input.read(buffer))!=-1){if(out.size()+n>16384){p.destroyForcibly();break;}out.write(buffer,0,n);}
            } catch(IOException ignored){}
        });
        reader.setDaemon(true);reader.start();
        try {
            if(!p.waitFor(seconds,TimeUnit.SECONDS))throw new IOException("Ayar komutu zaman aşımı");
            reader.join(1000);
            if(reader.isAlive() || p.exitValue()!=0)throw new IOException("Shell ayara erişemedi");
            return out.toString("UTF-8");
        } finally {p.destroyForcibly();}
    }
    @Override public void destroy() {System.exit(0);}
}
