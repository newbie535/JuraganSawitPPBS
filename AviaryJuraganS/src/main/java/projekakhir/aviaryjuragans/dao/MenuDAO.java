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
}
