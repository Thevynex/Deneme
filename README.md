# Isı Dedektifi — Android

Türkçe, reklamsız, internete veri göndermeyen telefon durum izleyicisi. Android 8.0+.

- Pil sıcaklığı ve son 60 ölçümün grafiği. **Pil sıcaklığı CPU sıcaklığı değildir.**
- Android termal baskı durumu (Android 10+ ve üretici desteği gerekir).
- Sistem RAM'i, kullanılabilir bellek, düşük bellek uyarısı, depolama ve pil durumu.
- Kullanım erişimi verilirse son 24 saatlik yaklaşık ön plan kullanım süreleri ve uygulama ayarlarına bağlantılar.
- **İsteğe bağlı root** ile süreç bazında CPU, RAM (RSS), PID ve üreticinin termal sensörleri. CPU/RAM sıralaması; ilk 80 süreç ekranda, yakalanan tüm süreçler raporda.
- Ölçüm raporunu Android paylaşım ekranından kullanıcı seçimiyle paylaşma.

## Ölçümlerin sınırları

Modern Android, rootsuz bir uygulamanın diğer uygulamaların anlık CPU/RAM kullanımını okumasını engeller. Root yoksa bu alanlar erişilemiyor olarak gösterilir. Kullanım süresi CPU tüketimi değildir; uygulama otomatik olarak “ısınmanın suçlusu” ilan edilmez. Toplam CPU sayacı bazı rootsuz ROM'larda okunabilir, diğerlerinde root gerekir. CPU yüzdesi tüm çekirdeklerin toplam kapasitesine göre normalize edilir; ilk iki örnek arasındaki fark gerekir. Süreç RSS değerleri paylaşılan sayfaları tekrar sayabilir; PSS veya uygulama toplamı değildir. Süreç adı kernel tarafından kısaltılabilir. Root modunda da gizlenen/okunamayan süreçler bulunabilir.

Kullanım süreleri Android olay geçmişinden yaklaşık hesaplanır; çoklu pencere, üretici kayıt politikası ve eksik olaylar sonucu etkileyebilir. Arka plandaki işleri ölçmez. Termal sensör adları üreticiden gelir; hepsi CPU sensörü değildir. Sensörler Linux thermal-zone ABI'sinin milisantigrat birimiyle yorumlanır.

Uygulama telefonu rootlamaz, süreç öldürmez, otomatik “RAM temizliği” yapmaz. Yalnızca ön plandayken 5 saniyede bir örnek alır. İnternet izni, reklam, analiz SDK'sı ve arka plan servisi yoktur. Root izni sadece Süreçler sekmesindeki düğmeyle istenir; salt okunur sabit komutlar kullanılır. Paylaşılan rapor uygulama/süreç adlarını ve cihaz modelini içerebilir.

## Kurulum ve kullanım

1. Derlenen APK'yı Android telefonuna indirip aç. İstendiğinde dosyayı açtığın uygulamanın APK yükleme iznini etkinleştir.
2. Genel sekmesinde sıcaklık/RAM durumunu incele.
3. Uygulamalar sekmesinde kullanım erişimi düğmesiyle Android ayarlarından Isı Dedektifi'ne izin ver.
4. Telefon **zaten rootluysa** Süreçler → Root izni iste ve ölç. Root yöneticisindeki isteği onayla; CPU için sonraki örneği bekle.

APK debug anahtarıyla imzalanır; mağaza sürümü değildir. GitHub derlemeleri farklı geçici imza anahtarları kullanabilir; farklı derlemeye geçiş eski APK'nın kaldırılmasını gerektirebilir.

## GitHub'da derleme

Kaynakları depoya yüklediğinde `.github/workflows/android.yml` push üzerinde derler. GitHub → Actions → Android APK → başarılı çalıştırma → **Artifacts → Isi-Dedektifi-APK**. İndirilen ZIP içindeki `app-debug.apk` kurulabilir. Actions → Run workflow ile elle de başlatılabilir. İş akışı test, lint ve APK derlemesini çalıştırır. GitHub'a yükleme/çalıştırma için geçerli depo yazma yetkisi gerekir.

## Yerel derleme

JDK 21, Gradle 8.13, Android SDK Platform 35 ve Build Tools 35.0.0 gerekir.

```sh
export ANDROID_HOME=/path/to/android-sdk
bash scripts/test.sh
gradle :app:lintDebug :app:assembleDebug
```

Çıktı: `app/build/outputs/apk/debug/app-debug.apk`.

Bu bulut ortamında Java araçları için mevcut proxy ve CA'yı kullanmak gerekirse `python3 scripts/network-run.py <komut>` kullanılabilir; kimlik bilgileri depoya yazılmaz.

## Cihaz doğrulama listesi

Gerçek telefonda test edilmesi gerekir: Android 8 ve 15'te açılış; kullanım erişiminin verilmesi/geri alınması; root olmayan cihazda düzgün hata; root izninin kabul/ret/zaman aşımı; 4K/16K sayfa boyutunda RSS; iki örnek arasında CPU; arka plana geçince örneklemenin durması; büyük yazı ve ekran döndürme; rapor paylaşımı. Telefon/emülatör bağlantısı olmadan bunlar doğrulanmış sayılmaz.
