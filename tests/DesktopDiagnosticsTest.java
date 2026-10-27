import dev.vynex.thermal.*;
import java.io.*;
import java.util.*;

public class DesktopDiagnosticsTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static String flags(String s)throws Exception{return DesktopDiagnostics.desktopFlags(new StringReader(s));}
    public static void main(String[] args)throws Exception{
        var commands=DesktopDiagnostics.commands();
        for(var command:commands.values()) {
            boolean read=command[0].equals("/system/bin/settings") && command.length==4 && command[1].equals("get") && command[2].equals("global");
            read|=command[0].equals("/system/bin/getprop") && command.length==2;
            read|=command[0].equals("/system/bin/aflags") && command.length==2 && command[1].equals("list");
            check(read,"every diagnostic command is a fixed read-only operation");
        }
        check(Arrays.equals(commands.get(DesktopDiagnostics.PROPERTY),new String[]{"/system/bin/getprop","persist.wm.debug.desktop_experience_devopts"}),"actual AOSP runtime property");
        String line="com.android.window.flags.enable_desktop_windowing_mode enabled read-only default";
        check(flags(line+"\nother.flag secret\n").trim().equals(line),"retain flag value/permission without unrelated entries");
        check(flags("pkg.show_desktop_experience_dev_option disabled read-write").contains("read-write"),"retain final line without newline");
        check(flags("pkg.DESKTOP_WINDOWING_MODE enabled\n").contains("enabled"),"case-insensitive matching");
        check(flags("Permission denied\n").contains("bulunamadı"),"missing output not reported as feature disabled");
        String oversized="desktop_window"+"a".repeat(5000)+"\n";
        check(!flags(oversized).contains("aaaa"),"oversized flag line discarded");
        String many=(line+"x".repeat(180)+"\n").repeat(200);
        String result=flags(many);
        check(result.length()<16500 && result.contains("çıktı sınırında"),"bounded result marks truncation");
        check(flags("other\n".repeat(350000)).contains("çıktı sınırında"),"total scan budget bounded for unrelated input");
        for(String[] c:commands.values())check(Arrays.stream(c).noneMatch(s->s.contains("serial") || s.contains("android_id") || s.contains("imei")),"no unique device identifiers queried");
        System.out.println(checks+" read-only diagnostic checks passed");
    }
}
