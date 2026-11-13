package projekakhir.aviaryjuragans.model;

/** Satu item pesanan di dalam sebuah reservasi (untuk tombol "Detail"). */
public record DetailReservasi(String namaMenu, int jumlah, int hargaSatuan, int subtotal) {
}
