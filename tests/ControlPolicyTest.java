import dev.vynex.thermal.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class ControlPolicyTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    interface Throwing {void run()throws Exception;}
    static void rejects(Throwing action,String why)throws Exception{boolean rejected=false;try{action.run();}catch(java.io.IOException e){rejected=true;}check(rejected,why);}
    public static void main(String[] args)throws Exception{
        check(Arrays.equals(DesktopSetting.readCommand(),new String[]{"/system/bin/settings","get","global","override_desktop_experience_features"}),"only researched global key read");
        check(Arrays.equals(DesktopSetting.writeCommand("0"),new String[]{"/system/bin/settings","put","global",DesktopSetting.KEY,"0"}),"disable is precisely zero");
        check(DesktopSetting.writeCommand("null")[1].equals("delete"),"unset backup restores absent setting");
        check(DesktopSetting.writeCommand("-1")[4].equals("-1"),"restore default override");
        check(DesktopSetting.writeCommand("1")[4].equals("1"),"restore enabled override");
        for(String invalid:new String[]{"0; reboot","", "2", "true",null})rejects(()->DesktopSetting.writeCommand(invalid),"reject unrecognized setting value");
        byte[] apk="abc".getBytes(StandardCharsets.US_ASCII);
        String hash="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        UpdatePolicy.checkHash(apk,hash);check(true,"known SHA256 vector");
        rejects(()->UpdatePolicy.checkHash("changed".getBytes(),hash),"reject tampered APK");
        rejects(()->UpdatePolicy.checkHash(apk,"not-a-hash"),"reject malformed hash");
        Set<String> keys=Set.of("signer-A");
        UpdatePolicy.verifyArchive("app",2,keys,"app",3,keys,3);check(true,"new same-signer package accepted");
        rejects(()->UpdatePolicy.verifyArchive("app",2,keys,"other",3,keys,3),"reject unrelated package");
        rejects(()->UpdatePolicy.verifyArchive("app",2,keys,"app",3,Set.of("signer-B"),3),"reject substituted signer");
        rejects(()->UpdatePolicy.verifyArchive("app",2,keys,"app",3,Set.of("signer-A","signer-B"),3),"reject additional signer");
        rejects(()->UpdatePolicy.verifyArchive("app",2,Set.of(),"app",3,Set.of(),3),"reject missing certificates");
        rejects(()->UpdatePolicy.verifyArchive("app",2,keys,"app",2,keys,2),"reject equal-version install");
        rejects(()->UpdatePolicy.verifyArchive("app",2,keys,"app",1,keys,1),"reject downgrade");
        rejects(()->UpdatePolicy.verifyArchive("app",2,keys,"app",3,keys,4),"reject stale manifest/archive pair");
        System.out.println(checks+" update/desktop policy checks passed");
    }
}
