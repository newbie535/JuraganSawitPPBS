package projekakhir.aviaryjuragans.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class LogDAO {

    /** Mencatat aktivitas memakai koneksi yang sama (ikut transaksi pemanggil). */
    static void catat(Connection c, int idUser, String aktivitas) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO log_aktivitas (id_user, aktivitas) VALUES (?, ?)")) {
            ps.setInt(1, idUser);
            ps.setString(2, aktivitas);
            ps.executeUpdate();
        }
    }
}
