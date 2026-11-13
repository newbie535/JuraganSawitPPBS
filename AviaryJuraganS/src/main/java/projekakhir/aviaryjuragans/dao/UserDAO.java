package projekakhir.aviaryjuragans.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import projekakhir.aviaryjuragans.model.User;
import projekakhir.aviaryjuragans.util.DBConnection;

public class UserDAO {

    /** @return user, atau null jika username tidak ditemukan */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id_user, nama_lengkap, no_hp, username, role "
                + "FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new User(rs.getInt("id_user"), rs.getString("nama_lengkap"),
                        rs.getString("no_hp"), rs.getString("username"), rs.getString("role"));
            }
        }
    }
}
