import dev.vynex.thermal.ProcSnapshot;

public class ProcSnapshotTest {
    private static int checks;
    static void check(boolean b,String reason) { checks++; if(!b)throw new AssertionError(reason); }
    static String proc(int pid,String name,long ticks,long start,long rss) {
        String[] fields=new String[22]; java.util.Arrays.fill(fields,"0");
        fields[0]="S";fields[11]=String.valueOf(ticks);fields[12]="0";fields[19]=String.valueOf(start);fields[21]=String.valueOf(rss);
        return pid+" ("+name+") "+String.join(" ",fields);
    }
    public static void main(String[] args) {
        var a=ProcSnapshot.parse("cpu 100 0 0 900 0 0 0 0 99 0\n"+proc(1,"app (worker)",10,100,4)+"\nPAGE|16384");
        var b=ProcSnapshot.parse("cpu 150 0 0 950 0 0 0 0 120 0\n"+proc(1,"app (worker)",30,100,8)+"\nPAGE|16384\n__THERMAL__\ncpu-0|45000\nbad|oops\ninvalid|999999");
        check(a.total==1000,"exclude guest from CPU total");
        check(b.cpuSince(a)==50.0,"system CPU delta");
        check(b.rowsSince(a).get(0).cpu()==20.0,"process CPU delta all cores");
        check(b.entries.get(1).name().equals("app (worker)"),"parenthesized process names");
        check(b.entries.get(1).rssBytes()==131072,"16k page-size RSS");
        check(b.temperatures.size()==1 && b.temperatures.get(0).contains("45.0"),"thermal ABI and malformed sensors");
        check(a.cpuSince(null)==null,"no fabricated first CPU");
        check(a.rowsSince(null).get(0).cpu()==null,"no fabricated first process CPU");
        var reused=ProcSnapshot.parse("cpu 200 0 0 1000\n"+proc(1,"replacement",40,200,3));
        check(reused.rowsSince(b).get(0).cpu()==null,"PID reuse invalidates process delta");
        check(a.cpuSince(b)==null,"counter resets invalidated");
        check(b.cpuSince(b)==null,"zero time invalidated");
        check(ProcSnapshot.parse("permission denied\n1 (x) S\ncpu x y z").entries.isEmpty(),"malformed process ignored");
        check(ProcSnapshot.parse(proc(4,"negative",0,1,-1)).entries.get(4).rssBytes()==0,"negative RSS clamped");
        check(ProcSnapshot.parse("cpu x y z 0").total==-1,"invalid CPU is unavailable, not zero");
        System.out.println(checks+" checks passed");
    }
}
