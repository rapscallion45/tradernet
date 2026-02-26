package com.tradernet.api.resources;

import com.tradernet.jpa.dao.RoleDao;
import com.tradernet.jpa.dao.ResourceDao;
import com.tradernet.jpa.entities.RoleEntity;
import com.tradernet.jpa.entities.ResourceEntity;
import com.tradernet.api.resources.dto.RoleDto;
import com.tradernet.api.resources.dto.UpdateRoleRequestDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST API for querying roles.
 */
@Path("/roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoleResource {

    private static final List<ResourceSeed> DEFAULT_RESOURCES = List.of(
        new ResourceSeed("Users", "users"),
        new ResourceSeed("Groups", "groups"),
        new ResourceSeed("Security Roles", "roles")
    );

    @Inject
    private RoleDao roleDao;

    @Inject
    private ResourceDao resourceDao;

    @GET
    public List<RoleDto> getRoles() {
        ensureDefaultResourcesExist();
        return roleDao.findAllWithResources().stream().map(RoleDto::fromEntity).collect(Collectors.toList());
    }

    @GET
    @Path("/{name}")
    public Response getRole(@PathParam("name") String name) {
        ensureDefaultResourcesExist();
        return roleDao.findAllWithResources().stream()
            .filter(role -> role.getName().equals(name))
            .findFirst()
            .map(role -> Response.ok(RoleDto.fromEntity(role)).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/resources")
    public List<String> getResources() {
        ensureDefaultResourcesExist();
        return resourceDao.findAll().stream().map(ResourceEntity::getName).sorted().collect(Collectors.toList());
    }

    @PUT
    @Path("/{name}")
    public Response updateRole(@PathParam("name") String name, UpdateRoleRequestDto request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        ensureDefaultResourcesExist();
        return roleDao.findAllWithResources().stream()
            .filter(role -> role.getName().equals(name))
            .findFirst()
            .map(role -> {
                role.setResources(resolveResources(request.getResourceNames()));
                roleDao.save(role);
                return Response.ok(RoleDto.fromEntity(role)).build();
            })
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private void ensureDefaultResourcesExist() {
        for (ResourceSeed resourceSeed : DEFAULT_RESOURCES) {
            if (resourceDao.findByName(resourceSeed.name).isPresent()) {
                continue;
            }
            ResourceEntity resource = new ResourceEntity();
            resource.setName(resourceSeed.name);
            resource.setPathPrefix(resourceSeed.pathPrefix);
            resourceDao.save(resource);
        }
    }

    private Set<ResourceEntity> resolveResources(Set<String> resourceNames) {
        if (resourceNames == null || resourceNames.isEmpty()) {
            return new HashSet<>();
        }

        List<ResourceEntity> allResources = new ArrayList<>(resourceDao.findAll());
        Set<ResourceEntity> resources = new HashSet<>();
        for (String resourceName : resourceNames) {
            ResourceEntity resource = allResources.stream()
                .filter(candidate -> resourceName.equals(candidate.getName()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Resource not found: " + resourceName));
            resources.add(resource);
        }
        return resources;
    }

    private static class ResourceSeed {
        private final String name;
        private final String pathPrefix;

        private ResourceSeed(String name, String pathPrefix) {
            this.name = name;
            this.pathPrefix = pathPrefix;
        }
    }
}
