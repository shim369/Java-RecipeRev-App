package com.shim369.reciperev.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

/** SCR-08 調理ログの入力。写真は MultipartFile として受け取る(PHP の $_FILES に相当) */
public class CookLogForm {

    @NotNull(message = "調理日を入力してください")
    @PastOrPresent(message = "調理日に未来の日付は指定できません")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // <input type="date"> の yyyy-MM-dd を LocalDate に変換する
    private LocalDate cookedOn = LocalDate.now();

    @NotNull(message = "評価を選んでください")
    @Min(value = 1, message = "評価は1〜5で選んでください")
    @Max(value = 5, message = "評価は1〜5で選んでください")
    private Integer rating;

    @Size(max = 1000, message = "味メモは1000文字以内で入力してください")
    private String memo;

    private MultipartFile photo;

    public LocalDate getCookedOn() {
        return cookedOn;
    }

    public void setCookedOn(LocalDate cookedOn) {
        this.cookedOn = cookedOn;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }
}
