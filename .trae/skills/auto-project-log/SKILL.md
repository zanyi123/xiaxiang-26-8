---
name: "auto-project-log"
description: "Automatically retrieves conversation history and generates project logs when user requests logging. Invoke when user mentions 日志、项目日志、记录总结、开发日志 or asks to summarize solved issues into a log file."
---

# Auto Project Log

This skill automatically retrieves historical issues solved in the current conversation and generates a structured project development log file.

## When to Invoke

Trigger this skill when the user:
- Mentions "日志"、"项目日志"、"开发日志"、"记录总结"
- Asks to summarize solved problems into a file
- Requests a review of recent work history
- Wants to create a project development log

## Workflow

### Step 1: Retrieve Memory Context
1. Read `project_memory.md` from the memory folder: `c:\Users\Lenovo\.trae-cn\memory\projects\<project-path>\project_memory.md`
2. Read the latest `topics.md` from the most recent date folder under: `c:\Users\Lenovo\.trae-cn\memory\projects\<project-path>\`
3. If deeper context is needed, read `session_memory_*.jsonl` files referenced in topics.md

### Step 2: Scan Current Conversation
Review the current conversation for:
- All problems encountered and their root causes
- All fixes applied (code changes, config changes, etc.)
- Files modified with their paths
- Key decisions made
- Engineering conventions established

### Step 3: Generate Log Structure
Organize the log into the following sections:

```markdown
# <Project Name> 开发日志

> 记录时间：<current date>
> 项目路径：<project path>
> 技术栈：<detected tech stack>

---

## 一、项目架构与基础建设
（Architecture decisions, structural changes, foundational work）

## 二、功能开发
（New features implemented, with problem→solution format）

## 三、Bug修复与优化
（Each bug: problem description, root cause, fix applied, files involved）

## 四、编译与兼容性问题
（Build errors, language version compatibility issues）

## 五、项目约束与规范
（Hard constraints, engineering conventions, lessons learned）

## 六、待办事项
（Remaining TODOs identified during the session）
```

### Step 4: Write Log File
- Default output path: `<project root>/<project-name>项目日志.md`
- If a log file already exists, ask the user whether to overwrite or append
- Use clickable file links (`file:///absolute/path`) for all file references in the log
- Include code references with line numbers where applicable

### Step 5: Summary
After generating the log, provide a brief summary to the user:
- Total sections covered
- Number of issues recorded
- Number of files involved
- Link to the generated log file

## Rules
1. **Language**: Follow the user's language for the log content (Chinese if user speaks Chinese)
2. **Accuracy**: Only record issues that were actually solved or discussed, do not fabricate
3. **Completeness**: Include root cause analysis for bugs, not just the surface fix
4. **File references**: Always use clickable absolute file links
5. **No sensitive info**: Do not include passwords, API keys, or credentials in the log
6. **Concise**: Each issue should be 3-5 lines max, link to files for details
