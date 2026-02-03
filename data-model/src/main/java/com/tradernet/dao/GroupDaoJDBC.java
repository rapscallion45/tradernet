package com.tradernet.dao;

import com.tradernet.entities.GroupEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of GroupDao using Spring JDBC.
 */
@Repository
public class GroupDaoJDBC implements GroupDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<GroupEntity> rowMapper = (rs, rowNum) -> {
        GroupEntity group = new GroupEntity();
        group.setId(rs.getLong("id"));
        return group;
    };

    public GroupDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(GroupEntity group) {
        jdbcTemplate.update("INSERT INTO tblGroups DEFAULT VALUES");
    }

    @Override
    public List<GroupEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM tblGroups", rowMapper);
    }

    @Override
    public Optional<GroupEntity> findById(long id) {
        return jdbcTemplate.query(
            "SELECT * FROM tblGroups WHERE id = ?",
            rowMapper,
            id
        ).stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tblGroups");
    }
}
