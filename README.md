# Isı Dedektifi 1.3.0 — Android

[Türkçe APK'yı indir](https://github.com/Thevynex/Deneme/raw/refs/heads/main/downloads/Isi-Dedektifi.apk) · Android 8.0+

Pil sıcaklığı, sistem RAM'i, Android termal baskı durumu, pil akımı/gerilimi, depolama ve kullanım erişimiyle yaklaşık ön plan kullanım geçmişi. Reklam ve analiz SDK'sı yoktur.

## 1.3: Seçtiğin uygulamayı yönet ve RAM farkını ölç

1. **Süreçler → Shizuku ile yetkilendir ve ölç** ile bağlantıyı kur.
2. **Uygulamalar → Shizuku ile uygulamaları listele / yenile** düğmesine dokun.
3. İsim veya paket adıyla ara, yönetilecek uygulamayı seç.
4. **Zorla durdur ve RAM farkını ölç** seçilen paketi Android `am force-stop` ile durdurur. İşlem öncesi ve yaklaşık 1,5 saniye sonrası kullanılabilir sistem RAM'i gösterilir. Son durum `dumpsys package` içindeki ilgili profilin `stopped` durumu ile kontrol edilir; okunamayan durum başarı olarak gösterilmez.
5. **Kaldırma ekranını aç** Android'in standart kaldırma onay ekranını açar; veri kaybına yol açabilen kaldırmayı kullanıcı burada onaylar. Sessiz kaldırma yapılmaz.

Yalnızca uygulamanın çalıştığı Android profilindeki kurulu üçüncü taraf uygulamalar hedeflenir. Hedef listesi `pm list packages -3 --user <profil>` ile alınır ve durdurma öncesinde tekrar kontrol edilir. Sistem uygulamaları, Isı Dedektifi ve Shizuku yönetim listesinden çıkarılır. Paket adı ve Binder çağıran UID/profil sınırı serviste doğrulanır; genel amaçlı komut çalıştırma arayüzü yoktur. Paket bilgileri Android görünürlük sınırına takılırsa paket adı gösterilir.

Zorla durdurma kaydedilmemiş işleri, bildirimleri ve arka plan görevlerini etkileyebilir; kullanıcı uygulamayı tekrar açana kadar bunlar durabilir. Her durdurma, seçilen paket adı gösterilerek kullanıcı tarafından başlatılır. RAM artışı garanti edilmez: sistemin önbellek kullanımı normaldir ve diğer uygulamalar da ölçüm farkını etkiler. Toplu RAM temizliği yapılmaz. Pil tasarrufu ve Android pil kullanım ayarları için kısayollar eklenmiştir.

Bu uygulama asistana telefonuna uzaktan erişim sağlamaz. İşlemler cihaz üzerinde kullanıcı seçimiyle başlatılır; uygulama listesi veya ölçümler sunucuya gönderilmez. Gerçek telefonda kaldırma/force-stop ve RAM değişimi burada doğrulanmadı.

## Shizuku ile root olmadan süreç ölçümü

1. [Shizuku'yu kur ve başlat](https://shizuku.rikka.app/guide/setup/). Android 11+ için kablosuz hata ayıklama; eski Android sürümleri için bilgisayardan ADB kullanılabilir.
2. Isı Dedektifi → **Süreçler → Shizuku ile yetkilendir ve ölç** düğmesine dokun, Shizuku izin isteğini onayla.
3. CPU yüzdeleri için iki ölçüm bekle. CPU veya RAM sıralamasını değiştirebilirsin.

Shizuku v11+ gerekir; güncel sürüm önerilir. SDK 13.1.5 kullanılır. Shizuku/Sui root ile başlatılmışsa root, ADB ile başlatılmışsa shell yetkileri kullanılır. Shizuku telefonu rootlamaz ve shell erişimi tam root yetkisi sağlamaz. Üreticinin erişime kapattığı sayaçlar erişilemiyor olarak gösterilir. Shizuku izni, kullanım geçmişi izninden ayrıdır.

Servis yalnızca kullanıcı bağlantı isteği verdikten sonra başlatılır. Uygulama arka plana geçince ölçüm servisi kaldırılır, ön plana dönünce istenen bağlantı yeniden kurulur. İzin reddi, servis ölümü ve bağlantı zaman aşımı ekranda gösterilir. Telefon yeniden başlatıldığında Shizuku'yu yeniden başlatmak gerekebilir. Root isteyenler için eski **Root izni iste ve ölç** seçeneği de bulunur.

## Araçlar → Güncelleme

**Güncellemeyi kontrol et ve indir** düğmesi GitHub'daki `downloads/update.json` ve APK'yı HTTPS ile alır. APK SHA-256 özeti, paket adı, sürümü ve kurulu uygulamanın imzası doğrulanır. Android'in APK yükleme izni ve kurulum onayı kullanıcı tarafından verilir; sessiz kurulum yapılmaz. İlk eski sürümde güncelleyici bulunmadığı için bu APK bir kez elle kurulmalıdır.

İnternet izni yalnızca kullanıcı tarafından istenen güncelleme indirmesi içindir; cihaz ölçümleri sunucuya gönderilmez. Güncelleme dosyası sınırı 40 MiB, manifest sınırı 16 KiB'dir. İmza uyuşmazlığı, bozuk dosya ve eski sürüme dönüş reddedilir. Doğrudan indirilen APK'lar aynı yerel geliştirme anahtarıyla imzalanır. GitHub Actions geçici bir debug anahtarı üretir; Actions APK'sı ile doğrudan indirilen APK'nın imzaları farklı olabilir. Böyle bir kurulum güncelleyicide açık hata verir.

## Araçlar → Android 16 masaüstü deneyimi

Ekrandaki **Enable desktop experience features** için AOSP Android 16 karşılığı `global override_desktop_experience_features` ayarıdır. Araç mevcut değeri okur, kapatma istenirse eski değeri uygulamanın özel alanına yedekler, `0` yazar ve geri okuyarak doğrular. Eski değeri geri yükleme düğmesi de vardır. Yazma sadece Shizuku bağlantısıyla ve kullanıcı onayıyla yapılır. Android 16'dan eski sistemlerde bu işlev reddedilir.

Yeniden başlatma otomatik yapılmaz. Samsung One UI 8.5 üretici politikası/özellik bayrakları bu seçeneği ayrıca kilitleyebilir. Ayarın `0` olması gerçek pencere davranışının kapandığını tek başına kanıtlamaz; yeniden başlatıp geliştirici seçeneklerinden ve harici ekran davranışından kontrol edilmelidir. Bu seçeneğin telefonun ısınmasına neden olduğu doğrulanmış değildir. [Araştırma, komutlar ve kaynaklar](DESKTOP-MODE.md).

## 1.2.1: Masaüstü tanılaması

Samsung cihazında ayar `0` ve yeniden başlatma sonrasında seçeneğin açık kaldığı bildirildi. AOSP'de masaüstü deneyimi yolu `OVERRIDE_OFF` için derleme varsayılanını kullanabilir; `0` her masaüstü özelliğini zorla kapatmaz. Geliştirici ayarının veritabanındaki değeri, görsel anahtar ve gerçek pencere özellikleri ayrı gözlemlerdir.

**Araçlar → Masaüstü tanılamasını oku (Shizuku)** mevcut ayarı, WMS'nin kullandığı `persist.wm.debug.desktop_experience_devopts` özelliğini ve erişilebilen masaüstü aconfig bayraklarını okur. Eski alternatif ayarlar, derleme tipi ve shell/root servis UID'si de raporlanır. **Tanılama raporunu kopyala** düğmesi sonucu panoya alır; otomatik gönderim yapılmaz. Herhangi bir ayar, özellik veya bayrak değiştirilmez. Okunamayan/olmayan komutlar açıkça belirtilir; sonuç tüm bayrakların mevcut olduğunu garanti etmez. `read-only` bayraklar shell yetkisiyle değiştirilemeyebilir.

Tanılama cihaz modelini ve Android sürümünü içerir; seri numarası, IMEI veya Android ID okunmaz. Araçtaki yazma düğmesi artık **AOSP masaüstü açma zorlamasını kaldır** olarak adlandırılır; gerçek masaüstü durumunun kapandığı iddia edilmez.

## Ölçümlerin anlamı ve sınırları

- Pil sıcaklığı CPU sıcaklığı değildir. Root/Shizuku ile okunabilen termal sensörler üreticinin adlarıyla gösterilir; hepsi CPU'ya ait değildir.
- CPU yüzdesi tüm çekirdeklerin toplam kapasitesine göre normalize edilir. İlk örnekte yüzde üretilmez. PID tekrar kullanımı ve sayaç sıfırlanması eski örneği geçersiz kılar.
- Süreç RAM'i RSS'dir; paylaşılan sayfalar tekrar sayılabilir. Uygulama toplamı veya PSS değildir. Bu uygulamanın kendi RAM'i ayrıca PSS olarak gösterilir. 4K/16K sayfa boyutu cihazdan okunur.
- Shell kullanıcısının toplam CPU sayacına erişimi kapalıysa RAM verisi korunur ve CPU yüzdesi hesaplanmaz. Süreçler örnekleme sırasında kapanabilir. Süreç adı kısaltılabilir; bir uygulama birden fazla sürece sahip olabilir.
- Binder yanıtı boyut sınırına ulaşılırsa süreç listesi eksik olarak işaretlenir. Ekranda sıralanmış ilk 80 süreç, paylaşım raporunda yakalanan tüm süreçler bulunur.
- Kullanım geçmişi olay kayıtlarından yaklaşık ön plan süresidir; CPU/RAM ölçümü veya ısınma nedeni değildir. Çoklu pencere ve üretici kayıt politikası etkileyebilir. Dakikada bir yenilenir; arka plan işleri burada ölçülmez.

Canlı ölçümler yalnızca ön planda, 5 saniyelik aralıklarla alınır. Kullanıcı seçimi dışında süreç durdurma, otomatik RAM temizliği, sessiz güncelleme veya otomatik yeniden başlatma yoktur. Paylaşım raporu cihaz modelini ve uygulama/süreç adlarını içerebilir; Android paylaşım ekranından kullanıcı gönderir.

## Derleme ve doğrulama

JDK 21, Gradle 8.13, SDK Platform 35 ve Build Tools 35.0.0:

```sh
export ANDROID_HOME=/path/to/android-sdk
bash scripts/test.sh
gradle :app:lintDebug :app:assembleDebug
```

Çıktı: `app/build/outputs/apk/debug/app-debug.apk`. Yönetilen bulut proxy/CA ayarları gerekiyorsa `python3 scripts/network-run.py <komut>` kullanılabilir. GitHub Actions aynı test/lint/APK adımlarını çalıştırır; sonuç **Artifacts → Isi-Dedektifi-APK** bölümündedir.

100 kontrol: CPU/RSS ayrıştırma, PID tekrar kullanımı, 16K sayfalar, kısıtlı dosya erişimi, Binder çıktı sınırı, masaüstü ayarı komut sınırları, APK hash/paket/sürüm/imza politikaları salt okunur masaüstü tanılaması, tam paket hedefleme, profil izolasyonu, durdurma durumu ve sınırlı komut çıktısı. Derleme ve lint çalıştırılmıştır. Gerçek telefonda Shizuku izin/bağlantı yaşam döngüsü, Android kurulum ekranı ve Samsung masaüstü etkisi henüz doğrulanmamıştır.
