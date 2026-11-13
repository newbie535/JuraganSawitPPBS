package projekakhir.aviaryjuragans.model;

public record Meja(int id, String nomor, int kapasitas, String area) {

    /** Teks yang tampil di combo "Pilih Meja". */
    @Override
    public String toString() {
        return nomor + " - " + area + " (" + kapasitas + " kursi)";
    }
}
