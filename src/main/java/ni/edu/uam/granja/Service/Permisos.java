package ni.edu.uam.granja.Service;

import ni.edu.uam.granja.GranjaApplication;
import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Permisos {
    private Permisos() {}

    public static boolean esAdministrador() {
        Usuario usuario = GranjaApplication.sesion();
        return usuario != null && "ADMINISTRADOR".equals(usuario.rol());
    }

    public static boolean puedeReportes() {
        Usuario usuario = GranjaApplication.sesion();
        return usuario != null && ("ADMINISTRADOR".equals(usuario.rol()) || "ENCARGADO".equals(usuario.rol()));
    }

    public static void exigirReportes() throws SQLException {
        Usuario usuario = GranjaApplication.sesion();
        if (usuario == null) throw new IllegalStateException("Debes iniciar sesión.");
        try (Connection conexion = ConexionBaseDatos.abrir();
             PreparedStatement consulta = conexion.prepareStatement(
                 "SELECT 1 FROM usuarios WHERE id=? AND activo AND rol IN ('ADMINISTRADOR','ENCARGADO')")) {
            consulta.setLong(1, usuario.id());
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) throw new IllegalStateException("Tu cuenta no tiene permiso para consultar reportes.");
            }
        }
    }
}
