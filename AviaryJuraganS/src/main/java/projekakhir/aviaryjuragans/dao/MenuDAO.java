package projekakhir.aviaryjuragans.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import projekakhir.aviaryjuragans.model.Kategori;
import projekakhir.aviaryjuragans.model.Menu;
import projekakhir.aviaryjuragans.util.DBConnection;
import projekakhir.aviaryjuragans.model.MenuAdmin;

public class MenuDAO {

    public List<Kategori> findAllKategori() throws SQLException {
        String sql = "SELECT id_kategori, nama_kategori FROM kategori_menu ORDER BY nama_kategori";
        List<Kategori> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Kategori(rs.getInt(1), rs.getString(2)));
            }
        }
        return list;
    }

    /**
     * Menu yang berstatus 'Tersedia'.
     * @param idKategori null = semua kategori
     */
    public List<Menu> findTersedia(Integer idKategori) throws SQLException {
        String sql = "SELECT m.id_menu, m.nama_menu, k.nama_kategori, m.harga "
                + "FROM menu m JOIN kategori_menu k ON k.id_kategori = m.id_kategori "
                + "WHERE m.status = 'Tersedia' "
                + (idKategori != null ? "AND m.id_kategori = ? " : "")
                + "ORDER BY k.nama_kategori, m.nama_menu";
        List<Menu> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (idKategori != null) {
                ps.setInt(1, idKategori);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Menu(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)));
                }
            }
        }
        return list;
    }
        // ===================================================================
    //  KHUSUS ADMIN (Management Menu)
    // ===================================================================
    private static final int ERR_FK = 1451;   // baris masih dipakai tabel lain

    /** Semua menu (termasuk yang Habis). keyword kosong = semua. */
    public List<MenuAdmin> findAllAdmin(String keyword) throws SQLException {
        String sql = "SELECT m.id_menu, m.id_kategori, m.nama_menu, k.nama_kategori, m.harga, m.status "
                + "FROM menu m JOIN kategori_menu k ON k.id_kategori = m.id_kategori "
                + "WHERE m.nama_menu LIKE ? ORDER BY k.nama_kategori, m.nama_menu";
        List<MenuAdmin> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MenuAdmin(rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getString(4), rs.getInt(5), rs.getString(6)));
                }
            }
        }
        return list;
    }

    public void insert(int adminId, int idKategori, String nama, int harga, String status)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO menu (id_kategori, nama_menu, harga, status) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idKategori);
                    ps.setString(2, nama);
                    ps.setInt(3, harga);
                    ps.setString(4, status);
                    ps.executeUpdate();
                }
                LogDAO.catat(c, adminId, "Menambah menu \"" + nama + "\"");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void update(int adminId, int id, int idKategori, String nama, int harga, String status)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE menu SET id_kategori = ?, nama_menu = ?, harga = ?, status = ? WHERE id_menu = ?")) {
                    ps.setInt(1, idKategori);
                    ps.setString(2, nama);
                    ps.setInt(3, harga);
                    ps.setString(4, status);
                    ps.setInt(5, id);
                    ps.executeUpdate();
                }
                LogDAO.catat(c, adminId, "Mengubah menu \"" + nama + "\"");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Menghapus menu. Jika menu sudah pernah dipesan (ada di riwayat reservasi) maka
     * tidak bisa dihapus permanen; statusnya diubah menjadi 'Habis'.
     *
     * @return true jika benar-benar terhapus, false jika hanya diubah ke 'Habis'
     */
    public boolean delete(int adminId, int id, String nama) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean terhapus = true;
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu WHERE id_menu = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    if (ex.getErrorCode() != ERR_FK) {
                        throw ex;
                    }
                    terhapus = false;
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE menu SET status = 'Habis' WHERE id_menu = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                LogDAO.catat(c, adminId, (terhapus ? "Menghapus menu \"" : "Menonaktifkan (Habis) menu \"") + nama + "\"");
                c.commit();
                return terhapus;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
