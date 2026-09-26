package com.example.jobtracker.service.avatar;

public record AvatarImage(
        String contentType,
        int width,
        int height,
        byte[] thumbnail,
        String thumbnailContentType) {
}
