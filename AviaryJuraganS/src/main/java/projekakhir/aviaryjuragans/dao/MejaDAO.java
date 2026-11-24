package projekakhir.aviaryjuragans.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import projekakhir.aviaryjuragans.model.Meja;
import projekakhir.aviaryjuragans.util.DBConnection;
import projekakhir.aviaryjuragans.model.MejaAdmin;

public class MejaDAO {

    /** Satu reservasi dianggap memakai meja selama 2 jam. */
    public static final int DURASI_DETIK = 2 * 60 * 60;

    // Meja bentrok jika ada reservasi aktif di tanggal yang sama
    // dengan selisih jam kurang dari DURASI_DETIK.
    private static final String CEK_BENTROK =
            "NOT EXISTS (SELECT 1 FROM reservasi r "
            + "WHERE r.id_meja = m.id_meja AND r.tanggal = ? "
            + "AND r.status IN ('Menunggu','Dikonfirmasi') "
            + "AND ABS(TIME_TO_SEC(r.jam) - TIME_TO_SEC(?)) < " + DURASI_DETIK + ")";

    /** Meja aktif yang muat untuk jumlah tamu dan kosong pada tanggal & jam tersebut. */
    public List<Meja> findAvailable(LocalDate tanggal, LocalTime jam, int jumlahTamu)
            throws SQLException {
        String sql = "SELECT m.id_meja, m.nomor_meja, m.kapasitas, m.area FROM meja m "
                + "WHERE m.status = 'Aktif' AND m.kapasitas >= ? AND " + CEK_BENTROK
                + " ORDER BY m.kapasitas, m.nomor_meja";
        List<Meja> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, jumlahTamu);
            ps.setDate(2, Date.valueOf(tanggal));
            ps.setTime(3, Time.valueOf(jam));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Meja(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4)));
                }
            }
        }
        return list;
    }

    /** Dipakai ReservasiDAO untuk cek ulang di dalam transaksi. */
    static boolean isTersedia(Connection c, int idMeja, LocalDate tanggal, LocalTime jam)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM meja m WHERE m.id_meja = ? AND " + CEK_BENTROK;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMeja);
            ps.setDate(2, Date.valueOf(tanggal));
            ps.setTime(3, Time.valueOf(jam));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }
        // ===================================================================
    //  KHUSUS ADMIN (Management Meja)
    // ===================================================================
    private static final int ERR_DUPLICATE = 1062;
    private static final int ERR_FK = 1451;

    public List<MejaAdmin> findAllAdmin() throws SQLException {
        String sql = "SELECT id_meja, nomor_meja, kapasitas, area, status FROM meja "
                + "ORDER BY LENGTH(nomor_meja), nomor_meja";
        List<MejaAdmin> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MejaAdmin(rs.getInt(1), rs.getString(2), rs.getInt(3),
                        rs.getString(4), rs.getString(5)));
            }
        }
        return list;
    }

    /** @throws IllegalStateException jika nomor meja sudah ada */
    public void insert(int adminId, String nomor, int kapasitas, String area) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO meja (nomor_meja, kapasitas, area, status) VALUES (?, ?, ?, 'Aktif')")) {
                    ps.setString(1, nomor);
                    ps.setInt(2, kapasitas);
                    ps.setString(3, area);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    throw terjemahkan(ex, nomor);
                }
                LogDAO.catat(c, adminId, "Menambah meja " + nomor);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Update juga mengaktifkan kembali meja yang tadinya 'Nonaktif'. */
    public void update(int adminId, int id, String nomor, int kapasitas, String area) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE meja SET nomor_meja = ?, kapasitas = ?, area = ?, status = 'Aktif' WHERE id_meja = ?")) {
                    ps.setString(1, nomor);
                    ps.setInt(2, kapasitas);
                    ps.setString(3, area);
                    ps.setInt(4, id);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    throw terjemahkan(ex, nomor);
                }
                LogDAO.catat(c, adminId, "Mengubah meja " + nomor);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Menghapus meja. Jika meja sudah punya riwayat reservasi, tidak bisa dihapus
     * permanen; statusnya diubah menjadi 'Nonaktif' (tidak muncul di Cek Meja customer).
     *
     * @return true jika benar-benar terhapus, false jika hanya dinonaktifkan
     */
    public boolean delete(int adminId, int id, String nomor) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean terhapus = true;
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM meja WHERE id_meja = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    if (ex.getErrorCode() != ERR_FK) {
                        throw ex;
                    }
                    terhapus = false;
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE meja SET status = 'Nonaktif' WHERE id_meja = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                LogDAO.catat(c, adminId, (terhapus ? "Menghapus meja " : "Menonaktifkan meja ") + nomor);
                c.commit();
                return terhapus;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static SQLException terjemahkan(SQLException ex, String nomor) {
        if (ex.getErrorCode() == ERR_DUPLICATE) {
            throw new IllegalStateException("Nomor meja \"" + nomor + "\" sudah ada.");
        }
        return ex;
    }
}
