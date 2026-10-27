package dev.vynex.thermal;

import java.io.IOException;

/** Only the documented Android 16 desktop-experience override may be changed. */
public final class DesktopSetting {
    public static final String KEY="override_desktop_experience_features";
    public static String valid(String value) throws IOException {
        if(!"null".equals(value) && !"-1".equals(value) && !"0".equals(value) && !"1".equals(value))
            throw new IOException("Bilinmeyen masaüstü ayarı değeri; değişiklik yapılmadı");
        return value;
    }
    public static String[] readCommand(){return new String[]{"/system/bin/settings","get","global",KEY};}
    public static String[] writeCommand(String value) throws IOException {
        valid(value);
        return "null".equals(value)?new String[]{"/system/bin/settings","delete","global",KEY}
            :new String[]{"/system/bin/settings","put","global",KEY,value};
    }
}
