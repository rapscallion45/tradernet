package com.tradernet.user;

import com.tradernet.user.dto.AuthUserDto;
import com.tradernet.user.dto.GroupDto;
import jakarta.ejb.Local;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Contract for group administration workflows.
 */
@Local
public interface GroupManagementOperations {

    List<GroupDto> getGroups();

    Optional<GroupDto> getGroup(long id);

    Optional<GroupDto> updateGroup(long id, Set<String> usernames, Set<String> roleNames, AuthUserDto actor);
}
