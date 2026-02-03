package com.tradernet.dao;

import com.tradernet.databaseservice.passwords.PBKDF2PasswordHash;
import com.tradernet.entities.PasswordEntity;
import com.tradernet.entities.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * JDBC implementation of PasswordDao using Spring JDBC.
 */
@Repository
public class PasswordDaoJDBC implements PasswordDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PasswordEntity> rowMapper = (rs, rowNum) -> {
        PasswordEntity password = new PasswordEntity();
        password.setLastChanged(toDate(rs.getTimestamp("lastChanged")));
        String storedHash = rs.getString("password");
        if (storedHash != null) {
            password.setPassword(new PBKDF2PasswordHash(storedHash));
        }
        UserEntity user = new UserEntity();
        user.setPk(rs.getLong("userId"));
        password.setUser(user);
        return password;
    };

    public PasswordDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(PasswordEntity password) {
        Long userId = password.getUser() == null ? null : password.getUser().getPk();
        String hash = password.getPassword() == null ? null : password.getPassword().toString();
        Timestamp lastChanged = toTimestamp(password.getLastChanged());
        jdbcTemplate.update(
            "INSERT INTO tblPasswords (userId, password, lastChanged) VALUES (?, ?, ?)",
            userId,
            hash,
            lastChanged
        );
    }

    @Override
    public List<PasswordEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM tblPasswords", rowMapper);
    }

    @Override
    public List<PasswordEntity> findByUserId(long userId) {
        return jdbcTemplate.query(
            "SELECT * FROM tblPasswords WHERE userId = ?",
            rowMapper,
            userId
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tblPasswords");
    }

    private static Timestamp toTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }

    private static Date toDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new Date(timestamp.getTime());
    }
}
