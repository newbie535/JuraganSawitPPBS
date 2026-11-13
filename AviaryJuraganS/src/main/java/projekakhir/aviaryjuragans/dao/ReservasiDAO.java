package projekakhir.aviaryjuragans.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import projekakhir.aviaryjuragans.model.CartItem;
import projekakhir.aviaryjuragans.model.DetailReservasi;
import projekakhir.aviaryjuragans.model.Reservasi;
import projekakhir.aviaryjuragans.util.DBConnection;
import projekakhir.aviaryjuragans.util.FormatUtil;

public class ReservasiDAO {

    /**
     * Menyimpan reservasi + pesanan dalam satu transaksi.
     * Harga & total dihitung ulang dari database (bukan dari data GUI).
     *
     * @return id reservasi baru
     * @throws IllegalStateException jika melanggar aturan bisnis
     *         (meja sudah diambil orang lain, menu habis, dll). Pesannya aman ditampilkan ke user.
     */
    public int create(int idUser, int idMeja, LocalDate tanggal, LocalTime jam,
                      int jumlahTamu, List<CartItem> items) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1. Kunci baris meja supaya dua customer tidak bisa booking meja yang sama bersamaan
                int kapasitas;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT kapasitas FROM meja WHERE id_meja = ? AND status = 'Aktif' FOR UPDATE")) {
                    ps.setInt(1, idMeja);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Meja tidak tersedia.");
                        }
                        kapasitas = rs.getInt(1);
                    }
                }
                if (jumlahTamu > kapasitas) {
                    throw new IllegalStateException("Jumlah tamu melebihi kapasitas meja.");
                }

                // 2. Cek ulang bentrok jadwal
                if (!MejaDAO.isTersedia(c, idMeja, tanggal, jam)) {
                    throw new IllegalStateException(
                            "Maaf, meja tersebut baru saja dipesan orang lain pada jam itu.\n"
                            + "Silakan klik \"Cek Meja\" lagi.");
                }

                // 3. Header reservasi
                int idReservasi;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO reservasi (id_user, id_meja, tanggal, jam, jumlah_tamu, status) "
                        + "VALUES (?, ?, ?, ?, ?, 'Menunggu')", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idUser);
                    ps.setInt(2, idMeja);
                    ps.setDate(3, Date.valueOf(tanggal));
                    ps.setTime(4, Time.valueOf(jam));
                    ps.setInt(5, jumlahTamu);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        idReservasi = keys.getInt(1);
                    }
                }

                // 4. Detail pesanan (harga diambil dari tabel menu, hanya yang 'Tersedia')
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO detail_reservasi (id_reservasi, id_menu, jumlah, harga_satuan, subtotal) "
                        + "SELECT ?, id_menu, ?, harga, harga * ? FROM menu "
                        + "WHERE id_menu = ? AND status = 'Tersedia'")) {
                    for (CartItem item : items) {
                        ps.setInt(1, idReservasi);
                        ps.setInt(2, item.qty());
                        ps.setInt(3, item.qty());
                        ps.setInt(4, item.menu().id());
                        if (ps.executeUpdate() != 1) {
                            throw new IllegalStateException(
                                    "Menu \"" + item.menu().nama() + "\" sudah tidak tersedia.");
                        }
                    }
                }

                // 5. Total bayar
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservasi SET total_bayar = (SELECT COALESCE(SUM(subtotal), 0) "
                        + "FROM detail_reservasi WHERE id_reservasi = ?) WHERE id_reservasi = ?")) {
                    ps.setInt(1, idReservasi);
                    ps.setInt(2, idReservasi);
                    ps.executeUpdate();
                }

                LogDAO.catat(c, idUser, "Membuat reservasi " + FormatUtil.kodeReservasi(idReservasi));
                c.commit();
                return idReservasi;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Riwayat reservasi milik satu customer, terbaru di atas. */
    public List<Reservasi> findByUser(int idUser) throws SQLException {
        String sql = "SELECT r.id_reservasi, r.tanggal, r.jam, m.nomor_meja, "
                + "r.jumlah_tamu, r.total_bayar, r.status "
                + "FROM reservasi r JOIN meja m ON m.id_meja = r.id_meja "
                + "WHERE r.id_user = ? "
                + "ORDER BY r.tanggal DESC, r.jam DESC, r.id_reservasi DESC";
        List<Reservasi> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Reservasi(rs.getInt(1), rs.getDate(2).toLocalDate(),
                            rs.getTime(3).toLocalTime(), rs.getString(4),
                            rs.getInt(5), rs.getInt(6), rs.getString(7)));
                }
            }
        }
        return list;
    }

    public List<DetailReservasi> findDetail(int idReservasi) throws SQLException {
        String sql = "SELECT mn.nama_menu, d.jumlah, d.harga_satuan, d.subtotal "
                + "FROM detail_reservasi d JOIN menu mn ON mn.id_menu = d.id_menu "
                + "WHERE d.id_reservasi = ? ORDER BY d.id_detail";
        List<DetailReservasi> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idReservasi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DetailReservasi(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        }
        return list;
    }

    /**
     * Membatalkan reservasi milik user. Hanya bisa jika status masih
     * 'Menunggu' / 'Dikonfirmasi' dan waktunya belum lewat.
     *
     * @return true jika berhasil dibatalkan
     */
    public boolean cancel(int idReservasi, int idUser) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservasi SET status = 'Dibatalkan' "
                        + "WHERE id_reservasi = ? AND id_user = ? "
                        + "AND status IN ('Menunggu','Dikonfirmasi') "
                        + "AND TIMESTAMP(tanggal, jam) > NOW()")) {
                    ps.setInt(1, idReservasi);
                    ps.setInt(2, idUser);
                    rows = ps.executeUpdate();
                }
                if (rows > 0) {
                    LogDAO.catat(c, idUser, "Membatalkan reservasi " + FormatUtil.kodeReservasi(idReservasi));
                }
                c.commit();
                return rows > 0;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
