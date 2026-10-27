# Android 16 / One UI 8.5 masaüstü deneyimi

Paylaşılan ekran görüntüsü Samsung SM-A366B, Android 16, One UI 8.5 üzerinde **Enable desktop experience features** / **Enable desktop windowing on secondary displays** seçeneğini gösteriyor. Anahtar kapalı etkileşimli (gri metin) ve açık görünüyor. Ekran görüntüsü tek başına ayarın neden kilitli olduğunu açıklamaz.

AOSP Android 16 kaynakları bu **tam başlık ve açıklamayı** `DesktopExperiencePreferenceController` ile eşliyor. Bu controller'ın yazdığı global anahtar `override_desktop_experience_features`; enum `OVERRIDE_OFF=0`, `OVERRIDE_ON=1`, `OVERRIDE_UNSET=-1`. Controller değişiklikten sonra yeniden başlatma onayı gösteriyor.

Bu, geliştirici override ayarıdır; gerçek masaüstü özelliklerinin tamamı için bir kapatma anahtarı değildir. `DesktopModeFlags.isFlagTrue()` içinde masaüstü deneyimi yolunda `OVERRIDE_OFF` derleme varsayılanını döndürür. Varsayılan açık olan özellikler `0` ile açık kalabilir. `WindowManagerService.SettingsObserver` ayarı okuyup `persist.wm.debug.desktop_experience_devopts` sistem özelliğine aktarır; framework bu özelliği başlangıçta önbelleğe alır.

Kullanıcı bu Samsung cihazda ayarın `0` olduğunu ve yeniden başlatmaya rağmen anahtarın açık kaldığını bildirdi. Bu, `settings put ... 0` yönteminin bu cihazda kapatma çözümü olarak doğrulanmadığını gösterir. Samsung'un gerçek bayrak ve politika değerleri henüz alınmadı. 1.2.1 tanılaması yalnızca bunların okunabilir bölümünü toplar; `setprop`, `aflags enable/disable` veya başka ayar değişiklikleri çalıştırmaz.

Shizuku yetkili bir shell oturumunda geliştirici override ayarına karşılık gelen komutlar:

```sh
# Önce oku ve değeri kaydet:
settings get global override_desktop_experience_features
# Açma zorlamasını kaldır (varsayılan açık özellikler açık kalabilir):
settings put global override_desktop_experience_features 0
# Geri okuyarak doğrula:
settings get global override_desktop_experience_features
```

Önceki değer `1`, `0` veya `-1` ise geri yükleme aynı değeri `settings put global ...` ile yazar. Önceki sonuç `null` ise anahtar önceden atanmamıştır; geri yükleme `settings delete global override_desktop_experience_features` kullanır. Bilinmeyen değerlerde uygulama değişiklik yapmaz. Uygulama bu sabit komutları Shizuku UserService içinde, shell string birleştirmeden `ProcessBuilder` argümanlarıyla çalıştırır. Genel amaçlı komut çalıştırma arayüzü açılmaz.

Samsung bu AOSP ayarını değiştirmiş veya üretici politikasıyla geçersiz kılmış olabilir. `0` geri okunması yalnızca ayar veritabanını doğrular. Yeniden başlatma sonrası görünür seçenek/harici ekran davranışı telefonda sınanmalıdır. Gri anahtarın mutlaka düzenlenebilir olacağını veya telefonun daha az ısınacağını iddia etmiyoruz. Eski `force_desktop_mode_on_external_displays` ayarı farklı controller'a aittir; bu işlem onu, DeX ayarlarını veya freeform/çoklu pencere seçeneklerini değiştirmez.

Kaynaklar:

- [Android 16 DesktopExperiencePreferenceController](https://github.com/aosp-mirror/platform_packages_apps_settings/blob/android16-release/src/com/android/settings/development/desktopexperience/DesktopExperiencePreferenceController.java)
- [Başlık ve açıklama dizeleri](https://github.com/aosp-mirror/platform_packages_apps_settings/blob/android16-release/res/values/strings.xml)
- [Settings.Global anahtar tanımı](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/provider/Settings.java)
- [DesktopModeFlags.ToggleOverride değerleri](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/window/DesktopModeFlags.java)
- [Shizuku izin ve UserService belgeleri](https://github.com/RikkaApps/Shizuku-API)

Ek kaynaklar:

- [WMS ayar gözlemcisi ve sistem özelliği aktarımı](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/services/core/java/com/android/server/wm/WindowManagerService.java)
- [aflags list çıktısı: değer, izin ve varsayılan kaynağı](https://github.com/aosp-mirror/platform_build/blob/android16-release/tools/aconfig/aflags/src/main.rs)
