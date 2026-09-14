package com.bi.queryer.ssm.test;

/**
 * @Author contributor
 * @Date 15:04 2025/12/3
 * @Description TODO
 **/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MaxConcurrentQueriesFromCSV {

    // 日期时间格式：2025-10-10 12:22:22
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        String csvFile = "/Users/contributor/Documents/doc/98日常运营/多维分析/压测/query_log.csv";          // CSV 文件路径
        String targetDateStr = "2025-10-10";     // 要分析的日期

        try {
            int maxConcurrent = calculateMaxConcurrentForDate(csvFile, targetDateStr);
            System.out.println("日期 " + targetDateStr + " 的最大并发查询数: " + maxConcurrent);
        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从 CSV 文件中读取日志，计算指定日期的最大并发查询数
     */
    public static int calculateMaxConcurrentForDate(String csvFile, String targetDateStr) throws IOException {
        LocalDate targetDate = LocalDate.parse(targetDateStr);

        List<QueryLog> logs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 跳过 header
                }

                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                String startStr = parts[0].trim();
                String endStr = parts[1].trim();

                LocalDateTime startTime = LocalDateTime.parse(startStr, FORMATTER);
                LocalDateTime endTime = LocalDateTime.parse(endStr, FORMATTER);

                // 只保留 startTime 在目标日期的记录（简化处理）
                if (startTime.toLocalDate().equals(targetDate)) {
                    logs.add(new QueryLog(startTime, endTime));
                }
                // 注意：若查询跨天（如 23:59 -> 次日 00:01），可根据需求扩展
            }
        }

        return calculateMaxConcurrent(logs);
    }

    /**
     * 使用扫描线算法计算最大并发数
     */
    private static int calculateMaxConcurrent(List<QueryLog> logs) {
        if (logs.isEmpty()) return 0;

        List<Event> events = new ArrayList<>(logs.size() * 2);
        for (QueryLog log : logs) {
            events.add(new Event(log.startTime, 1));   // 开始
            events.add(new Event(log.endTime, -1));    // 结束
        }

        // 排序：时间升序；时间相同时，结束事件(-1)排在开始事件(+1)前面
        events.sort((a, b) -> {
            int cmp = a.time.compareTo(b.time);
            if (cmp != 0) return cmp;
            return Integer.compare(a.delta, b.delta); // -1 < +1
        });

        int current = 0;
        int max = 0;
        for (Event e : events) {
            current += e.delta;
            if (current > max) {
                max = current;
            }
        }
        return max;
    }

    // 内部类：查询日志
    static class QueryLog {
        LocalDateTime startTime;
        LocalDateTime endTime;

        QueryLog(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    // 内部类：事件（开始/结束）
    static class Event {
        LocalDateTime time;
        int delta; // +1 表示开始，-1 表示结束

        Event(LocalDateTime time, int delta) {
            this.time = time;
            this.delta = delta;
        }
    }
}
