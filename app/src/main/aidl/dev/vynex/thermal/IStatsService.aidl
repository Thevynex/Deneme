package dev.vynex.thermal;

interface IStatsService {
    String readSnapshot() = 0;
    String readDesktopSetting() = 1;
    void writeDesktopSetting(String value) = 2;
    void destroy() = 16777114;
}
