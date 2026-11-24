/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package projekakhir.aviaryjuragans.model;

import java.time.LocalDate;
import java.time.LocalTime;
/**
 *
 * @author RafaAcer
 */
public record OrderAdmin(int id, LocalDate tanggal, LocalTime jam, String customer,
                         String nomorMeja, int jumlahTamu, int totalBayar, String status) {
}