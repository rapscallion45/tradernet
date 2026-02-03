package com.tradernet.dao;

import com.tradernet.entities.UserEntity;
import com.tradernet.enums.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of UserDao using Spring JDBC.
 */
@Repository
public class UserDaoJDBC implements UserDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UserEntity> rowMapper = (rs, rowNum) -> {
        UserEntity user = new UserEntity();
        user.setPk(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setStatus(UserStatus.get(rs.getInt("type")));
        user.setAccountExpiry(toDate(rs.getTimestamp("accountExpiry")));
        user.setEmailAddress(rs.getString("emailAddress"));
        user.setPasswordNoExpire(rs.getBoolean("passwordNoExpire"));
        user.setLastLogin(toDate(rs.getTimestamp("lastLogin")));
        user.setIncorrectLoginAttempts(rs.getInt("incorrectLoginAttempts"));
        user.setBypassLockout(rs.getBoolean("bypassLockout"));
        user.setChangePasswordNextLogin(rs.getBoolean("changePasswordNextLogin"));
        user.setFullName(rs.getString("fullName"));
        user.setBypassDocumentSecurity(rs.getBoolean("bypassDocumentSecurity"));
        user.setExternalIdentity(rs.getBoolean("isExternalIdentity"));
        return user;
    };

    public UserDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(UserEntity user) {
        jdbcTemplate.update(
            "INSERT INTO tblUsers (id, username, password_hash, type, accountExpiry, emailAddress, passwordNoExpire, lastLogin, incorrectLoginAttempts, bypassLockout, changePasswordNextLogin, fullName, bypassDocumentSecurity, isExternalIdentity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            user.getPk(),
            user.getUsername(),
            user.getPasswordHash(),
            user.getStatus().getId(),
            toTimestamp(user.getAccountExpiry()),
            user.getEmailAddress(),
            user.getPasswordNoExpire(),
            toTimestamp(user.getLastLogin()),
            user.getIncorrectLoginAttempts(),
            user.isBypassLockout(),
            user.isChangePasswordNextLogin(),
            user.getFullName(),
            user.isBypassDocumentSecurity(),
            user.isExternalIdentity()
        );
    }

    @Override
    public List<UserEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM tblUsers", rowMapper);
    }

    @Override
    public Optional<UserEntity> findById(long id) {
        return jdbcTemplate.query(
            "SELECT * FROM tblUsers WHERE id = ?",
            rowMapper,
            id
        ).stream().findFirst();
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return jdbcTemplate.query(
            "SELECT * FROM tblUsers WHERE username = ?",
            rowMapper,
            username
        ).stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tblUsers");
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
