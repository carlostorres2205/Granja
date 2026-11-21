package ni.edu.uam.granja.Guardado;
import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.model.EventosSanitarios;
import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EventoSanitarioRepository {
    public List<EventosSanitarios> listar(long animalId) throws SQLException {
        String sql = "SELECT e.id,e.animal_id,e.tipo,e.fecha,e.descripcion,i.nombre AS producto,e.dosis,e.fecha_proxima,e.fecha_fin_retiro,e.observaciones FROM eventos_sanitarios e LEFT JOIN insumos i ON i.id=e.insumo_id WHERE e.animal_id=? ORDER BY e.fecha DESC,e.id DESC";
        List<EventosSanitarios> eventos = new ArrayList<>();
        try (Connection c = ConexionBaseDatos.abrir(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, animalId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) eventos.add(leer(r));
            }
        }
        return eventos;
    }
    public List<EventosSanitarios> proximos() throws SQLException {
        String sql = "SELECT e.id,e.animal_id,e.tipo,e.fecha,e.descripcion,i.nombre AS producto,e.dosis,e.fecha_proxima,e.fecha_fin_retiro,e.observaciones FROM eventos_sanitarios e LEFT JOIN insumos i ON i.id=e.insumo_id JOIN animales a ON a.id=e.animal_id WHERE a.estado='ACTIVO' AND e.fecha_proxima BETWEEN CURRENT_DATE AND CURRENT_DATE + 30 ORDER BY e.fecha_proxima,e.id";
        List<EventosSanitarios> eventos = new ArrayList<>();
        try (Connection c = ConexionBaseDatos.abrir(); PreparedStatement s = c.prepareStatement(sql); ResultSet r = s.executeQuery()) {
            while (r.next()) eventos.add(leer(r));
        }
        return eventos;
    }
    public void guardar(EventosSanitarios e,Long insumoId,BigDecimal cantidad) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                if(insumoId!=null) {
                    BigDecimal disponible;
                    try(PreparedStatement s=c.prepareStatement("SELECT existencia FROM insumos WHERE id=? AND activo=TRUE AND categoria='MEDICAMENTO' FOR UPDATE")) {
                        s.setLong(1,insumoId);
                        try(ResultSet r=s.executeQuery()) {
                            if(!r.next()) throw new IllegalArgumentException("Selecciona un medicamento activo.");
                            disponible=r.getBigDecimal(1);
                        }
                    }
                    if(disponible.compareTo(cantidad)<0) throw new IllegalArgumentException("No hay suficiente medicamento en inventario.");
                    try(PreparedStatement s=c.prepareStatement("UPDATE insumos SET existencia=existencia-? WHERE id=?")) {
                        s.setBigDecimal(1,cantidad);s.setLong(2,insumoId);s.executeUpdate();
                    }
                    try(PreparedStatement s=c.prepareStatement("INSERT INTO movimientos_inventario(insumo_id,tipo,cantidad,observaciones) VALUES(?,'SALIDA',?,?)")) {
                        s.setLong(1,insumoId);s.setBigDecimal(2,cantidad);
                        s.setString(3,"Aplicación sanitaria al animal #"+e.animalId()+"; fecha "+e.fecha());s.executeUpdate();
                    }
                }
                String sql="INSERT INTO eventos_sanitarios(animal_id,tipo,fecha,descripcion,insumo_id,dosis,fecha_proxima,fecha_fin_retiro,observaciones) VALUES(?,?,?,?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(sql)) {
                    s.setLong(1,e.animalId());s.setString(2,e.tipo());s.setDate(3,Date.valueOf(e.fecha()));
                    s.setString(4,e.descripcion());
                    if(insumoId==null)s.setNull(5,Types.BIGINT);else s.setLong(5,insumoId);
                    s.setString(6,e.dosis());
                    if(e.fechaProxima()==null)s.setNull(7,Types.DATE);else s.setDate(7,Date.valueOf(e.fechaProxima()));
                    if(e.finRetiro()==null)s.setNull(8,Types.DATE);else s.setDate(8,Date.valueOf(e.finRetiro()));
                    s.setString(9,e.observaciones());s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException error) {
                try { c.rollback(); } catch(SQLException fallo) { error.addSuppressed(fallo); }
                throw error;
            }
        }
    }
    private EventosSanitarios leer(ResultSet r) throws SQLException {
        Date proxima=r.getDate("fecha_proxima"), retiro=r.getDate("fecha_fin_retiro");
        return new EventosSanitarios(r.getLong("id"),r.getLong("animal_id"),r.getString("tipo"),
            r.getDate("fecha").toLocalDate(),r.getString("descripcion"),r.getString("producto"),
            r.getString("dosis"),proxima==null?null:proxima.toLocalDate(),
            retiro==null?null:retiro.toLocalDate(),r.getString("observaciones"));
    }
}
