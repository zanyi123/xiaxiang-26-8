package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.service.ContentManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * 内容管理 Controller
 *
 * 提供后台内容动态管理功能：
 * 1. GET  /admin/content          — 内容管理主页（按专栏分组展示）
 * 2. GET  /admin/api/content/modules — 获取所有模块定义
 * 3. GET  /admin/api/content/{module}/items — 获取模块下所有单元
 * 4. POST /admin/api/content/{module}       — 新增单元
 * 5. PUT  /admin/api/content/{module}/{id}  — 编辑单元
 * 6. DELETE /admin/api/content/{module}/{id} — 删除单元
 *
 * 需要登录后访问（复用 AuthController 的 Session 认证）
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class ContentManageController {

    @Autowired
    private ContentManageService contentManageService;

    // ==================== 页面 ====================

    @GetMapping("/content")
    public String contentPage(Model model, HttpSession session) {
        // 未登录跳转登录页
        if (session.getAttribute(AuthController.SESSION_USER_KEY) == null) {
            return "redirect:/admin/login?redirect=/admin/content";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) session.getAttribute(AuthController.SESSION_USER_KEY);
        model.addAttribute("user", user);
        model.addAttribute("modules", contentManageService.getModuleDefs());
        return "admin/content";
    }

    // ==================== REST API ====================

    /**
     * 获取所有模块定义（字段结构）
     */
    @GetMapping("/api/content/modules")
    @ResponseBody
    public Result<List<ContentManageService.ModuleDef>> getModules() {
        return Result.success(contentManageService.getModuleDefs());
    }

    /**
     * 获取模块下所有单元（含素材编号和填充状态）
     */
    @GetMapping("/api/content/{module}/items")
    @ResponseBody
    public Result<List<Map<String, Object>>> getItems(@PathVariable String module) {
        try {
            List<Map<String, Object>> items = contentManageService.listItemsWithSlots(module);
            return Result.success(items);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 素材编号校对（单个模块）
     * GET /admin/api/content/{module}/verify
     */
    @GetMapping("/api/content/{module}/verify")
    @ResponseBody
    public Result<Map<String, Object>> verifySlots(@PathVariable String module) {
        try {
            return Result.success(contentManageService.verifySlots(module));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 全模块素材编号校对
     * GET /admin/api/content/verify-all
     */
    @GetMapping("/api/content/verify-all")
    @ResponseBody
    public Result<List<Map<String, Object>>> verifyAllSlots() {
        return Result.success(contentManageService.verifyAllSlots());
    }

    /**
     * 删除前检查：返回该单元绑定的素材信息
     * GET /admin/api/content/{module}/{id}/check-delete
     */
    @GetMapping("/api/content/{module}/{id}/check-delete")
    @ResponseBody
    public Result<Map<String, Object>> checkBeforeDelete(
            @PathVariable String module,
            @PathVariable int id) {
        try {
            return Result.success(contentManageService.checkBeforeDelete(module, id));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 新增单元
     */
    @PostMapping("/api/content/{module}")
    @ResponseBody
    public Result<Map<String, Object>> createItem(
            @PathVariable String module,
            @RequestBody Map<String, String> formData) {

        try {
            // 校验必填字段
            ContentManageService.ModuleDef def = findModuleDef(module);
            if (def == null) {
                return Result.fail("未知模块: " + module);
            }
            for (ContentManageService.FieldDef f : def.getFields()) {
                if (f.isRequired()) {
                    String val = formData.get(f.getName());
                    if (val == null || val.trim().isEmpty()) {
                        return Result.fail("必填字段[" + f.getLabel() + "]不能为空");
                    }
                }
            }

            Map<String, Object> result = contentManageService.createItem(module, formData);
            String slots = result.get("slotIds") != null ? result.get("slotIds").toString() : "";
            return Result.success(result, "新增成功，素材编号：" + slots);
        } catch (Exception e) {
            log.error("[ContentManage] 新增失败: {}", e.getMessage(), e);
            return Result.fail("新增失败: " + e.getMessage());
        }
    }

    /**
     * 编辑单元
     */
    @PutMapping("/api/content/{module}/{id}")
    @ResponseBody
    public Result<Map<String, Object>> updateItem(
            @PathVariable String module,
            @PathVariable int id,
            @RequestBody Map<String, String> formData) {

        try {
            Map<String, Object> result = contentManageService.updateItem(module, id, formData);
            return Result.success(result, "更新成功");
        } catch (Exception e) {
            log.error("[ContentManage] 更新失败: {}", e.getMessage(), e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除单元
     */
    @DeleteMapping("/api/content/{module}/{id}")
    @ResponseBody
    public Result<Map<String, Object>> deleteItem(
            @PathVariable String module,
            @PathVariable int id) {

        try {
            Map<String, Object> result = contentManageService.deleteItem(module, id);
            return Result.success(result, "删除成功");
        } catch (Exception e) {
            log.error("[ContentManage] 删除失败: {}", e.getMessage(), e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    // ==================== 工具 ====================

    private ContentManageService.ModuleDef findModuleDef(String moduleKey) {
        for (ContentManageService.ModuleDef d : contentManageService.getModuleDefs()) {
            if (d.getKey().equals(moduleKey)) return d;
        }
        return null;
    }
}
