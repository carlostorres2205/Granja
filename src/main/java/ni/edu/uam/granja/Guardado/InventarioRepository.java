package ni.edu.uam.granja.Guardado;
import ni.edu.uam.granja.database.ConexionBaseDatos;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventarioRepository {
    public record Insumo(long id,String nombre,String categoria,String unidad,BigDecimal existencia,BigDecimal minimo,LocalDate vencimiento) {
        @Override public String toString() { return nombre+" ("+unidad+")"; }
    }
    public List<Insumo> listar() throws SQLException {
        List<Insumo> lista=new ArrayList<>();
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(
            "SELECT id,nombre,categoria,unidad,existencia,cantidad_minima,fecha_vencimiento FROM insumos WHERE activo=TRUE ORDER BY nombre");
            ResultSet r=s.executeQuery()) {
            while(r.next()) {
                Date v=r.getDate(7);
                lista.add(new Insumo(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),
                    r.getBigDecimal(5),r.getBigDecimal(6),v==null?null:v.toLocalDate()));
            }
        }
        return lista;
    }
    public void crear(String nombre,String categoria,String unidad,BigDecimal minimo,LocalDate vencimiento) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(
            "INSERT INTO insumos(nombre,categoria,unidad,cantidad_minima,fecha_vencimiento) VALUES(?,?,?,?,?)")) {
            s.setString(1,nombre); s.setString(2,categoria); s.setString(3,unidad); s.setBigDecimal(4,minimo);
            if(vencimiento==null) s.setNull(5,Types.DATE); else s.setDate(5,Date.valueOf(vencimiento));
            s.executeUpdate();
        }
    }
    public void mover(long insumoId,String tipo,BigDecimal cantidad) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                BigDecimal actual;
                try(PreparedStatement s=c.prepareStatement("SELECT existencia FROM insumos WHERE id=? AND activo=TRUE FOR UPDATE")) {
                    s.setLong(1,insumoId);
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("El insumo no está disponible.");
                        actual=r.getBigDecimal(1);
                    }
                }
                BigDecimal nuevo="ENTRADA".equals(tipo)?actual.add(cantidad):actual.subtract(cantidad);
                if(nuevo.signum()<0) throw new IllegalArgumentException("La salida supera la existencia disponible.");
                try(PreparedStatement s=c.prepareStatement("UPDATE insumos SET existencia=? WHERE id=?")) {
                    s.setBigDecimal(1,nuevo); s.setLong(2,insumoId); s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO movimientos_inventario(insumo_id,tipo,cantidad) VALUES(?,?,?)")) {
                    s.setLong(1,insumoId); s.setString(2,tipo); s.setBigDecimal(3,cantidad); s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException fallo) { e.addSuppressed(fallo); } throw e;
            }
        }
    }
    public void ajustar(long insumoId,BigDecimal existenciaNueva,String motivo) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                BigDecimal anterior;
                try(PreparedStatement s=c.prepareStatement("SELECT existencia FROM insumos WHERE id=? AND activo=TRUE FOR UPDATE")) {
                    s.setLong(1,insumoId);
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("El insumo no está disponible.");
                        anterior=r.getBigDecimal(1);
                    }
                }
                BigDecimal diferencia=existenciaNueva.subtract(anterior);
                if(diferencia.signum()==0) throw new IllegalArgumentException("La nueva existencia es igual a la actual.");
                try(PreparedStatement s=c.prepareStatement("UPDATE insumos SET existencia=? WHERE id=?")) {
                    s.setBigDecimal(1,existenciaNueva);s.setLong(2,insumoId);s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO movimientos_inventario(insumo_id,tipo,cantidad,cantidad_ajuste,observaciones) VALUES(?,'AJUSTE',?,?,?)")) {
                    s.setLong(1,insumoId);s.setBigDecimal(2,diferencia.abs());
                    s.setBigDecimal(3,diferencia);s.setString(4,motivo);s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException fallo) { e.addSuppressed(fallo); }
                throw e;
            }
        }
    }
    public void alimentar(long loteId,long insumoId,LocalDate fecha,BigDecimal cantidad) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                BigDecimal actual;
                try(PreparedStatement s=c.prepareStatement("SELECT existencia FROM insumos WHERE id=? AND categoria IN ('ALIMENTO','SUPLEMENTO') AND activo=TRUE FOR UPDATE")) {
                    s.setLong(1,insumoId);
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("Selecciona un alimento o suplemento activo.");
                        actual=r.getBigDecimal(1);
                    }
                }
                if(actual.compareTo(cantidad)<0) throw new IllegalArgumentException("No hay suficiente existencia.");
                try(PreparedStatement s=c.prepareStatement("UPDATE insumos SET existencia=existencia-? WHERE id=?")) {
                    s.setBigDecimal(1,cantidad); s.setLong(2,insumoId); s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO movimientos_inventario(insumo_id,tipo,cantidad,observaciones) VALUES(?,'SALIDA',?,'Alimentación de lote')")) {
                    s.setLong(1,insumoId); s.setBigDecimal(2,cantidad); s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO alimentaciones(lote_id,insumo_id,fecha,cantidad) VALUES(?,?,?,?)")) {
                    s.setLong(1,loteId); s.setLong(2,insumoId); s.setDate(3,Date.valueOf(fecha)); s.setBigDecimal(4,cantidad); s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException fallo) { e.addSuppressed(fallo); } throw e;
            }
        }
    }
}
