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

    @NotNull(message = "{validation.cookedOn.required}")
    @PastOrPresent(message = "{validation.cookedOn.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // <input type="date"> の yyyy-MM-dd を LocalDate に変換する
    private LocalDate cookedOn = LocalDate.now();

    @NotNull(message = "{validation.rating.required}")
    @Min(value = 1, message = "{validation.rating.range}")
    @Max(value = 5, message = "{validation.rating.range}")
    private Integer rating;

    @Size(max = 1000, message = "{validation.memo.size}")
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
