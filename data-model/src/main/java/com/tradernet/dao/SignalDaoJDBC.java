package com.tradernet.dao;

import com.tradernet.entities.SignalEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JDBC implementation of SignalDao using Spring JDBC.
 */
@Repository
public class SignalDaoJDBC implements SignalDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SignalEntity> rowMapper = (rs, rowNum) -> {
        SignalEntity signal = new SignalEntity();
        signal.setId(rs.getLong("id"));
        signal.setSymbol(rs.getString("symbol"));
        String type = rs.getString("type");
        if (type != null) {
            signal.setType(SignalEntity.Type.valueOf(type));
        }
        signal.setConfidence(rs.getDouble("confidence"));
        return signal;
    };

    public SignalDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(SignalEntity signal) {
        jdbcTemplate.update(
            "INSERT INTO tblSignals (symbol, type, confidence) VALUES (?, ?, ?)",
            signal.getSymbol(),
            signal.getType() == null ? null : signal.getType().name(),
            signal.getConfidence()
        );
    }

    @Override
    public List<SignalEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM tblSignals", rowMapper);
    }

    @Override
    public List<SignalEntity> findBySymbol(String symbol) {
        return jdbcTemplate.query(
            "SELECT * FROM tblSignals WHERE symbol = ?",
            rowMapper,
            symbol
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tblSignals");
    }
}
