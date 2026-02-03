package com.tradernet.dao;

import com.tradernet.entities.UserEntity;
import com.tradernet.entities.UserPropertyDefinition;
import com.tradernet.entities.UserPropertyEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JDBC implementation of UserPropertyDao using Spring JDBC.
 */
@Repository
public class UserPropertyDaoJDBC implements UserPropertyDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UserPropertyEntity> rowMapper = (rs, rowNum) -> {
        UserPropertyEntity property = new UserPropertyEntity();
        UserEntity user = new UserEntity();
        user.setPk(rs.getLong("userId"));
        property.setUser(user);
        UserPropertyDefinition definition = new UserPropertyDefinition();
        definition.setName(rs.getString("propDef"));
        property.setPropDef(definition);
        property.setValue(rs.getString("value"));
        return property;
    };

    public UserPropertyDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(UserPropertyEntity property) {
        Long userId = property.getUser() == null ? null : property.getUser().getPk();
        String propDef = property.getPropDef() == null ? null : property.getPropDef().getName();
        jdbcTemplate.update(
            "INSERT INTO tblUserProperties (userId, propDef, value) VALUES (?, ?, ?)",
            userId,
            propDef,
            property.getValue()
        );
    }

    @Override
    public List<UserPropertyEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM tblUserProperties", rowMapper);
    }

    @Override
    public List<UserPropertyEntity> findByUserId(long userId) {
        return jdbcTemplate.query(
            "SELECT * FROM tblUserProperties WHERE userId = ?",
            rowMapper,
            userId
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tblUserProperties");
    }
}
