package dev.vynex.thermal;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(16,24,32), CARD = Color.rgb(26,38,48),
        TEXT = Color.rgb(236,244,246), MUTED = Color.rgb(164,184,194), GREEN = Color.rgb(87,226,177);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayDeque<Float> history = new ArrayDeque<>();
    private final Map<String,String> labels = new HashMap<>();
    private LinearLayout body, tabBar;
    private ScrollView scroll;
    private boolean active, busy, rootEnabled, rootFirst, sortRam;
    private int tab;
    private String rootState = "Kapalı • isteğe bağlı", error = "";
    private ProcSnapshot previous;
    private List<AppUse> cachedApps = new ArrayList<>();
    private long usageUpdated;
    private State state = new State();
    private final Runnable poll = () -> refresh(false);
    private static class AppUse {
        String pkg, label; long ms, last;
        AppUse(String pkg, String label, long ms, long last) { this.pkg=pkg; this.label=label; this.ms=ms; this.last=last; }
    }
    private static class State {
        long time, totalRam, availableRam, appPss, storageFree, storageTotal;
        boolean lowMemory, charging, usageAccess;
        float temp = Float.NaN, battery = Float.NaN;
        int thermal = -1, voltageMv, currentMicroamps = Integer.MIN_VALUE;
        Double cpu;
        List<ProcSnapshot.Row> processes = new ArrayList<>();
        List<String> sensors = new ArrayList<>();
        List<AppUse> apps = new ArrayList<>();
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root = column(); root.setBackgroundColor(BG);
        root.setPadding(dp(18),0,dp(18),0);
        root.setOnApplyWindowInsetsListener((v,insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(dp(18)+i.left,i.top,dp(18)+i.right,i.bottom);
            }
            return insets;
        });
        root.addView(text("ISı DEDEKTİFİ".toUpperCase(new Locale("tr")),12,GREEN));
        root.addView(text("Telefonunda neler oluyor?",25,TEXT));
        TextView subtitle = text("Canlı durum · Yerel ve reklamsız",13,MUTED); subtitle.setPadding(0,dp(4),0,dp(12)); root.addView(subtitle);
        tabBar = new LinearLayout(this); root.addView(tabBar);
        scroll = new ScrollView(this); scroll.setFillViewport(true);
        body = column(); body.setPadding(0,dp(10),0,dp(24)); scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); root.requestApplyInsets(); render();
    }
    @Override protected void onResume() { super.onResume(); active=true; previous=null; refresh(false); }
    @Override protected void onPause() { active=false; handler.removeCallbacks(poll); super.onPause(); }
    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
    private void refresh(boolean enableRoot) {
        handler.removeCallbacks(poll);
        if (!active || busy) return;
        if (enableRoot) { rootEnabled=true; rootFirst=true; rootState="Root izni bekleniyor…"; previous=null; }
        busy=true;
        worker.execute(() -> {
            State next;
            String problem="";
            try { next=sample(); } catch (Exception e) { next=state; problem="Ölçüm alınamadı: " + e.getClass().getSimpleName(); }
            State result=next; String message=problem;
            handler.post(() -> {
                busy=false; if (isDestroyed()) return;
                state=result; error=message;
                if (Float.isFinite(result.temp)) {
                    history.addLast(result.temp); while (history.size()>60) history.removeFirst();
                }
                render(); if (active) handler.postDelayed(poll,5000);
            });
        });
    }
    private State sample() throws Exception {
        State s = new State(); s.time=System.currentTimeMillis();
        ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo(); am.getMemoryInfo(info);
        s.totalRam=info.totalMem; s.availableRam=info.availMem; s.lowMemory=info.lowMemory;
        android.os.Debug.MemoryInfo own=new android.os.Debug.MemoryInfo(); android.os.Debug.getMemoryInfo(own); s.appPss=own.getTotalPss()*1024L;
        StatFs disk=new StatFs(getFilesDir().getPath()); s.storageFree=disk.getAvailableBytes(); s.storageTotal=disk.getTotalBytes();
        Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b!=null) {
            if (b.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
                float temp=b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10f;
                if (temp>0 && temp<100) s.temp=temp;
            }
            int scale=b.getIntExtra(BatteryManager.EXTRA_SCALE,-1), level=b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            if (scale>0 && level>=0) s.battery=100f*level/scale;
            s.charging=b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
            s.voltageMv=b.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0);
        }
        BatteryManager batteryManager=(BatteryManager)getSystemService(BATTERY_SERVICE);
        s.currentMicroamps=batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT>=29) s.thermal=pm.getCurrentThermalStatus();
        if (rootEnabled) {
            try {
                ProcSnapshot snapshot=RootReader.read(rootFirst);
                rootFirst=false; rootState="Açık • salt okunur ölçüm";
                s.cpu=snapshot.cpuSince(previous); s.processes=snapshot.rowsSince(previous);
                s.sensors=snapshot.temperatures; previous=snapshot;
            } catch (Exception e) {
                rootEnabled=false; rootFirst=false; previous=null;
                rootState="Erişilemedi • root yok, reddedildi veya süre aşıldı";
            }
        } else {
            // A few vendor ROMs permit this. Never substitute this app's CPU for device CPU.
            try (BufferedReader reader=new BufferedReader(new FileReader("/proc/stat"))) {
                ProcSnapshot snapshot=ProcSnapshot.parse(reader.readLine());
                s.cpu=snapshot.cpuSince(previous); previous=snapshot;
            } catch (Exception ignored) { previous=null; }
        }
        AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
        s.usageAccess=ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;
        if (s.usageAccess) {
            if(s.time-usageUpdated>=60000) { cachedApps=usage(s.time); usageUpdated=s.time; }
            s.apps=cachedApps;
        } else { cachedApps=new ArrayList<>(); usageUpdated=0; }
        return s;
    }
    private List<AppUse> usage(long end) {
        long start=end-TimeUnit.HOURS.toMillis(24);
        UsageStatsManager manager=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
        UsageEvents events=manager.queryEvents(start-TimeUnit.HOURS.toMillis(24),end);
        if(events==null) return new ArrayList<>();
        UsageEvents.Event event=new UsageEvents.Event();
        Map<String,Long> opened=new HashMap<>(), durations=new HashMap<>(), last=new HashMap<>();
        while(events.hasNextEvent()) {
            events.getNextEvent(event); String pkg=event.getPackageName(); long when=event.getTimeStamp();
            if (pkg==null) continue;
            int type=event.getEventType();
            if (type==UsageEvents.Event.ACTIVITY_RESUMED) {
                opened.putIfAbsent(pkg,when); if(when>=start) last.put(pkg,when);
            } else if (type==UsageEvents.Event.ACTIVITY_PAUSED || type==UsageEvents.Event.ACTIVITY_STOPPED) {
                Long began=opened.remove(pkg);
                if(began!=null && when>start) durations.merge(pkg,Math.max(0,when-Math.max(start,began)),Long::sum);
            } else if (type==UsageEvents.Event.DEVICE_SHUTDOWN || type==UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                for (Map.Entry<String,Long> e:opened.entrySet()) if (when>start)
                    durations.merge(e.getKey(),Math.max(0,when-Math.max(start,e.getValue())),Long::sum);
                opened.clear();
            }
        }
        for (Map.Entry<String,Long> e:opened.entrySet()) durations.merge(e.getKey(),end-Math.max(start,e.getValue()),Long::sum);
        List<AppUse> apps=new ArrayList<>();
        for (Map.Entry<String,Long> e:durations.entrySet()) if(e.getValue()>0)
            apps.add(new AppUse(e.getKey(),label(e.getKey()),Math.min(TimeUnit.HOURS.toMillis(24),e.getValue()),last.getOrDefault(e.getKey(),0L)));
        apps.sort((a,b)->Long.compare(b.ms,a.ms)); return apps;
    }
    private String label(String pkg) {
        if(labels.containsKey(pkg)) return labels.get(pkg);
        String value=pkg;
        try { value=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString(); }
        catch(PackageManager.NameNotFoundException ignored) {}
        labels.put(pkg,value); return value;
    }
    private void render() {
        tabBar.removeAllViews();
        String[] names={"Genel","Uygulamalar","Süreçler"};
        for(int i=0;i<names.length;i++) {
            final int index=i;
            Button b=button(names[i],()->{tab=index;scroll.scrollTo(0,0);render();});
            b.setTextColor(tab==i?BG:MUTED); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tab==i?GREEN:CARD));
            tabBar.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
        }
        body.removeAllViews();
        if(!error.isEmpty()) body.addView(text(error,14,Color.rgb(255,160,120)));
        if(state.time==0) {body.addView(text("İlk ölçüm alınıyor…",18,MUTED));return;}
        if(tab==0) overview(); else if(tab==1) apps(); else processes();
        TextView time=text("Son ölçüm: " + new java.text.SimpleDateFormat("HH:mm:ss",new Locale("tr")).format(new Date(state.time))+" · 5 sn aralık",12,MUTED);
        time.setPadding(0,dp(16),0,dp(6)); body.addView(time);
    }
    private void overview() {
        LinearLayout heat=card("PİL SICAKLIĞI");
        TextView temp=text(Float.isFinite(state.temp)?String.format(Locale.ROOT,"%.1f °C",state.temp):"Okunamıyor",42,Float.isFinite(state.temp)&&state.temp>=40?Color.rgb(255,165,110):GREEN);
        heat.addView(temp); heat.addView(text("Bu değer CPU sıcaklığı değildir.",13,MUTED));
        heat.addView(new Graph(this,new ArrayList<>(history)),new LinearLayout.LayoutParams(-1,dp(76)));
        heat.addView(text("Son 60 ölçüm · yalnızca ekran açıkken birikir",12,MUTED));
        metric("Sistem ısı durumu",thermal(state.thermal),"Android'in bildirdiği termal baskı seviyesi; desteklenmeyen cihazlarda okunamayabilir.");
        long used=Math.max(0,state.totalRam-state.availableRam);
        metric("SİSTEM RAM",bytes(used)+" / "+bytes(state.totalRam),"Kullanılabilir: "+bytes(state.availableRam)+" • "+(state.lowMemory?"Düşük bellek uyarısı":"Bellek baskısı uyarısı yok")+"\nÖnbellek RAM kullanabilir; tek başına ısınma kanıtı değildir.");
        metric("TOPLAM CPU",state.cpu==null?"Erişilemiyor / örnek bekleniyor":percent(state.cpu),"Tüm çekirdeklerin toplam kapasitesindeki kullanım. Android bu sayacı rootsuz erişime kapatabilir.");
        metric("PİL VE DEPOLAMA",(Float.isFinite(state.battery)?Math.round(state.battery)+"%":"—")+(state.charging?" · Şarj bağlı":" · Şarj bağlı değil"),
            "Gerilim: "+(state.voltageMv>0?String.format(Locale.ROOT,"%.2f V",state.voltageMv/1000.0):"okunamıyor")+
            "\nAnlık pil akımı: "+(state.currentMicroamps==Integer.MIN_VALUE?"okunamıyor":String.format(Locale.ROOT,"%+.0f mA",state.currentMicroamps/1000.0))+
            "\nAkım işareti üreticiye bağlıdır; uygulama başına tüketim değildir."+
            "\nDepolama boş: "+bytes(state.storageFree)+" / "+bytes(state.storageTotal)+"\nBu uygulamanın RAM'i (PSS): "+bytes(state.appPss));
        LinearLayout tips=card("NE YAPABİLİRSİN?");
        tips.addView(text("Şarj sırasında ağır oyun/video kullanımı ısıyı artırabilir. Isınma sürerse şarjı çıkar, parlaklığı azalt ve telefonu bir süre dinlendir. Uygulamaların pil tüketimini sistem ekranından incele.",15,TEXT));
        tips.addView(button("Android pil kullanımını aç",()->open(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY))));
        tips.addView(button("Ölçüm raporunu paylaş",this::share));
        if(!state.sensors.isEmpty()) {
            LinearLayout c=card("CİHAZIN TERMAL SENSÖRLERİ · ROOT");
            c.addView(text("Üretici sensör adlarıdır; her sensör CPU'ya ait değildir.",13,MUTED));
            for(String sensor:state.sensors) c.addView(text(sensor,14,TEXT));
        }
    }
    private void apps() {
        LinearLayout intro=card("SON 24 SAAT · ÖN PLAN KULLANIMI");
        intro.addView(text("Kullanım süresi, CPU/RAM tüketimi veya ısınmanın nedeni değildir. Rootsuz Android diğer uygulamaların canlı CPU/RAM verisini paylaşmaz. Arka plan işleri bu listede ölçülmez.",14,MUTED));
        intro.addView(button(state.usageAccess?"Kullanım erişimini yönet":"Kullanım erişimine izin ver",()->open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        if(!state.usageAccess) {intro.addView(text("Açılan listede Isı Dedektifi'ni seçip kullanım erişimini etkinleştir.",15,TEXT));return;}
        intro.addView(text("Olay kayıtlarından yaklaşık süre; dakikada bir yenilenir. Üretici kayıtları ve çoklu pencere sonucu etkileyebilir.",12,MUTED));
        if(state.apps.isEmpty()) body.addView(text("Görülebilen kullanım kaydı yok.",16,MUTED));
        for(AppUse app:state.apps) {
            LinearLayout c=card(app.label);
            c.addView(text(duration(app.ms),24,GREEN)); c.addView(text(app.pkg,12,MUTED));
            c.addView(button("Uygulama ayrıntıları",()->appInfo(app.pkg)));
        }
    }
    private void processes() {
        LinearLayout intro=card("CANLI CPU / RAM · İSTEĞE BAĞLI ROOT");
        intro.addView(text("Durum: "+rootState,14,GREEN));
        intro.addView(text("Root mevcutsa, izin verdiğinde salt okunur /proc ölçümü yapılır. Bu uygulama telefonu rootlamaz. Süreç adı kısaltılmış olabilir; bir uygulamanın birden fazla süreci olabilir. RAM: RSS (paylaşılan sayfalar dahil), uygulama toplamı değildir.",14,MUTED));
        if(rootEnabled) intro.addView(button("Root ölçümünü kapat",()->{
            if(busy) {Toast.makeText(this,"Ölçüm tamamlanınca tekrar dene",Toast.LENGTH_SHORT).show();return;}
            rootEnabled=false;rootState="Kapalı • isteğe bağlı";previous=null;state.processes.clear();state.sensors.clear();state.cpu=null;render();
        }));
        else intro.addView(button("Root izni iste ve ölç",()->{
            if(busy) {Toast.makeText(this,"Ölçüm sürüyor, birazdan tekrar dene",Toast.LENGTH_SHORT).show();return;}
            new AlertDialog.Builder(this).setTitle("Root ölçümü")
                .setMessage("Yalnızca süreç sayaçları ve termal sensörler okunacak. Root yöneticin varsa izin penceresi açılabilir. Sistem ayarları değiştirilmeyecek.")
                .setNegativeButton("Vazgeç",null).setPositiveButton("Ölç",(d,w)->{refresh(true);render();}).show();
        }));
        if(state.processes.isEmpty()) {intro.addView(text("Rootsuz kullanımda diğer uygulamaların CPU ve RAM değerleri erişilemez. Kullanım geçmişi için Uygulamalar sekmesini aç.",15,TEXT));return;}
        intro.addView(button(sortRam?"Sıralama: RAM ↓ • CPU için dokun":"Sıralama: CPU ↓ • RAM için dokun",()->{sortRam=!sortRam;render();}));
        intro.addView(text("CPU: tüm çekirdekler toplamında %; ilk örnekte —. Süreçler eşzamanlı okunmadığından küçük ölçüm sapmaları olabilir.",12,MUTED));
        List<ProcSnapshot.Row> rows=new ArrayList<>(state.processes);
        rows.sort((a,b)->sortRam?Long.compare(b.entry().rssBytes(),a.entry().rssBytes()):Double.compare(b.cpu()==null?-1:b.cpu(),a.cpu()==null?-1:a.cpu()));
        // Bound rendered views to keep the monitor lightweight; report includes every captured row.
        int limit=Math.min(80,rows.size());
        intro.addView(text(rows.size()+" süreç yakalandı · ilk "+limit+" gösteriliyor",13,MUTED));
        for(int i=0;i<limit;i++) {
            ProcSnapshot.Row r=rows.get(i);
            LinearLayout c=card(r.entry().name());
            c.addView(text("CPU "+(r.cpu()==null?"—":percent(r.cpu()))+"   ·   "+bytes(r.entry().rssBytes()),20,GREEN));
            c.addView(text("PID "+r.entry().pid()+" · RAM: RSS",12,MUTED));
        }
        body.addView(button("Tüm süreçleri raporla paylaş",this::share));
    }
    private void share() {
        StringBuilder s=new StringBuilder("Isı Dedektifi 1.0\n");
        s.append("Cihaz: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" · Android ").append(Build.VERSION.RELEASE).append('\n');
        s.append("Ölçüm: ").append(new Date(state.time)).append('\n');
        s.append("Pil sıcaklığı (CPU değil): ").append(Float.isFinite(state.temp)?state.temp+" °C":"erişilemiyor").append('\n');
        s.append("Termal durum: ").append(thermal(state.thermal)).append('\n');
        s.append("Sistem RAM kullanımı: ").append(bytes(state.totalRam-state.availableRam)).append(" / ").append(bytes(state.totalRam)).append('\n');
        s.append("Toplam CPU: ").append(state.cpu==null?"erişilemiyor / örnek bekleniyor":percent(state.cpu)).append('\n');
        s.append("Root: ").append(rootState).append("\nSüreçler: CPU toplam kapasite %, RAM RSS; paylaşılan RAM tekrar sayılabilir.\n");
        for(ProcSnapshot.Row r:state.processes) s.append(r.entry().pid()).append(' ').append(r.entry().name()).append(" CPU ").append(r.cpu()==null?"—":percent(r.cpu())).append(" RSS ").append(bytes(r.entry().rssBytes())).append('\n');
        for(String sensor:state.sensors) s.append(sensor).append('\n');
        s.append("\n24 saatlik yaklaşık ön plan kullanımı (CPU/RAM ölçümü değildir):\n");
        for(AppUse app:state.apps) s.append(app.pkg).append(": ").append(duration(app.ms)).append('\n');
        new AlertDialog.Builder(this).setTitle("Raporu paylaş")
            .setMessage("Rapor cihaz modelini, süreç adlarını ve izin verdiysen uygulama kullanım geçmişini içerir. Paylaşılacak uygulamayı sen seçersin.")
            .setNegativeButton("Vazgeç",null).setPositiveButton("Devam",(d,w)->open(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,s.toString()),"Ölçüm raporu"))).show();
    }
    private void appInfo(String pkg) {open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg)));}
    private void open(Intent intent) {try {startActivity(intent);}catch(ActivityNotFoundException|SecurityException e){Toast.makeText(this,"Bu ekran cihazda açılamıyor",Toast.LENGTH_LONG).show();}}
    private void metric(String label,String value,String note) {LinearLayout c=card(label);c.addView(text(value,24,TEXT));c.addView(text(note,13,MUTED));}
    private LinearLayout card(String title) {
        LinearLayout c=column(); c.setPadding(dp(16),dp(14),dp(16),dp(14));
        GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(18));c.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);body.addView(c,lp);
        TextView t=text(title,12,MUTED);t.setPadding(0,0,0,dp(8));c.addView(t);return c;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(3),0,dp(3));t.setTextIsSelectable(true);return t;}
    private Button button(String name,Runnable action){Button b=new Button(this);b.setText(name);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(GREEN);b.setOnClickListener(v->action.run());return b;}
    private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private static String percent(double p){return String.format(Locale.ROOT,"%.1f%%",p);}
    private static String bytes(long n){return n>=1024L*1024*1024?String.format(Locale.ROOT,"%.2f GiB",n/(1024.0*1024*1024)):String.format(Locale.ROOT,"%.1f MiB",n/(1024.0*1024));}
    private static String duration(long ms){long m=ms/60000;return m>=60?(m/60)+" sa "+(m%60)+" dk":m>0?m+" dk":"1 dk'dan az";}
    private static String thermal(int n){String[] names={"Normal","Hafif","Orta","Ciddi","Kritik","Acil","Kapanma eşiği"};return n>=0&&n<names.length?names[n]:"Sistem bildirmiyor";}
    private static class Graph extends View {
        private final List<Float> values; private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        Graph(Context c,List<Float> values){super(c);this.values=values;setContentDescription("Pil sıcaklığı geçmişi grafiği");}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float w=getWidth(),h=getHeight();
            paint.setColor(Color.rgb(45,64,76));paint.setStrokeWidth(1);
            canvas.drawLine(0,h/2,w,h/2,paint);
            if(values.size()<2)return;
            float min=Collections.min(values)-1,max=Collections.max(values)+1;
            path.reset();for(int i=0;i<values.size();i++){
                float x=i*w/(values.size()-1),y=h-8-(values.get(i)-min)/(max-min)*(h-16);
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            paint.setColor(GREEN);paint.setStrokeWidth(4);paint.setStyle(Paint.Style.STROKE);canvas.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
        }
    }
}
