package org.example.xiaxiang.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.example.xiaxiang.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识答题挑战模块
 * 趣味知识答题，支持分类筛选、答题评分与解析
 */
@Slf4j
@Controller
public class QuizController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/quiz")
    public String quizPage(@RequestParam(required = false) String category, Model model) {
        log.info("[QuizController] 访问知识答题页，category={}", category);

        List<AppProperties.QuizQuestion> all = appProperties.getQuizzes();
        if (all == null) {
            all = Collections.emptyList();
        }

        List<String> categories = all.stream()
                .map(AppProperties.QuizQuestion::getCategory)
                .distinct()
                .collect(Collectors.toList());

        model.addAttribute("quizzes", all);
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategory", category != null ? category : "all");
        return "quiz";
    }

    @RestController
    @RequestMapping("/api/quiz")
    public static class QuizApiController {

        @Autowired
        private QuizController quizController;

        @Autowired
        private AppProperties appProperties;

        @GetMapping
        public Result<List<AppProperties.QuizQuestion>> list(
                @RequestParam(required = false) String category,
                @RequestParam(required = false) String difficulty) {
            log.info("[QuizApiController] 获取题库列表，category={}, difficulty={}", category, difficulty);
            List<AppProperties.QuizQuestion> all = appProperties.getQuizzes();
            if (all == null) {
                return Result.success(Collections.emptyList());
            }
            List<AppProperties.QuizQuestion> filtered = new ArrayList<>(all);
            if (category != null && !category.trim().isEmpty() && !"all".equals(category)) {
                filtered = filtered.stream()
                        .filter(q -> category.equals(q.getCategory()))
                        .collect(Collectors.toList());
            }
            if (difficulty != null && !difficulty.trim().isEmpty() && !"all".equals(difficulty)) {
                filtered = filtered.stream()
                        .filter(q -> difficulty.equals(q.getDifficulty()))
                        .collect(Collectors.toList());
            }
            return Result.success(filtered);
        }

        @GetMapping("/{id}")
        public Result<AppProperties.QuizQuestion> detail(@PathVariable Integer id) {
            log.info("[QuizApiController] 获取题目详情，id={}", id);
            AppProperties.QuizQuestion q = quizController.findQuizById(id);
            if (q == null) {
                return Result.fail("题目不存在");
            }
            return Result.success(q);
        }

        @PostMapping("/submit")
        public Result<Map<String, Object>> submit(@RequestBody SubmitRequest request) {
            log.info("[QuizApiController] 提交答题，答题数={}",
                    request.getAnswers() != null ? request.getAnswers().size() : 0);

            List<AppProperties.QuizQuestion> all = appProperties.getQuizzes();
            if (all == null || request.getAnswers() == null) {
                Map<String, Object> r = new HashMap<>();
                r.put("total", 0);
                r.put("correct", 0);
                r.put("score", 0);
                r.put("details", Collections.emptyList());
                return Result.success(r);
            }

            int correct = 0;
            List<Map<String, Object>> details = new ArrayList<>();
            for (Map<Integer, Integer> entry : request.getAnswers()) {
                for (Map.Entry<Integer, Integer> e : entry.entrySet()) {
                    Integer qid = e.getKey();
                    Integer userAnswer = e.getValue();
                    AppProperties.QuizQuestion q = quizController.findQuizById(qid);
                    Map<String, Object> d = new HashMap<>();
                    d.put("questionId", qid);
                    d.put("userAnswer", userAnswer);
                    if (q != null) {
                        d.put("correctAnswer", q.getAnswer());
                        d.put("explanation", q.getExplanation());
                        boolean isCorrect = q.getAnswer().equals(userAnswer);
                        d.put("isCorrect", isCorrect);
                        if (isCorrect) {
                            correct++;
                        }
                    }
                    details.add(d);
                }
            }

            int total = request.getAnswers().size();
            int score = total == 0 ? 0 : (int) (correct * 100.0 / total);

            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("correct", correct);
            result.put("score", score);
            result.put("details", details);

            // 答题高手印章解锁条件
            if (score >= 80) {
                result.put("unlockedStamp", "答题高手");
            }

            return Result.success(result);
        }

        @GetMapping("/random")
        public Result<List<AppProperties.QuizQuestion>> random(
                @RequestParam(defaultValue = "5") int count) {
            log.info("[QuizApiController] 随机获取{}道题", count);
            List<AppProperties.QuizQuestion> all = appProperties.getQuizzes();
            if (all == null || all.isEmpty()) {
                return Result.success(Collections.emptyList());
            }
            List<AppProperties.QuizQuestion> copy = new ArrayList<>(all);
            Collections.shuffle(copy);
            return Result.success(copy.stream().limit(count).collect(Collectors.toList()));
        }
    }

    AppProperties.QuizQuestion findQuizById(Integer id) {
        if (appProperties.getQuizzes() == null) {
            return null;
        }
        return appProperties.getQuizzes().stream()
                .filter(q -> id.equals(q.getId()))
                .findFirst()
                .orElse(null);
    }

    @Data
    public static class SubmitRequest {
        private List<Map<Integer, Integer>> answers;
    }
}
