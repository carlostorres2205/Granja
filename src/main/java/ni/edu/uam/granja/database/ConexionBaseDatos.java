package ni.edu.uam.granja.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import ni.edu.uam.granja.GranjaApplication;
import ni.edu.uam.granja.model.Usuario;

public final class ConexionBaseDatos {
    private ConexionBaseDatos() {}
    public static Connection abrir() throws SQLException {
        String url = System.getenv().getOrDefault("GRANJA_DB_URL", "jdbc:postgresql://localhost:5432/Granja");
        String usuario = System.getenv().getOrDefault("GRANJA_DB_USER", "postgres");
        String contrasena = System.getenv("GRANJA_DB_PASSWORD");
        if (contrasena == null || contrasena.isBlank())
            throw new SQLException("Configura GRANJA_DB_PASSWORD con la contraseña de PostgreSQL.");
        Connection conexion = DriverManager.getConnection(url, usuario, contrasena);
        try {
            Usuario sesion = GranjaApplication.sesion();
            if (sesion != null) {
                try (PreparedStatement permiso = conexion.prepareStatement(
                        "SELECT 1 FROM usuarios WHERE id=? AND activo AND rol=?")) {
                    permiso.setLong(1, sesion.id());
                    permiso.setString(2, sesion.rol());
                    try (ResultSet resultado = permiso.executeQuery()) {
                        if (!resultado.next())
                            throw new SQLException("Tu cuenta fue desactivada o cambió de rol. Cierra sesión y vuelve a entrar.");
                    }
                }
                try (PreparedStatement contexto = conexion.prepareStatement("SELECT set_config('granja.actor_id', ?, false)")) {
                    contexto.setString(1, Long.toString(sesion.id()));
                    contexto.execute();
                }
            }
            return conexion;
        } catch (SQLException | RuntimeException error) {
            try { conexion.close(); } catch (SQLException cierre) { error.addSuppressed(cierre); }
            throw error;
        }
    }
}
