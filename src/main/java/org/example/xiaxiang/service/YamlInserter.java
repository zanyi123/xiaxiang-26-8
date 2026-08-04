package org.example.xiaxiang.service;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML 列表项插入/删除工具
 *
 * 用于在 application.yml 的指定列表末尾追加新条目，
 * 或删除指定索引的列表条目。
 *
 * 与 YamlUpdater 不同：
 * - YamlUpdater 只能修改已有行的值
 * - YamlInserter 可以增删整行（列表项）
 *
 * 实现方式：按行扫描，定位列表块的起始和结束位置，
 * 在末尾追加新行或删除指定行范围。
 */
@Slf4j
public class YamlInserter {

    /**
     * 在 application.yml 的指定列表末尾追加新条目
     *
     * @param itemBlock  新条目的 YAML 文本（如 "    - id: 3\n      title: \"xxx\"\n"）
     * @param listName   列表名（如 "qiaopi"）
     * @return 成功写入返回 1，否则 0
     */
    public static int appendListItem(String itemBlock, String listName) {
        File yml = locateYaml();
        if (yml == null || !yml.exists()) {
            log.warn("[YamlInsert] 找不到 application.yml，跳过写盘，内存已生效");
            return 0;
        }

        try {
            String content = readFile(yml);
            String[] lines = content.split("\n", -1);

            // 找到列表块的位置
            int listStartLine = findListStart(lines, listName);
            if (listStartLine < 0) {
                log.warn("[YamlInsert] 找不到列表: {}", listName);
                return 0;
            }

            // 找到列表基础缩进
            int baseIndent = countLeadingSpaces(lines[listStartLine]) + 2;

            // 找到列表块末尾（下一个同级或更少缩进的非空行）
            int listEndLine = findListEnd(lines, listStartLine, baseIndent);

            // 构建插入文本
            StringBuilder insertText = new StringBuilder();
            insertText.append(itemBlock);

            // 在 listEndLine 位置插入
            String[] newLines = new String[lines.length + countLines(itemBlock)];
            System.arraycopy(lines, 0, newLines, 0, listEndLine);
            String[] blockLines = itemBlock.split("\n", -1);
            for (int i = 0; i < blockLines.length; i++) {
                if (i < blockLines.length - 1 || !blockLines[i].isEmpty()) {
                    newLines[listEndLine + i] = blockLines[i];
                }
            }
            // Shift remaining lines
            int actualInserted = blockLines.length;
            if (blockLines.length > 0 && blockLines[blockLines.length - 1].isEmpty()) {
                actualInserted = blockLines.length - 1;
            }
            System.arraycopy(lines, listEndLine, newLines, listEndLine + actualInserted, lines.length - listEndLine);

            String newContent = String.join("\n", newLines);
            writeFile(yml, newContent);
            log.info("[YamlInsert] 成功追加条目到 {} ({} 行)", listName, actualInserted);
            return 1;

        } catch (Exception e) {
            log.error("[YamlInsert] 追加失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 删除 application.yml 中指定列表的第 index 个条目
     *
     * @param listName  列表名
     * @param index     要删除的条目索引（0-based）
     * @return 成功删除返回 1，否则 0
     */
    public static int removeListItem(String listName, int index) {
        File yml = locateYaml();
        if (yml == null || !yml.exists()) {
            log.warn("[YamlRemove] 找不到 application.yml，跳过写盘，内存已生效");
            return 0;
        }

        try {
            String content = readFile(yml);
            String[] lines = content.split("\n", -1);

            int listStartLine = findListStart(lines, listName);
            if (listStartLine < 0) {
                log.warn("[YamlRemove] 找不到列表: {}", listName);
                return 0;
            }

            int baseIndent = countLeadingSpaces(lines[listStartLine]) + 2;

            // 找到第 index 个列表项的起始行
            int itemStart = -1;
            int currentIndex = -1;
            for (int i = listStartLine + 1; i < lines.length; i++) {
                String ln = lines[i];
                if (isBlank(ln)) continue;
                int sp = countLeadingSpaces(ln);
                if (sp < baseIndent) break;
                String trim = stripLeading(ln);
                if (sp == baseIndent && trim.startsWith("- ")) {
                    currentIndex++;
                    if (currentIndex == index) {
                        itemStart = i;
                        break;
                    }
                } else if (sp == baseIndent && !trim.startsWith("- ")) {
                    break;
                }
            }

            if (itemStart < 0) {
                log.warn("[YamlRemove] 索引越界: {}[{}]", listName, index);
                return 0;
            }

            // 找到该列表项的结束行（下一个 "- " 同级或列表结束）
            int itemEnd = itemStart + 1;
            int itemIndent = baseIndent + 2;
            while (itemEnd < lines.length) {
                String ln = lines[itemEnd];
                if (isBlank(ln)) {
                    itemEnd++;
                    continue;
                }
                int sp = countLeadingSpaces(ln);
                String trim = stripLeading(ln);
                if (sp == baseIndent && trim.startsWith("- ")) {
                    break;
                }
                if (sp < baseIndent) {
                    break;
                }
                if (sp == baseIndent && !trim.startsWith("- ")) {
                    break;
                }
                itemEnd++;
            }

            // 删除 itemStart 到 itemEnd-1 的行
            int deleteCount = itemEnd - itemStart;
            String[] newLines = new String[lines.length - deleteCount];
            System.arraycopy(lines, 0, newLines, 0, itemStart);
            System.arraycopy(lines, itemEnd, newLines, itemStart, lines.length - itemEnd);

            String newContent = String.join("\n", newLines);
            writeFile(yml, newContent);
            log.info("[YamlRemove] 成功删除 {}[{}] ({} 行)", listName, index, deleteCount);
            return 1;

        } catch (Exception e) {
            log.error("[YamlRemove] 删除失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ================ 工具方法 ================

    private static int findListStart(String[] lines, String listName) {
        for (int i = 0; i < lines.length; i++) {
            String trim = stripLeading(lines[i]);
            if (trim.startsWith(listName + ":") || trim.equals(listName + ":")) {
                return i;
            }
        }
        return -1;
    }

    private static int findListEnd(String[] lines, int listStart, int baseIndent) {
        for (int i = listStart + 1; i < lines.length; i++) {
            String ln = lines[i];
            if (isBlank(ln)) continue;
            int sp = countLeadingSpaces(ln);
            String trim = stripLeading(ln);
            if (sp < baseIndent) {
                return i;
            }
            if (sp == baseIndent && !trim.startsWith("- ")) {
                return i;
            }
        }
        return lines.length;
    }

    private static int countLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }
        return count;
    }

    private static File locateYaml() {
        // 同 YamlUpdater 的逻辑
        try {
            Path p = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "application.yml");
            if (p.toFile().exists()) return p.toFile();
        } catch (Exception ignore) {}
        try {
            Path ext = Paths.get(System.getProperty("user.dir"), "application.yml");
            if (ext.toFile().exists()) return ext.toFile();
        } catch (Exception ignore) {}
        try {
            Path extCfg = Paths.get(System.getProperty("user.dir"), "config", "application.yml");
            if (extCfg.toFile().exists()) return extCfg.toFile();
        } catch (Exception ignore) {}
        try {
            java.net.URL url = Thread.currentThread().getContextClassLoader().getResource("");
            if (url != null) {
                File f = new File(url.toURI().getPath(), "application.yml");
                if (f.exists()) return f;
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static String readFile(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static void writeFile(File f, String content) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            bw.write(content);
        }
    }

    // ================ Java 8 兼容 ================

    private static int countLeadingSpaces(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') n++;
            else break;
        }
        return n;
    }

    private static String stripLeading(String s) {
        if (s == null) return null;
        int len = s.length();
        int st = 0;
        while (st < len && Character.isWhitespace(s.charAt(st))) st++;
        return (st > 0) ? s.substring(st) : s;
    }

    private static boolean isBlank(String s) {
        if (s == null) return true;
        return s.trim().isEmpty();
    }
}
