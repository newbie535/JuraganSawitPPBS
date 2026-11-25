/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package projekakhir.aviaryjuragans;

import projekakhir.aviaryjuragans.view.FormLogin;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
/**
 *
 * @author Asus TUF
 */
public class AviaryJuraganS {

    public static void main(String[] args) {
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Menjalankan FormLogin sebagai tampilan pertama kali
        SwingUtilities.invokeLater(() -> {
            FormLogin login = new FormLogin();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }
}

