package com.tradernet.user;

import com.tradernet.user.dto.UserProfileDto;
import jakarta.ejb.Local;

import java.util.List;
import java.util.Optional;

/**
 * Read-only user profile contract for API consumers.
 */
@Local
public interface UserProfileQueryService {

    List<UserProfileDto> getUserProfiles();

    Optional<UserProfileDto> getUserProfile(long id);

    Optional<UserProfileDto> getUserProfileByUsername(String username);
}
