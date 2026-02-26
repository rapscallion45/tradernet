package com.tradernet.api.resources;

import com.tradernet.jpa.dao.GroupDao;
import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.entities.GroupEntity;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.api.resources.dto.GroupDto;
import com.tradernet.api.resources.dto.UpdateGroupRequestDto;
import com.tradernet.user.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST API for querying groups.
 */
@Path("/groups")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GroupResource {

    @Inject
    private GroupDao groupDao;

    @Inject
    private RoleDao roleDao;

    @Inject
    private UserService userService;

    @GET
    public List<GroupDto> getGroups() {
        return groupDao.findAll().stream().map(GroupDto::fromEntity).collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response getGroup(@PathParam("id") long id) {
        return groupDao.findById(id)
            .map(group -> Response.ok(GroupDto.fromEntity(group)).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{id}")
    public Response updateGroup(@PathParam("id") long id, UpdateGroupRequestDto request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        return groupDao.findById(id)
            .map(group -> {
                group.setUsers(resolveUsers(request.getUsernames()));
                group.setRoles(resolveRoles(request.getRoleNames()));
                groupDao.save(group);

                return Response.ok(GroupDto.fromEntity(group)).build();
            })
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private Set<UserEntity> resolveUsers(Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return new HashSet<>();
        }

        Set<UserEntity> users = new HashSet<>();
        for (String username : usernames) {
            UserEntity user = userService.findByUsernameWithRoles(username)
                .orElseThrow(() -> new BadRequestException("User not found: " + username));
            users.add(user);
        }
        return users;
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<RoleEntity> roles = new HashSet<>();
        for (String roleName : roleNames) {
            RoleEntity role = roleDao.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role not found: " + roleName));
            roles.add(role);
        }
        return roles;
    }
}
