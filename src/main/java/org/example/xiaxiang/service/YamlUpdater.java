package org.example.xiaxiang.service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * YAML 文件热更新辅助工具
 *
 * 思路：按行扫描，匹配 yaml 路径对应行的位置，直接替换行尾的 value。
 * 不引入 SnakeYAML 的 dump（会丢失注释/顺序）。
 *
 * 路径匹配规则（举例）：
 *   yamlPath = "locations[2].imageKey"
 *   → 寻找 app: 块下的：
 *        locations:
 *          - id: 3  (注：这里 index 2，id 可能是3，我们按列表的顺序匹配，不按id)
 *            image-key: 原始值
 *   → 把 "image-key: ..." 行整行替换成新值
 *
 * 说明：YAML 中 coverImage 在文件里写的是 cover-image（kebab-case），
 *       这里通过"字段名→连字符"以及与"index对应第N个列表项"的方式定位。
 */
@Slf4j
public class YamlUpdater {

    /**
     * 批量修改 application.yml
     * @return 成功写入的条目数
     */
    public static int patchYaml(Map<String, String> yamlPathToValue) {
        File yml = locateYaml();
        if (yml == null || !yml.exists()) {
            log.warn("[YamlPatch] 找不到 application.yml，跳过写盘，内存已生效");
            return 0;
        }

        int patched = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(yml), StandardCharsets.UTF_8))) {
            StringBuilder all = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                all.append(line).append("\n");
            }
            String content = all.toString();
            for (Map.Entry<String, String> e : yamlPathToValue.entrySet()) {
                String prev = content;
                content = applyPatch(content, e.getKey(), e.getValue());
                if (!content.equals(prev)) {
                    patched++;
                }
            }

            if (patched > 0) {
                BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(yml), StandardCharsets.UTF_8));
                try {
                    bw.write(content);
                } finally {
                    bw.close();
                }
                log.info("[YamlPatch] 成功写盘 {} 项到 {}", patched, yml.getAbsolutePath());
            }
            return patched;

        } catch (Exception e) {
            log.error("[YamlPatch] 写盘失败：{}", e.getMessage(), e);
            return patched;
        }
    }

    private static File locateYaml() {
        // 1. 资源目录（开发期 IDE 运行）
        try {
            Path p = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "application.yml");
            if (p.toFile().exists()) {
                return p.toFile();
            }
        } catch (Exception ignore) {
        }
        // 2. jar 同级目录的外部配置（服务器部署用，Spring Boot 优先加载外部 yml）
        try {
            Path ext = Paths.get(System.getProperty("user.dir"), "application.yml");
            if (ext.toFile().exists()) {
                return ext.toFile();
            }
        } catch (Exception ignore) {
        }
        // 3. jar 同级 config/ 目录（Spring Boot 外部配置最高优先级位置）
        try {
            Path extCfg = Paths.get(System.getProperty("user.dir"), "config", "application.yml");
            if (extCfg.toFile().exists()) {
                return extCfg.toFile();
            }
        } catch (Exception ignore) {
        }
        // 4. classpath 同级目录（开发期 target/classes）
        try {
            java.net.URL url = Thread.currentThread().getContextClassLoader().getResource("");
            if (url != null) {
                String cp = url.toURI().getPath();
                File f = new File(cp, "application.yml");
                if (f.exists()) {
                    return f;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 核心：在整个 YAML 文本中，根据 path 找到对应行并替换 value。
     *
     * 做法：
     * 1. 解析路径：listName + index + scalarField（驼峰）
     * 2. 找到 app: 块下 listName: 所在行（如 locations:）
     * 3. 从该位置开始，按 "-" 开头的行计数，数到第(index+1)个列表项
     * 4. 在该列表项下的缩进区间内，寻找与 scalarField 匹配的 kebab-case 或 camelCase 键
     * 5. 替换 "key: oldValue" → "key: newValue"
     */
    private static String applyPatch(String content, String yamlPath, String newValue) {
        int lb = yamlPath.indexOf('[');
        int rb = yamlPath.indexOf(']');
        int dot = yamlPath.indexOf('.');
        
        // 处理简单字段路径（如 mapBackgroundImage）
        if (lb < 0 || rb < 0 || dot < 0) {
            if (yamlPath.contains(".") || yamlPath.contains("[")) {
                return content; // 格式不对，跳过
            }
            // 简单字段：找 kebab-case 版本并替换
            String kebabPath = camelToKebab(yamlPath);
            return patchSimpleField(content, kebabPath, yamlPath, newValue);
        }
        
        String listName = yamlPath.substring(0, lb);
        int index = Integer.parseInt(yamlPath.substring(lb + 1, rb));
        String fieldCamel = yamlPath.substring(dot + 1);
        String fieldKebab = camelToKebab(fieldCamel);

        String[] lines = content.split("\n", -1);

        // Step1: 找 listName: 行（app 块下）
        int listStartLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i];
            String trimmed = stripLeading(ln);
            if (trimmed.startsWith(listName + ":") || trimmed.equals(listName + ":")) {
                listStartLine = i;
                break;
            }
        }
        if (listStartLine < 0) {
            return content;
        }

        // Step2: 列表基础缩进
        int baseIndent = countLeadingSpaces(lines[listStartLine]) + 2;

        // Step3: 数列表项，定位第 index 个（首次必定命中 itemStartLine）
        int itemStartLine = -1;
        int currentIndex = -1;
        int i = listStartLine + 1;
        while (i < lines.length) {
            String ln = lines[i];
            if (isBlank(ln)) {
                i++;
                continue;
            }
            int sp = countLeadingSpaces(ln);
            if (sp < baseIndent) {
                break;
            }
            String trim = stripLeading(ln);
            if (sp == baseIndent && trim.startsWith("- ")) {
                currentIndex++;
                if (currentIndex == index) {
                    itemStartLine = i;
                    break;
                }
            } else if (sp == baseIndent && trim.length() > 0) {
                break;
            }
            i++;
        }
        if (itemStartLine < 0) {
            return content;
        }

        // Step4: 在 item 作用域内找 field
        int itemIndent = baseIndent + 2;
        int j = itemStartLine;
        int targetLine = -1;
        while (j < lines.length) {
            String ln = lines[j];
            if (isBlank(ln)) {
                j++;
                continue;
            }
            int sp = countLeadingSpaces(ln);
            String trim = stripLeading(ln);
            if (j != itemStartLine) {
                if (sp < itemIndent) {
                    break;
                }
                if (sp == baseIndent && trim.startsWith("- ")) {
                    break;
                }
            }
            if (sp >= itemIndent) {
                if (trim.startsWith(fieldKebab + ":") || trim.startsWith(fieldCamel + ":")) {
                    targetLine = j;
                    break;
                }
            } else if (j == itemStartLine) {
                String afterDash = trim.startsWith("- ") ? stripLeading(trim.substring(2)) : trim;
                if (afterDash.startsWith(fieldKebab + ":") || afterDash.startsWith(fieldCamel + ":")) {
                    targetLine = j;
                    break;
                }
            }
            j++;
        }

        if (targetLine < 0) {
            int insertAfter = itemStartLine;
            int k = itemStartLine + 1;
            while (k < lines.length) {
                String ln = lines[k];
                if (isBlank(ln)) {
                    insertAfter = k;
                    k++;
                    continue;
                }
                int sp = countLeadingSpaces(ln);
                String trim = stripLeading(ln);
                if (sp == baseIndent && trim.startsWith("- ")) {
                    break;
                }
                if (sp < itemIndent) {
                    break;
                }
                insertAfter = k;
                k++;
            }
            StringBuilder ind = new StringBuilder();
            for (int n = 0; n < itemIndent; n++) {
                ind.append(' ');
            }
            String newLine = ind.toString() + fieldKebab + ": \"" + escapeYaml(newValue) + "\"";
            return insertLine(lines, insertAfter + 1, newLine);
        }

        // 替换 value
        String targetLineContent = lines[targetLine];
        int colon = targetLineContent.indexOf(':');
        if (colon < 0) {
            return content;
        }
        String keyPart = targetLineContent.substring(0, colon + 1);
        String valStr = newValue == null ? "" : newValue;
        if (valStr.contains("#") || valStr.contains(":") || valStr.startsWith(" ")
                || valStr.endsWith(" ") || valStr.isEmpty()) {
            valStr = " \"" + escapeYaml(valStr) + "\"";
        } else {
            valStr = " " + valStr;
        }
        lines[targetLine] = keyPart + valStr;
        return String.join("\n", lines);
    }

    /**
     * 处理简单字段路径（如 mapBackgroundImage → map-background-image: "value"）
     */
    private static String patchSimpleField(String content, String kebabName, String camelName, String newValue) {
        String[] lines = content.split("\n", -1);
        
        // 查找字段所在行
        int targetLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String trim = stripLeading(lines[i]);
            if (trim.startsWith(kebabName + ":") || trim.startsWith(camelName + ":")) {
                targetLine = i;
                break;
            }
        }
        
        if (targetLine >= 0) {
            // 替换现有行的值
            String targetLineContent = lines[targetLine];
            int colon = targetLineContent.indexOf(':');
            if (colon >= 0) {
                String keyPart = targetLineContent.substring(0, colon + 1);
                String valStr = newValue == null ? "" : newValue;
                if (valStr.contains("#") || valStr.contains(":") || valStr.startsWith(" ")
                        || valStr.endsWith(" ") || valStr.isEmpty()) {
                    valStr = " \"" + escapeYaml(valStr) + "\"";
                } else {
                    valStr = " " + valStr;
                }
                lines[targetLine] = keyPart + valStr;
                return String.join("\n", lines);
            }
        }
        
        // 找不到字段，在 app: 块下插入新行
        int appLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String trim = stripLeading(lines[i]);
            if (trim.equals("app:") || trim.startsWith("app:")) {
                appLine = i;
                break;
            }
        }
        
        if (appLine >= 0) {
            // 找 app: 块的结束位置（缩进变小的行）
            int insertPos = appLine + 1;
            int appIndent = countLeadingSpaces(lines[appLine]);
            while (insertPos < lines.length) {
                if (isBlank(lines[insertPos])) {
                    insertPos++;
                    continue;
                }
                int sp = countLeadingSpaces(lines[insertPos]);
                if (sp <= appIndent) {
                    break;
                }
                insertPos++;
            }
            
            String indent = "  "; // app 块下的缩进
            String valStr = newValue == null ? "" : newValue;
            if (valStr.contains("#") || valStr.contains(":") || valStr.startsWith(" ")
                    || valStr.endsWith(" ") || valStr.isEmpty()) {
                valStr = " \"" + escapeYaml(valStr) + "\"";
            } else {
                valStr = " " + valStr;
            }
            String newLine = indent + kebabName + ":" + valStr;
            return insertLine(lines, insertPos, newLine);
        }
        
        return content; // 无法处理，返回原内容
    }

    private static String insertLine(String[] lines, int pos, String newLine) {
        String[] out = new String[lines.length + 1];
        System.arraycopy(lines, 0, out, 0, pos);
        out[pos] = newLine;
        System.arraycopy(lines, pos, out, pos + 1, lines.length - pos);
        return String.join("\n", out);
    }

    private static String escapeYaml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int countLeadingSpaces(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                n++;
            } else {
                break;
            }
        }
        return n;
    }

    private static String camelToKebab(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ============ Java 8 兼容工具方法（替代 Java 11 String.stripLeading/isBlank/repeat）============

    private static String stripLeading(String s) {
        if (s == null) {
            return null;
        }
        int len = s.length();
        int st = 0;
        while (st < len && Character.isWhitespace(s.charAt(st))) {
            st++;
        }
        return (st > 0) ? s.substring(st) : s;
    }

    private static boolean isBlank(String s) {
        if (s == null) {
            return true;
        }
        String trim = s.trim();
        return trim.isEmpty();
    }
}
