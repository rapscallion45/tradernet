package com.tradernet.api.resources.dto;

import com.tradernet.jpa.entities.UserEntity;
import com.tradernet.jpa.entities.UserPropertyDefinition;
import com.tradernet.jpa.entities.UserPropertyEntity;

/**
 * Safe user-property response that avoids serializing the nested user entity.
 */
public class UserPropertyDto {

    private Long userId;
    private String username;
    private String name;
    private String value;

    public static UserPropertyDto fromEntity(UserPropertyEntity property) {
        UserPropertyDto dto = new UserPropertyDto();
        UserEntity user = property.getUser();
        if (user != null) {
            dto.setUserId(user.getPk());
            dto.setUsername(user.getUsername());
        }

        UserPropertyDefinition definition = property.getPropDef();
        dto.setName(definition == null ? null : definition.getName());
        dto.setValue(property.getValue());
        return dto;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
