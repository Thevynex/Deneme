package dev.vynex.thermal;

public class AppShellTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void rejects(String script,String why)throws Exception {
        boolean rejected=false;try{AppShell.run(new String[]{"/bin/sh","-c",script});}catch(java.io.IOException e){rejected=true;}check(rejected,why);
    }
    public static void main(String[] args)throws Exception {
        check(AppShell.run(new String[]{"/bin/sh","-c","printf 'package:com.example.app\\n'"}).equals("package:com.example.app\n"),"complete process output captured");
        rejects("exit 2","nonzero command rejected");
        rejects("printf 'Error: permission denied'","zero-exit Android error text rejected");
        rejects("printf 'java.lang.SecurityException: denied'","security exception text rejected");
        rejects("head -c 260000 /dev/zero","oversized output rejected and process cleaned up");
        System.out.println(checks+" command execution checks passed");
    }
}
