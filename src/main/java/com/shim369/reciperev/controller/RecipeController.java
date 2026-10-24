package com.shim369.reciperev.controller;

import com.shim369.reciperev.domain.Recipe;
import com.shim369.reciperev.dto.IngredientForm;
import com.shim369.reciperev.dto.RecipeBasicForm;
import com.shim369.reciperev.dto.RecipeCreateForm;
import com.shim369.reciperev.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SCR-01〜04(レシピ一覧・登録・詳細・基本情報編集)。
 * 更新系は PRG パターン: POST の処理後は必ず redirect して、再読み込みでの二重送信を防ぐ。
 */
@Controller
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/recipes";
    }

    @GetMapping("/recipes")
    public String list(Model model) {
        model.addAttribute("recipes", recipeService.listSummaries());
        return "recipes/list";
    }

    @GetMapping("/recipes/new")
    public String newForm(Model model) {
        RecipeCreateForm form = new RecipeCreateForm();
        form.getIngredients().add(new IngredientForm()); // 空の材料行を1つ出しておく
        model.addAttribute("form", form);
        return "recipes/new";
    }

    /**
     * @Valid で入力検証し、結果は直後の BindingResult に入る(この順番で引数を並べる決まり)。
     * エラーがあれば redirect せずに同じテンプレートを返し、入力値とエラーメッセージを表示する。
     */
    @PostMapping("/recipes")
    public String create(@Valid @ModelAttribute("form") RecipeCreateForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "recipes/new";
        }
        Recipe recipe = recipeService.create(form);
        redirect.addFlashAttribute("message", "「" + recipe.getName() + "」を登録しました(第1版・下書き)");
        return "redirect:/recipes/" + recipe.getId();
    }

    @GetMapping("/recipes/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("detail", recipeService.getDetail(id));
        return "recipes/detail";
    }

    @GetMapping("/recipes/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Recipe recipe = recipeService.getActive(id);
        RecipeBasicForm form = new RecipeBasicForm();
        form.setName(recipe.getName());
        form.setDescription(recipe.getDescription());
        model.addAttribute("recipe", recipe);
        model.addAttribute("form", form);
        return "recipes/edit";
    }

    @PostMapping("/recipes/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") RecipeBasicForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("recipe", recipeService.getActive(id));
            return "recipes/edit";
        }
        recipeService.updateBasicInfo(id, form);
        redirect.addFlashAttribute("message", "基本情報を更新しました");
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/recipes/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        Recipe recipe = recipeService.getActive(id);
        recipeService.delete(id);
        redirect.addFlashAttribute("message", "「" + recipe.getName() + "」を削除しました");
        return "redirect:/recipes";
    }
}
