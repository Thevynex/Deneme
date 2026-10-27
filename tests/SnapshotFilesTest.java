import dev.vynex.thermal.*;
import java.nio.file.*;
import java.util.Comparator;

public class SnapshotFilesTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args) throws Exception {
        Path temp=Files.createTempDirectory("snapshot-fixture");
        try {
            Path proc=Files.createDirectories(temp.resolve("proc")),thermal=Files.createDirectories(temp.resolve("thermal"));
            Files.writeString(proc.resolve("stat"),"cpu 100 0 0 900 0 0 0 0\ncpu0 50 0 0 450\n");
            Path pid=Files.createDirectories(proc.resolve("12"));
            Files.writeString(pid.resolve("stat"),ProcSnapshotTest.proc(12,"sample",20,100,4)+"\n");
            Files.createDirectories(proc.resolve("999")); // process exited between listing and reading
            Files.createDirectories(proc.resolve("not-a-pid"));
            Path zone=Files.createDirectories(thermal.resolve("thermal_zone0"));
            Files.writeString(zone.resolve("type"),"cpu\n");Files.writeString(zone.resolve("temp"),"42000\n");
            var snapshot=ProcSnapshot.parse(SnapshotFiles.read(proc.toFile(),thermal.toFile(),16384));
            check(snapshot.total==1000,"global CPU line only");
            check(snapshot.entries.size()==1,"ignore vanished/non-numeric processes");
            check(snapshot.entries.get(12).rssBytes()==65536,"16K page size crosses IPC format");
            check(snapshot.temperatures.size()==1,"thermal sensor read");
            Files.delete(proc.resolve("stat"));
            snapshot=ProcSnapshot.parse(SnapshotFiles.read(proc.toFile(),thermal.toFile(),4096));
            check(snapshot.total==-1 && snapshot.entries.size()==1,"partial shell access keeps RAM without inventing CPU");
            check(snapshot.rowsSince(snapshot).get(0).cpu()==null,"CPU unavailable when global counter denied");
            for(int i=1000;i<1600;i++) {
                Path d=Files.createDirectories(proc.resolve(String.valueOf(i)));
                Files.writeString(d.resolve("stat"),ProcSnapshotTest.proc(i,"x".repeat(500),1,10,2));
            }
            String bounded=SnapshotFiles.read(proc.toFile(),thermal.toFile(),4096);
            check(bounded.length()<256100,"bounded Binder string payload");
            check(ProcSnapshot.parse(bounded).truncated,"partial process list explicitly marked");
            check(!ProcSnapshot.parse("cpu 1 0 0 1").truncated,"normal list not marked partial");
            String empty=SnapshotFiles.read(temp.resolve("missing").toFile(),temp.resolve("missing").toFile(),4096);
            check(ProcSnapshot.parse(empty).total==-1 && ProcSnapshot.parse(empty).entries.isEmpty(),"denied paths yield unavailable data");
            boolean invalid=false;try{SnapshotFiles.read(proc.toFile(),thermal.toFile(),0);}catch(java.io.IOException e){invalid=true;}
            check(invalid,"reject invalid memory page size");
            System.out.println(checks+" file snapshot checks passed");
        } finally {
            try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
        }
    }
}
