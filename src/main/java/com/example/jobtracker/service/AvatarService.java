package com.example.jobtracker.service;

import com.example.jobtracker.config.AvatarProperties;
import com.example.jobtracker.controller.dto.AvatarResponse;
import com.example.jobtracker.persistence.UserAvatarRepository;
import com.example.jobtracker.persistence.UserRepository;
import com.example.jobtracker.persistence.entity.UserAvatar;
import com.example.jobtracker.service.avatar.AvatarImage;
import com.example.jobtracker.service.avatar.AvatarImageProcessor;
import com.example.jobtracker.service.avatar.AvatarStorage;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarService {

    private final UserRepository userRepository;
    private final UserAvatarRepository avatarRepository;
    private final AvatarImageProcessor imageProcessor;
    private final AvatarStorage storage;
    private final AvatarProperties properties;
    private final Clock clock;

    public AvatarResponse upload(UUID userId, MultipartFile file) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + userId + " not found");
        }
        byte[] content = readBytes(file);
        AvatarImage image = imageProcessor.process(content);

        storage.put(originalKey(userId), content, image.contentType());
        storage.put(thumbnailKey(userId), image.thumbnail(), image.thumbnailContentType());

        UserAvatar avatar = new UserAvatar(
                userId,
                image.contentType(),
                content.length,
                image.width(),
                image.height(),
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS));
        avatarRepository.save(avatar);
        log.info("Uploaded avatar for user {} ({}, {} bytes)", userId, image.contentType(), content.length);
        return toResponse(avatar);
    }

    public AvatarResponse get(UUID userId) {
        return toResponse(findAvatarOrThrow(userId));
    }

    public void delete(UUID userId) {
        UserAvatar avatar = findAvatarOrThrow(userId);
        avatarRepository.delete(avatar);
        deleteFilesOfUser(userId);
        log.info("Deleted avatar of user {}", userId);
    }

    public void deleteFilesOfUser(UUID userId) {
        deleteFile(originalKey(userId));
        deleteFile(thumbnailKey(userId));
    }

    private AvatarResponse toResponse(UserAvatar avatar) {
        LocalDateTime expiresAt = LocalDateTime.now(clock).plus(properties.urlTtl()).truncatedTo(ChronoUnit.SECONDS);
        return new AvatarResponse(
                storage.createDownloadUrl(originalKey(avatar.getUserId()), properties.urlTtl()),
                storage.createDownloadUrl(thumbnailKey(avatar.getUserId()), properties.urlTtl()),
                expiresAt,
                avatar.getContentType(),
                avatar.getSizeBytes(),
                avatar.getWidth(),
                avatar.getHeight(),
                avatar.getUploadedAt());
    }

    private void deleteFile(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.warn("Could not delete avatar file {}: {}", key, e.getMessage());
        }
    }

    private UserAvatar findAvatarOrThrow(UUID userId) {
        return avatarRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + userId + " has no avatar"));
    }

    private static String originalKey(UUID userId) {
        return "avatars/" + userId + "/original";
    }

    private static String thumbnailKey(UUID userId) {
        return "avatars/" + userId + "/thumbnail";
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
    }
}
