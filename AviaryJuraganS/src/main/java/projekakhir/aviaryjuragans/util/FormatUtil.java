package projekakhir.aviaryjuragans.util;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Helper format tampilan: Rupiah, tanggal, jam, kode reservasi. */
public final class FormatUtil {

    public static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter JAM = DateTimeFormatter.ofPattern("HH:mm");

    private FormatUtil() {
    }

    /** 25000 -> "Rp 25.000" */
    public static String rupiah(long nominal) {
        return "Rp " + String.format(Locale.of("id", "ID"), "%,d", nominal);
    }

    /** 7 -> "RSV-0007" */
    public static String kodeReservasi(int id) {
        return String.format("RSV-%04d", id);
    }
}
