package com.shim369.reciperev.controller;

import com.shim369.reciperev.config.Messages;
import com.shim369.reciperev.domain.RecipeVersion;
import com.shim369.reciperev.dto.FinalizeResult;
import com.shim369.reciperev.dto.VersionContentForm;
import com.shim369.reciperev.exception.NoChangesException;
import com.shim369.reciperev.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SCR-05〜07(版編集・版詳細・版比較)と承認フローの操作 */
@Controller
@RequestMapping("/recipes/{recipeId}")
public class VersionController {

    private final VersionService versionService;
    private final Messages messages;

    public VersionController(VersionService versionService, Messages messages) {
        this.versionService = versionService;
        this.messages = messages;
    }

    /** SCR-05: base を省略したら最新版を元にする */
    @GetMapping("/versions/new")
    public String newForm(@PathVariable long recipeId, @RequestParam(required = false) Integer base, Model model) {
        int baseNo = (base != null) ? base : versionService.latestVersionNo(recipeId);
        RecipeVersion baseVersion = versionService.getVersion(recipeId, baseNo);
        VersionContentForm form = new VersionContentForm();
        form.fill(baseVersion.toContent());
        return showForm(recipeId, baseNo, form, model);
    }

    @PostMapping("/versions")
    public String create(@PathVariable long recipeId, @RequestParam int base,
                         @Valid @ModelAttribute("form") VersionContentForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showForm(recipeId, base, form, model);
        }
        try {
            RecipeVersion created = versionService.createVersion(recipeId, base, form);
            redirect.addFlashAttribute("message", messages.get("flash.versionCreated", created.getVersionNo()));
            return "redirect:/recipes/" + recipeId + "/versions/" + created.getVersionNo();
        } catch (NoChangesException e) {
            // BR-03 は入力エラーと同じ見せ方にする(エラー画面に飛ばさず、入力内容を残したまま戻す)
            // reject(キー, 引数, 既定文言): テンプレートの #fields.globalErrors() で利用者の言語に変換される
            result.reject(e.getCode(), e.getArguments(), e.getDefaultMessage());
            return showForm(recipeId, base, form, model);
        }
    }

    private String showForm(long recipeId, int baseNo, VersionContentForm form, Model model) {
        model.addAttribute("detail", versionService.getDetail(recipeId, baseNo));
        model.addAttribute("form", form);
        return "versions/form";
    }

    @GetMapping("/versions/{versionNo}")
    public String detail(@PathVariable long recipeId, @PathVariable int versionNo, Model model) {
        model.addAttribute("detail", versionService.getDetail(recipeId, versionNo));
        return "versions/detail";
    }

    @PostMapping("/versions/{versionNo}/start-trial")
    public String startTrial(@PathVariable long recipeId, @PathVariable int versionNo, RedirectAttributes redirect) {
        versionService.startTrial(recipeId, versionNo);
        redirect.addFlashAttribute("message", messages.get("flash.trialStarted", versionNo));
        return "redirect:/recipes/" + recipeId + "/versions/" + versionNo;
    }

    @PostMapping("/versions/{versionNo}/finalize")
    public String finalizeVersion(@PathVariable long recipeId, @PathVariable int versionNo,
                                  RedirectAttributes redirect) {
        FinalizeResult result = versionService.finalizeVersion(recipeId, versionNo);
        redirect.addFlashAttribute("message", result.previousFinalNo() == null
                ? messages.get("flash.finalized", versionNo)
                : messages.get("flash.finalizedSwitch", versionNo, result.previousFinalNo()));
        return "redirect:/recipes/" + recipeId + "/versions/" + versionNo;
    }

    /** SCR-07: from/to を省略したら「最新版とその1つ前」を比べる */
    @GetMapping("/diff")
    public String diff(@PathVariable long recipeId,
                       @RequestParam(required = false) Integer from,
                       @RequestParam(required = false) Integer to,
                       Model model, RedirectAttributes redirect) {
        int latest = versionService.latestVersionNo(recipeId);
        if (from == null || to == null) {
            if (latest < 2) {
                redirect.addFlashAttribute("errorFlash", messages.get("flash.diffNeedsTwo"));
                return "redirect:/recipes/" + recipeId;
            }
            return "redirect:/recipes/" + recipeId + "/diff?from=" + (latest - 1) + "&to=" + latest;
        }
        model.addAttribute("view", versionService.compare(recipeId, from, to));
        return "versions/diff";
    }
}
