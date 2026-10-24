package com.shim369.reciperev.controller;

import com.shim369.reciperev.dto.RatingPoint;
import com.shim369.reciperev.repository.RecipeQueryRepository;
import com.shim369.reciperev.service.RecipeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 評価推移グラフ用の JSON API(F-10)。
 * {@code @RestController} は戻り値を(テンプレート名ではなく)JSON としてそのまま返す。
 */
@RestController
public class RatingTrendApiController {

    private final RecipeService recipeService;
    private final RecipeQueryRepository queryRepository;

    public RatingTrendApiController(RecipeService recipeService, RecipeQueryRepository queryRepository) {
        this.recipeService = recipeService;
        this.queryRepository = queryRepository;
    }

    @GetMapping("/api/recipes/{recipeId}/rating-trend")
    public List<RatingPoint> ratingTrend(@PathVariable long recipeId) {
        recipeService.getActive(recipeId); // 存在しない・削除済みなら 404
        return queryRepository.findVersionSummaries(recipeId).stream().map(RatingPoint::from).toList();
    }
}
