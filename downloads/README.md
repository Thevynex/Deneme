# APK

[Isı Dedektifi 1.0 APK indir](https://github.com/Thevynex/Deneme/raw/refs/heads/main/downloads/Isi-Dedektifi.apk)

Android 8.0+. Bu dosya bulut çalışma alanında JDK 21, Gradle 8.13 ve Android SDK 35 ile derlenen debug imzalı APK'dır. GitHub Actions ayrıca aynı kaynakları derleyip çalıştırmanın Artifacts bölümüne APK yükler. Bu dosya GitHub Actions çıktısı olarak etiketlenmemiştir.

Doğrulama: 14 sayaç ayrıştırma/hesaplama kontrolü başarılı; `:app:lintDebug :app:assembleDebug` başarılı (0 lint hatası, 3 uyarı); `apksigner verify` başarılı; AndroidManifest içinde internet izni yok. Bağlı Android cihaz/emülatör olmadığı için cihaz üzerinde çalışma doğrulanmadı. Ayrıntılı diğer-uygulama CPU/RAM görünümü root gerektirir; rootsuz kullanımda sistem RAM'i, pil sıcaklığı ve izinli kullanım geçmişi görüntülenir.

Kaynak commit: `6f1c220` (sonraki commitler yalnızca CI ve dağıtım dosyalarını değiştirir).
SHA256: `16baf33008cde5160e75934483a881a21f9944a04a445127266c959539dde139`
