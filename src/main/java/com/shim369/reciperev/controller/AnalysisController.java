package com.shim369.reciperev.controller;

import com.shim369.reciperev.exception.AnalysisException;
import com.shim369.reciperev.service.AnalysisService;
import com.shim369.reciperev.service.RecipeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SCR-09 傾向分析 */
@Controller
public class AnalysisController {

    private final AnalysisService analysisService;
    private final RecipeService recipeService;

    public AnalysisController(AnalysisService analysisService, RecipeService recipeService) {
        this.analysisService = analysisService;
        this.recipeService = recipeService;
    }

    @GetMapping("/recipes/{recipeId}/analysis")
    public String show(@PathVariable long recipeId, Model model) {
        model.addAttribute("recipe", recipeService.getActive(recipeId));
        model.addAttribute("result", analysisService.loadLatest(recipeId).orElse(null));
        model.addAttribute("rawJson", analysisService.loadLatestJson(recipeId).orElse(null));
        return "analysis/show";
    }

    @PostMapping("/recipes/{recipeId}/analysis")
    public String run(@PathVariable long recipeId, RedirectAttributes redirect) {
        try {
            analysisService.run(recipeId);
            redirect.addFlashAttribute("message", "傾向分析を実行しました");
        } catch (AnalysisException e) {
            // 分析の失敗は想定内のエラーなので、エラー画面ではなく同じ画面にメッセージを出す
            redirect.addFlashAttribute("errorFlash", e.getMessage());
        }
        return "redirect:/recipes/" + recipeId + "/analysis";
    }
}
