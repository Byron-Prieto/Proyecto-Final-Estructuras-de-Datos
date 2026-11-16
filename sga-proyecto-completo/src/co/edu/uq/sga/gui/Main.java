package co.edu.uq.sga.gui;

import co.edu.uq.sga.servicio.RelojAjustable;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignorada) {
                // se usa el aspecto por defecto
            }
            Contexto ctx = new Contexto(RelojAjustable.enMarcha());
            String aviso = ctx.iniciarDesdeArchivo(); // recupera los datos de la sesión anterior
            if (aviso != null) JOptionPane.showMessageDialog(null, aviso, "Datos guardados", JOptionPane.WARNING_MESSAGE);
            new VentanaPrincipal(ctx).setVisible(true);
        });
    }
}
