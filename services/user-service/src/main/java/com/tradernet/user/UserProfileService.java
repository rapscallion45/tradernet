package com.tradernet.user;

import com.tradernet.jpa.dao.UserDao;
import com.tradernet.user.dto.UserProfileDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps persisted users to read-only profile contracts.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class UserProfileService implements UserProfileQueryService {

    @EJB
    private UserDao userDao;

    @Override
    public List<UserProfileDto> getUserProfiles() {
        return userDao.findAllWithRoles().stream()
            .map(UserDtoMapper::toUserProfile)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<UserProfileDto> getUserProfile(long id) {
        return userDao.findByIdWithRoles(id).map(UserDtoMapper::toUserProfile);
    }

    @Override
    public Optional<UserProfileDto> getUserProfileByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userDao.findByUsernameWithRoles(username).map(UserDtoMapper::toUserProfile);
    }
}
