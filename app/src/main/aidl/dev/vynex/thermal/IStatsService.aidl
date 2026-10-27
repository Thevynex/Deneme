package dev.vynex.thermal;

interface IStatsService {
    String readSnapshot() = 0;
    String readDesktopSetting() = 1;
    void writeDesktopSetting(String value) = 2;
    String desktopDiagnostics() = 3;
    String listUserApps(int userId) = 4;
    boolean forceStopApp(String packageName, int userId) = 5;
    void destroy() = 16777114;
}
