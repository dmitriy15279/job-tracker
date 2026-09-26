package com.example.jobtracker.persistence;

import com.example.jobtracker.persistence.entity.UserAvatar;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, UUID> {
}
