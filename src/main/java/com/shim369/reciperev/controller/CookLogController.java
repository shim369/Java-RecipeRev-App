package com.shim369.reciperev.controller;

import com.shim369.reciperev.config.Messages;
import com.shim369.reciperev.dto.CookLogForm;
import com.shim369.reciperev.exception.InvalidPhotoException;
import com.shim369.reciperev.service.CookLogService;
import com.shim369.reciperev.service.PhotoStorageService;
import com.shim369.reciperev.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SCR-08 調理ログ登録、調理ログ削除、写真の配信 */
@Controller
public class CookLogController {

    private final CookLogService cookLogService;
    private final VersionService versionService;
    private final PhotoStorageService photoStorage;
    private final Messages messages;

    public CookLogController(CookLogService cookLogService, VersionService versionService,
                             PhotoStorageService photoStorage, Messages messages) {
        this.cookLogService = cookLogService;
        this.versionService = versionService;
        this.photoStorage = photoStorage;
        this.messages = messages;
    }

    @GetMapping("/recipes/{recipeId}/versions/{versionNo}/logs/new")
    public String newForm(@PathVariable long recipeId, @PathVariable int versionNo, Model model) {
        return showForm(recipeId, versionNo, new CookLogForm(), model);
    }

    /** フォームは enctype="multipart/form-data" で送られてくる(写真を含むため) */
    @PostMapping("/recipes/{recipeId}/versions/{versionNo}/logs")
    public String create(@PathVariable long recipeId, @PathVariable int versionNo,
                         @Valid @ModelAttribute("form") CookLogForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showForm(recipeId, versionNo, form, model);
        }
        try {
            boolean startedTrial = cookLogService.register(recipeId, versionNo, form);
            redirect.addFlashAttribute("message", startedTrial
                    ? messages.get("flash.logCreatedTrial", versionNo)
                    : messages.get("flash.logCreated"));
            return "redirect:/recipes/" + recipeId + "/versions/" + versionNo;
        } catch (InvalidPhotoException e) {
            result.rejectValue("photo", e.getCode(), e.getArguments(), e.getDefaultMessage());
            return showForm(recipeId, versionNo, form, model);
        }
    }

    private String showForm(long recipeId, int versionNo, CookLogForm form, Model model) {
        model.addAttribute("detail", versionService.getDetail(recipeId, versionNo));
        model.addAttribute("form", form);
        return "logs/form";
    }

    @PostMapping("/recipes/{recipeId}/logs/{logId}/delete")
    public String delete(@PathVariable long recipeId, @PathVariable long logId, RedirectAttributes redirect) {
        int versionNo = cookLogService.delete(recipeId, logId);
        redirect.addFlashAttribute("message", messages.get("flash.logDeleted"));
        return "redirect:/recipes/" + recipeId + "/versions/" + versionNo;
    }

    /**
     * 写真を返す。uploads/ を静的ファイルとして丸ごと公開せず、コントローラを通すことで
     * ファイル名の検査(パストラバーサル対策)を必ず行う。
     */
    @GetMapping("/photos/{fileName:.+}")
    public ResponseEntity<Resource> photo(@PathVariable String fileName) {
        return photoStorage.resolve(fileName)
                .map(path -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(photoStorage.mediaTypeOf(fileName)))
                        .cacheControl(CacheControl.noCache())
                        .<Resource>body(new FileSystemResource(path)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
