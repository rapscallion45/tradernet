package com.tradernet.user;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.user.dto.AuthUserDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupManagementServiceAuthorizationTest {

    @Test
    void administratorCannotGrantAllRights() {
        final GroupEntity group = group("Administrators", SecurityRoleNames.ADMIN_RIGHTS);
        final GroupManagementService service = service(group);
        final AuthUserDto administrator = user(SecurityRoleNames.ADMIN_RIGHTS);

        assertThrows(
            AuthorizationDeniedException.class,
            () -> service.updateGroup(group.getId(), Set.of(), Set.of(SecurityRoleNames.ALL_RIGHTS), administrator)
        );
    }

    @Test
    void administratorCannotModifyAnExistingAllRightsAssignment() {
        final GroupEntity group = group("Operations", SecurityRoleNames.ALL_RIGHTS);
        final GroupManagementService service = service(group);

        assertThrows(
            AuthorizationDeniedException.class,
            () -> service.updateGroup(
                group.getId(),
                Set.of(),
                Set.of(SecurityRoleNames.STANDARD_RIGHTS),
                user(SecurityRoleNames.ADMIN_RIGHTS)
            )
        );
    }

    @Test
    void allRightsUserCanManageAllRightsAssignments() {
        final GroupEntity group = group("Operations", SecurityRoleNames.ADMIN_RIGHTS);
        final GroupManagementService service = service(group);

        service.updateGroup(
            group.getId(),
            Set.of(),
            Set.of(SecurityRoleNames.ALL_RIGHTS),
            user(SecurityRoleNames.ALL_RIGHTS)
        ).orElseThrow();

        assertEquals(Set.of(SecurityRoleNames.ALL_RIGHTS), group.getRoles().stream()
            .map(RoleEntity::getName)
            .collect(Collectors.toSet()));
    }

    private GroupManagementService service(GroupEntity group) {
        return new GroupManagementService(
            new SingleGroupDao(group),
            new StubAssignmentService(),
            new NoOpAuditService()
        );
    }

    private GroupEntity group(String name, String roleName) {
        final GroupEntity group = new GroupEntity();
        group.setId(10L);
        group.setName(name);
        group.addRole(role(roleName));
        return group;
    }

    private RoleEntity role(String name) {
        final RoleEntity role = new RoleEntity();
        role.setName(name);
        return role;
    }

    private AuthUserDto user(String roleName) {
        return new AuthUserDto(1L, "operator", Set.of(roleName));
    }

    private final class StubAssignmentService extends AccessControlAssignmentService {
        @Override
        public Set<UserEntity> resolveUsers(Set<String> usernames) {
            return Set.of();
        }

        @Override
        public Set<RoleEntity> resolveRoles(Set<String> roleNames) {
            return roleNames.stream().map(GroupManagementServiceAuthorizationTest.this::role).collect(Collectors.toSet());
        }
    }

    private static final class NoOpAuditService extends AuthenticationAuditService {
        @Override
        public void record(String event, String outcome, String subject, String sourceAddress, String reason) {
        }
    }

    private static final class SingleGroupDao implements GroupDao {
        private final GroupEntity group;

        private SingleGroupDao(GroupEntity group) {
            this.group = group;
        }

        @Override
        public GroupEntity save(GroupEntity group) {
            return group;
        }

        @Override
        public List<GroupEntity> findAll() {
            return List.of(group);
        }

        @Override
        public Optional<GroupEntity> findById(long id) {
            return id == group.getId() ? Optional.of(group) : Optional.empty();
        }

        @Override
        public Optional<GroupEntity> findByIdForUpdate(long id) {
            return findById(id);
        }

        @Override
        public Optional<GroupEntity> findByName(String name) {
            return name.equals(group.getName()) ? Optional.of(group) : Optional.empty();
        }

        @Override
        public void deleteAll() {
        }
    }
}
