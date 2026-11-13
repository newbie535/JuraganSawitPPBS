package projekakhir.aviaryjuragans.model;

/** Satu baris di tabel "Pesanan" (keranjang) sebelum reservasi dikonfirmasi. */
public record CartItem(Menu menu, int qty) {

    public int subtotal() {
        return menu.harga() * qty;
    }

    public CartItem tambah(int tambahan) {
        return new CartItem(menu, qty + tambahan);
    }
}
