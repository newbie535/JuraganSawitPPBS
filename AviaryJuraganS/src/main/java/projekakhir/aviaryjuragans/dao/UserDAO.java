package projekakhir.aviaryjuragans.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import projekakhir.aviaryjuragans.model.User;
import projekakhir.aviaryjuragans.util.DBConnection;

public class UserDAO {

    private static final int ERR_DUPLICATE = 1062;

    private static User baca(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id_user"), rs.getString("nama_lengkap"),
                rs.getString("no_hp"), rs.getString("username"), rs.getString("role"));
    }
    
    /** @return user, atau null jika username tidak ditemukan */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id_user, nama_lengkap, no_hp, username, role "
                + "FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? baca(rs) : null;
            }
        }
    }

    public User login(String username, String password) throws SQLException {
    String sql = "SELECT id_user, nama_lengkap, no_hp, username, role "
            + "FROM users WHERE username = ? AND password = SHA2(?, 256)";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, username);
        ps.setString(2, password);
        try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? baca(rs) : null;
            }
        }
    }
    
    public void register(String namaLengkap, String noHp, String username, String password)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users (nama_lengkap, no_hp, username, password, role) "
                        + "VALUES (?, ?, ?, SHA2(?, 256), 'customer')", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, namaLengkap);
                    ps.setString(2, noHp);
                    ps.setString(3, username);
                    ps.setString(4, password);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                } catch (SQLException ex) {
                    if (ex.getErrorCode() == ERR_DUPLICATE) {
                        throw new IllegalStateException("Username \"" + username + "\" sudah dipakai. Coba username lain.");
                    }
                    throw ex;
                }
                LogDAO.catat(c, id, "Registrasi akun baru");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
