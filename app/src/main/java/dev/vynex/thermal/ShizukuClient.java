package dev.vynex.thermal;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import rikka.shizuku.Shizuku;

/** Main-thread connection lifecycle; the worker only calls read(). */
final class ShizukuClient {
    private static final int REQUEST=701;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable changed;
    private final Shizuku.UserServiceArgs args;
    private boolean foreground, wanted, binding, closed;
    private volatile IStatsService service;
    private ServiceConnection connection;
    private String status="Kapalı • isteğe bağlı";
    private final Shizuku.OnBinderReceivedListener received=()->{if(!closed) connect();};
    private final Shizuku.OnBinderDeadListener died=()->{
        disconnect();
        update("Shizuku durdu • Shizuku'yu başlatıp tekrar bağlan");
    };
    private final Shizuku.OnRequestPermissionResultListener permission=(code,result)->{
        if(code!=REQUEST || closed) return;
        if(result==PackageManager.PERMISSION_GRANTED) connect();
        else {wanted=false;update("Shizuku izni reddedildi • tekrar izin verebilirsin");}
    };
    private final Runnable timeout=()->{
        if(binding) {wanted=false;disconnect();update("Shizuku servisine bağlantı zaman aşımı • tekrar dene");}
    };
    ShizukuClient(Context context,Runnable changed) {
        this.changed=changed;
        args=new Shizuku.UserServiceArgs(new ComponentName(context,StatsUserService.class))
            .daemon(false).tag("thermal-stats").processNameSuffix("thermal_stats").version(5);
        Shizuku.addBinderReceivedListenerSticky(received,main);
        Shizuku.addBinderDeadListener(died,main);
        Shizuku.addRequestPermissionResultListener(permission,main);
    }
    String status(){return status;}
    boolean isWanted(){return wanted;}
    boolean isReady(){IStatsService s=service;return s!=null && s.asBinder().isBinderAlive();}
    void foreground(boolean value) {
        foreground=value;
        if(value) connect();
        else {disconnect();if(wanted) update("Duraklatıldı • uygulamaya dönünce bağlanır");}
    }
    void request() {
        wanted=true;
        try {
            if(!Shizuku.pingBinder()) {update("Shizuku çalışmıyor • önce Shizuku'yu başlat");return;}
            if(Shizuku.isPreV11()) {wanted=false;update("Shizuku sürümünü güncelle (v11+ gerekli)");return;}
            if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED) connect();
            else if(Shizuku.shouldShowRequestPermissionRationale()) {
                wanted=false;update("Shizuku → Yetkilendirilmiş uygulamalar içinden izin ver");
            } else {update("Shizuku izin penceresini onayla…");Shizuku.requestPermission(REQUEST);}
        } catch(RuntimeException e) {wanted=false;update("Shizuku'ya erişilemiyor • yeniden başlatıp dene");}
    }
    private void connect() {
        if(closed || !wanted || !foreground || binding || isReady()) return;
        try {
            if(!Shizuku.pingBinder() || Shizuku.isPreV11() || Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED) return;
            binding=true;update("Shizuku ölçüm servisine bağlanılıyor…");
            connection=new ServiceConnection() {
                @Override public void onServiceConnected(ComponentName name,IBinder binder) {
                    if(connection==this) connected(binder);
                }
                @Override public void onServiceDisconnected(ComponentName name) {
                    if(connection==this) disconnected();
                }
            };
            main.postDelayed(timeout,15000);Shizuku.bindUserService(args,connection);
        } catch(RuntimeException e) {disconnect();update("Shizuku servisi başlatılamadı • tekrar dene");}
    }
    private void connected(IBinder binder) {
        main.removeCallbacks(timeout);binding=false;
        if(closed || !wanted || !foreground) {disconnect();return;}
        service=IStatsService.Stub.asInterface(binder);
        if(!isReady()) {disconnect();update("Shizuku servisi bağlantısı geçersiz");return;}
        try {update("Bağlı • "+(Shizuku.getUid()==0?"root":"ADB/shell"));}
        catch(RuntimeException e){disable();}
    }
    private void disconnected() {
        service=null;binding=false;connection=null;main.removeCallbacks(timeout);
        if(!closed && wanted && foreground) update("Shizuku bağlantısı kesildi • tekrar bağlan");
    }
    ProcSnapshot read() throws Exception {
        IStatsService s=service;
        try {
        if(s==null || !Shizuku.pingBinder() || Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Shizuku izni/bağlantısı yok");
        ProcSnapshot snapshot=ProcSnapshot.parse(s.readSnapshot());
        return snapshot;
        } catch(Exception e) {failed(s);throw e;}
    }
    String readDesktopSetting() throws Exception {return authorizedService().readDesktopSetting();}
    String desktopDiagnostics() throws Exception {return authorizedService().desktopDiagnostics();}
    String listUserApps(int userId)throws Exception {return authorizedService().listUserApps(userId);}
    boolean forceStopApp(String pkg,int userId)throws Exception {return authorizedService().forceStopApp(AppManagementPolicy.validatePackage(pkg),userId);}
    void writeDesktopSetting(String value) throws Exception {authorizedService().writeDesktopSetting(DesktopSetting.valid(value));}
    private IStatsService authorizedService() {
        IStatsService s=service;
        if(s==null || !Shizuku.pingBinder() || Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Önce Süreçler sekmesinden Shizuku ile yetkilendir");
        return s;
    }
    private void failed(IStatsService failedService) {main.post(()->{
        if(!closed && wanted && foreground && service==failedService && failedService!=null) {
            wanted=false;disconnect();update("Sayaçlara erişilemedi • izin/bağlantıyı kontrol et; cihaz shell erişimini kısıtlayabilir");
        }
    });}
    void disable() {wanted=false;disconnect();update("Kapalı • isteğe bağlı");}
    private void disconnect() {
        main.removeCallbacks(timeout);service=null;
        ServiceConnection old=connection;connection=null;binding=false;
        try {if(old!=null && Shizuku.pingBinder()) Shizuku.unbindUserService(args,old,true);}
        catch(RuntimeException ignored) {}
    }
    void close() {
        closed=true;wanted=false;disconnect();
        Shizuku.removeBinderReceivedListener(received);Shizuku.removeBinderDeadListener(died);Shizuku.removeRequestPermissionResultListener(permission);
    }
    private void update(String message) {if(closed)return;status=message;changed.run();}
}
