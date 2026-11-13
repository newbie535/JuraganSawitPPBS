package projekakhir.aviaryjuragans.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Satu baris di tabel "Riwayat Reservasi". */
public record Reservasi(int id, LocalDate tanggal, LocalTime jam, String nomorMeja,
                        int jumlahTamu, int totalBayar, String status) {
}
