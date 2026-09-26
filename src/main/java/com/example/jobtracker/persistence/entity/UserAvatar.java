package com.example.jobtracker.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_avatars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatar {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
