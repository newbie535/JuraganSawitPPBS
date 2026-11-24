package projekakhir.aviaryjuragans.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import projekakhir.aviaryjuragans.model.LogAktivitas;
import projekakhir.aviaryjuragans.util.DBConnection;

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
    
    public List<LogAktivitas> findRecent(int limit) throws SQLException {
        String sql = "SELECT l.waktu, COALESCE(u.username, '(dihapus)'), l.aktivitas "
                + "FROM log_aktivitas l LEFT JOIN users u ON u.id_user = l.id_user "
                + "ORDER BY l.waktu DESC, l.id_log DESC LIMIT ?";
        List<LogAktivitas> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LogAktivitas(rs.getTimestamp(1).toLocalDateTime(),
                            rs.getString(2), rs.getString(3)));
                }
            }
        }
        return list;
    }
}
