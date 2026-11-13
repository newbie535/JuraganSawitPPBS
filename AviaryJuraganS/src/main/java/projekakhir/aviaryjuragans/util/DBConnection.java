package projekakhir.aviaryjuragans.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Koneksi ke database MySQL.
 * Sesuaikan USER / PASSWORD dengan MySQL kamu (XAMPP default: root & kosong).
 */
public final class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/aviary_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DBConnection() {
    }

    /** Setiap pemanggil wajib menutup Connection (pakai try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
