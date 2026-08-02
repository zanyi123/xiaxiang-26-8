package org.example.xiaxiang.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.example.xiaxiang.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老照片对比互动模块
 * 同一地点百年前后照片滑动对比，展示时代变迁
 */
@Slf4j
@Controller
public class PhotoController {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CosService cosService;

    @GetMapping("/photo-compare")
    public String photoComparePage(Model model) {
        log.info("[PhotoController] 访问老照片对比页");
        List<AppProperties.PhotoCompare> photos = appProperties.getPhotoCompares();
        model.addAttribute("photos", photos);

        // 构建老照片与新照片的 URL Map（key = photo.id + "-old" / "-new"）
        Map<String, String> oldImageUrls = new HashMap<>();
        Map<String, String> newImageUrls = new HashMap<>();
        if (photos != null) {
            for (AppProperties.PhotoCompare p : photos) {
                String oldUrl = cosService.getUrlSafely(p.getOldImageKey());
                String newUrl = cosService.getUrlSafely(p.getNewImageKey());
                if (oldUrl != null) oldImageUrls.put(String.valueOf(p.getId()), oldUrl);
                if (newUrl != null) newImageUrls.put(String.valueOf(p.getId()), newUrl);
            }
        }
        model.addAttribute("oldImageUrls", oldImageUrls);
        model.addAttribute("newImageUrls", newImageUrls);
        return "photo-compare";
    }

    @RestController
    @RequestMapping("/api/photo-compare")
    public static class PhotoApiController {

        @Autowired
        private PhotoController photoController;

        @GetMapping
        public Result<List<AppProperties.PhotoCompare>> list() {
            log.info("[PhotoApiController] 获取老照片对比列表");
            List<AppProperties.PhotoCompare> photos = appProperties.getPhotoCompares();
            if (photos == null) {
                return Result.success(Collections.emptyList());
            }
            return Result.success(photos);
        }

        @GetMapping("/{id}")
        public Result<AppProperties.PhotoCompare> detail(@PathVariable Integer id) {
            log.info("[PhotoApiController] 获取老照片对比详情，id={}", id);
            AppProperties.PhotoCompare photo = photoController.findPhotoById(id);
            if (photo == null) {
                return Result.fail("照片不存在");
            }
            return Result.success(photo);
        }

        @Autowired
        private AppProperties appProperties;
    }

    AppProperties.PhotoCompare findPhotoById(Integer id) {
        if (appProperties.getPhotoCompares() == null) {
            return null;
        }
        return appProperties.getPhotoCompares().stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst()
                .orElse(null);
    }
}
