package com.tradernet.dao;

import com.tradernet.entities.RoleEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of RoleDao using Spring JDBC.
 */
@Repository
public class RoleDaoJDBC implements RoleDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RoleEntity> rowMapper = (rs, rowNum) -> {
        RoleEntity role = new RoleEntity();
        role.setId(rs.getLong("id"));
        role.setName(rs.getString("name"));
        return role;
    };

    public RoleDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(RoleEntity role) {
        jdbcTemplate.update(
            "INSERT INTO tblRoles (name) VALUES (?)",
            role.getName()
        );
    }

    @Override
    public List<RoleEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM tblRoles", rowMapper);
    }

    @Override
    public Optional<RoleEntity> findByName(String name) {
        return jdbcTemplate.query(
            "SELECT * FROM tblRoles WHERE name = ?",
            rowMapper,
            name
        ).stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tblRoles");
    }
}
