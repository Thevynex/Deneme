import dev.vynex.thermal.AppManagementPolicy;
import java.util.*;
import java.io.IOException;

public class AppManagementPolicyTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    interface Throwing {void run()throws Exception;}
    static void rejects(Throwing run,String why)throws Exception{boolean caught=false;try{run.run();}catch(IOException e){caught=true;}check(caught,why);}
    public static void main(String[] args)throws Exception {
        Set<String> packages=AppManagementPolicy.userPackages("package:com.example.one\npackage:com.example.two\npackage:com.example.one\npackage:dev.vynex.thermal\npackage:moe.shizuku.privileged.api\npackage:--user\nError: denied\n");
        check(packages.equals(Set.of("com.example.one","com.example.two")),"only valid exact packages, excluding own app/backend");
        for(String name:new String[]{null,"", "--user", "com.x; reboot", "com.x\ncom.y", "android", "com.x/Activity", "dev.vynex.thermal", "moe.shizuku.privileged.api"})
            rejects(()->AppManagementPolicy.validatePackage(name),"reject unsafe/protected package");
        AppManagementPolicy.validateUser(0,10234);check(true,"same primary profile accepted");
        AppManagementPolicy.validateUser(10,1010234);check(true,"same work profile accepted");
        rejects(()->AppManagementPolicy.validateUser(0,1010234),"work profile cannot control primary user");
        rejects(()->AppManagementPolicy.validateUser(10,10234),"primary user cannot control work profile");
        rejects(()->AppManagementPolicy.validateUser(-1,10234),"all-users option rejected");
        rejects(()->AppManagementPolicy.validateUser(0,-1),"invalid binder caller rejected");
        check(Arrays.equals(AppManagementPolicy.listCommand(10),new String[]{"/system/bin/pm","list","packages","-3","--user","10"}),"only installed third-party apps for exact profile requested");
        check(Arrays.equals(AppManagementPolicy.stopCommand("com.example.one",0,packages),new String[]{"/system/bin/am","force-stop","--user","0","com.example.one"}),"stop targets one exact package without shell expansion");
        rejects(()->AppManagementPolicy.stopCommand("com.example.one.extra",0,packages),"prefix matches are not eligible");
        rejects(()->AppManagementPolicy.stopCommand("com.android.systemui",0,packages),"system app absent from eligible third-party set rejected");
        rejects(()->AppManagementPolicy.stopCommand("com.example.one",0,Set.of()),"uninstalled/stale selection rejected");
        String state="Package [com.example.one]\n  User 0: installed=true stopped=true enabled=0\n  User 10: installed=true stopped=false enabled=0\n";
        check(Boolean.TRUE.equals(AppManagementPolicy.stoppedForUser(state,0)),"stopped state primary profile verified");
        check(Boolean.FALSE.equals(AppManagementPolicy.stoppedForUser(state,10)),"foreign profile stopped flag not reused");
        check(AppManagementPolicy.stoppedForUser(state,1)==null,"user 1 is not user 10");
        check(AppManagementPolicy.stoppedForUser("User 0: installed=true\nUser 10: stopped=true",0)==null,"do not scan past selected user section");
        check(Boolean.TRUE.equals(AppManagementPolicy.stoppedForUser("User 0: installed=true\n   stopped=true\n",0)),"multiline state supported");
        check(AppManagementPolicy.stoppedForUser("permission denied",0)==null,"missing state cannot be reported as verified");
        System.out.println(checks+" app management policy checks passed");
    }
}
