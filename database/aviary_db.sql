-- =====================================================================
--  AVIARY CAFE - RESERVATION
--  Database untuk CustomerMainFrame (dan siap dipakai AdminMainFrame)
--  Jalankan di phpMyAdmin (XAMPP) -> tab SQL, atau lewat mysql CLI.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS aviary_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE aviary_db;

-- Hapus tabel lama (urutan: anak dulu, baru induk)
DROP TABLE IF EXISTS log_aktivitas;
DROP TABLE IF EXISTS detail_reservasi;
DROP TABLE IF EXISTS reservasi;
DROP TABLE IF EXISTS menu;
DROP TABLE IF EXISTS kategori_menu;
DROP TABLE IF EXISTS meja;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
-- 1. USERS  (dipakai Login, Register, dan identitas customer)
--    password disimpan sebagai hash SHA-256 (64 karakter hex)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id_user      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nama_lengkap VARCHAR(100) NOT NULL,
    no_hp        VARCHAR(20)  NOT NULL,
    username     VARCHAR(50)  NOT NULL,
    password     CHAR(64)     NOT NULL,
    role         ENUM('admin','customer') NOT NULL DEFAULT 'customer',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_user),
    UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. KATEGORI MENU  (combo "Kategori" di customer & admin)
-- ---------------------------------------------------------------------
CREATE TABLE kategori_menu (
    id_kategori   INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nama_kategori VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id_kategori),
    UNIQUE KEY uq_kategori_nama (nama_kategori)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. MENU  (tabel "Daftar Menu")
-- ---------------------------------------------------------------------
CREATE TABLE menu (
    id_menu     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_kategori INT UNSIGNED NOT NULL,
    nama_menu   VARCHAR(100) NOT NULL,
    harga       INT UNSIGNED NOT NULL,                 -- Rupiah, tanpa desimal
    status      ENUM('Tersedia','Habis') NOT NULL DEFAULT 'Tersedia',
    PRIMARY KEY (id_menu),
    KEY idx_menu_kategori (id_kategori),
    CONSTRAINT fk_menu_kategori FOREIGN KEY (id_kategori)
        REFERENCES kategori_menu (id_kategori) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. MEJA  (combo "Pilih Meja" / tombol "Cek Meja")
-- ---------------------------------------------------------------------
CREATE TABLE meja (
    id_meja    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nomor_meja VARCHAR(10)  NOT NULL,
    kapasitas  TINYINT UNSIGNED NOT NULL,
    area       VARCHAR(30)  NOT NULL,                  -- Indoor / Outdoor / VIP
    status     ENUM('Aktif','Nonaktif') NOT NULL DEFAULT 'Aktif',
    PRIMARY KEY (id_meja),
    UNIQUE KEY uq_meja_nomor (nomor_meja)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. RESERVASI  (header: tanggal, jam, meja, jumlah tamu, total bayar)
--    Status:
--      Menunggu     = baru dibuat customer, belum diproses admin
--      Dikonfirmasi = sudah di-approve admin
--      Selesai      = customer sudah datang & bayar
--      Dibatalkan   = dibatalkan customer / admin
-- ---------------------------------------------------------------------
CREATE TABLE reservasi (
    id_reservasi INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_user      INT UNSIGNED NOT NULL,
    id_meja      INT UNSIGNED NOT NULL,
    tanggal      DATE         NOT NULL,
    jam          TIME         NOT NULL,
    jumlah_tamu  TINYINT UNSIGNED NOT NULL,
    total_bayar  INT UNSIGNED NOT NULL DEFAULT 0,
    status       ENUM('Menunggu','Dikonfirmasi','Selesai','Dibatalkan')
                 NOT NULL DEFAULT 'Menunggu',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_reservasi),
    KEY idx_reservasi_user (id_user),
    KEY idx_reservasi_meja_waktu (id_meja, tanggal, jam),
    CONSTRAINT fk_reservasi_user FOREIGN KEY (id_user)
        REFERENCES users (id_user) ON UPDATE CASCADE,
    CONSTRAINT fk_reservasi_meja FOREIGN KEY (id_meja)
        REFERENCES meja (id_meja) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. DETAIL RESERVASI  (isi tabel "Pesanan" / keranjang)
--    harga_satuan disalin saat order, jadi kalau harga menu berubah
--    di kemudian hari, riwayat lama tidak ikut berubah.
-- ---------------------------------------------------------------------
CREATE TABLE detail_reservasi (
    id_detail    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_reservasi INT UNSIGNED NOT NULL,
    id_menu      INT UNSIGNED NOT NULL,
    jumlah       SMALLINT UNSIGNED NOT NULL,
    harga_satuan INT UNSIGNED NOT NULL,
    subtotal     INT UNSIGNED NOT NULL,
    PRIMARY KEY (id_detail),
    KEY idx_detail_reservasi (id_reservasi),
    CONSTRAINT fk_detail_reservasi FOREIGN KEY (id_reservasi)
        REFERENCES reservasi (id_reservasi) ON DELETE CASCADE,
    CONSTRAINT fk_detail_menu FOREIGN KEY (id_menu)
        REFERENCES menu (id_menu) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. LOG AKTIVITAS  (untuk tab "Log Aktivitas" di admin)
-- ---------------------------------------------------------------------
CREATE TABLE log_aktivitas (
    id_log    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_user   INT UNSIGNED NULL,
    aktivitas VARCHAR(255) NOT NULL,
    waktu     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_log),
    KEY idx_log_user (id_user),
    CONSTRAINT fk_log_user FOREIGN KEY (id_user)
        REFERENCES users (id_user) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =====================================================================
--  DATA AWAL (SAMPLE)
-- =====================================================================

-- Akun uji.   admin / admin123     customer1 / customer123
INSERT INTO users (nama_lengkap, no_hp, username, password, role) VALUES
    ('Administrator',  '081200000001', 'admin',     SHA2('admin123', 256),    'admin'),
    ('Customer Demo',  '081200000002', 'customer1', SHA2('customer123', 256), 'customer');

INSERT INTO kategori_menu (nama_kategori) VALUES
    ('Makanan'), ('Minuman'), ('Snack'), ('Dessert');

INSERT INTO menu (id_kategori, nama_menu, harga, status) VALUES
    (1, 'Nasi Goreng Aviary',   32000, 'Tersedia'),
    (1, 'Spaghetti Carbonara',  38000, 'Tersedia'),
    (1, 'Chicken Katsu Rice',   35000, 'Tersedia'),
    (1, 'Beef Burger',          42000, 'Tersedia'),
    (2, 'Es Kopi Susu',         22000, 'Tersedia'),
    (2, 'Cappuccino',           25000, 'Tersedia'),
    (2, 'Lemon Tea',            18000, 'Tersedia'),
    (2, 'Matcha Latte',         27000, 'Habis'),
    (3, 'French Fries',         20000, 'Tersedia'),
    (3, 'Chicken Wings',        28000, 'Tersedia'),
    (4, 'Chocolate Lava Cake',  30000, 'Tersedia'),
    (4, 'Pancake Maple',        26000, 'Tersedia');

INSERT INTO meja (nomor_meja, kapasitas, area, status) VALUES
    ('M01', 2, 'Indoor',  'Aktif'),
    ('M02', 2, 'Indoor',  'Aktif'),
    ('M03', 4, 'Indoor',  'Aktif'),
    ('M04', 4, 'Indoor',  'Aktif'),
    ('M05', 4, 'Outdoor', 'Aktif'),
    ('M06', 6, 'Outdoor', 'Aktif'),
    ('M07', 8, 'VIP',     'Aktif'),
    ('M08', 10,'VIP',     'Aktif');
