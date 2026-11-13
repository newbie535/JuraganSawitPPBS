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
}
