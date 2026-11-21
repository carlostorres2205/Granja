package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.GranjaApplication;
import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaRepository {
    public record Cambio(OffsetDateTime fecha, String usuario, String tabla, String registro, String operacion) {}

    public List<Cambio> recientes() throws SQLException {
        Usuario actor = GranjaApplication.sesion();
        if (actor == null || !"ADMINISTRADOR".equals(actor.rol()))
            throw new IllegalStateException("Solo un administrador puede consultar el historial.");
        try (Connection conexion = ConexionBaseDatos.abrir()) {
            try (PreparedStatement permiso = conexion.prepareStatement(
                    "SELECT 1 FROM usuarios WHERE id=? AND rol='ADMINISTRADOR' AND activo")) {
                permiso.setLong(1, actor.id());
                try (ResultSet resultado = permiso.executeQuery()) {
                    if (!resultado.next()) throw new IllegalStateException("Tu cuenta no tiene permiso para consultar el historial.");
                }
            }
            List<Cambio> cambios = new ArrayList<>();
            try (PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT fecha,nombre_usuario,tabla,registro,operacion FROM auditoria_cambios ORDER BY fecha DESC,id DESC LIMIT 300");
                 ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) cambios.add(new Cambio(
                    resultado.getObject(1, OffsetDateTime.class), resultado.getString(2),
                    resultado.getString(3), resultado.getString(4), resultado.getString(5)));
            }
            return cambios;
        }
    }
}
