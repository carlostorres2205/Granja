package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.database.ConexionBaseDatos;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrganizacionRepository {
    public record Opcion(long id, String nombre) {
        @Override public String toString() { return nombre; }
    }
    public record Asignacion(long animalId, Long loteId, Long ubicacionId) {}
    public record FilaAnimal(long id, String arete, String tipo, String lote, String ubicacion) {}

    public List<Opcion> lotes() throws SQLException {
        return opciones("SELECT id,nombre FROM lotes WHERE activo=TRUE ORDER BY nombre");
    }
    public List<Opcion> ubicaciones() throws SQLException {
        return opciones("SELECT id,nombre FROM ubicaciones WHERE activa=TRUE ORDER BY nombre");
    }
    private List<Opcion> opciones(String sql) throws SQLException {
        List<Opcion> lista=new ArrayList<>();
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(sql); ResultSet r=s.executeQuery()) {
            while(r.next()) lista.add(new Opcion(r.getLong(1),r.getString(2)));
        }
        return lista;
    }
    public List<FilaAnimal> animales() throws SQLException {
        String sql="SELECT a.id,a.arete,a.nombre,l.nombre,u.nombre FROM animales a LEFT JOIN lotes l ON a.lote_id=l.id LEFT JOIN ubicaciones u ON a.ubicacion_id=u.id WHERE a.estado='ACTIVO' ORDER BY a.arete";
        List<FilaAnimal> lista=new ArrayList<>();
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(sql); ResultSet r=s.executeQuery()) {
            while(r.next()) lista.add(new FilaAnimal(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));
        }
        return lista;
    }
    public void crearLote(String nombre,String proposito) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir();
            PreparedStatement s=c.prepareStatement("INSERT INTO lotes(nombre,proposito) VALUES (?,?)")) {
            s.setString(1,nombre); s.setString(2,proposito); s.executeUpdate();
        }
    }
    public void crearUbicacion(String nombre,String tipo,Integer capacidad) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir();
            PreparedStatement s=c.prepareStatement("INSERT INTO ubicaciones(nombre,tipo,capacidad) VALUES (?,?,?)")) {
            s.setString(1,nombre); s.setString(2,tipo);
            if(capacidad==null) s.setNull(3,Types.INTEGER); else s.setInt(3,capacidad);
            s.executeUpdate();
        }
    }
    public void asignar(long animalId,Long loteId,Long ubicacionId) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                Long loteAnterior, ubicacionAnterior;
                try(PreparedStatement s=c.prepareStatement("SELECT lote_id,ubicacion_id FROM animales WHERE id=? AND estado='ACTIVO' FOR UPDATE")) {
                    s.setLong(1,animalId);
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("El animal ya no está activo.");
                        loteAnterior=(Long)r.getObject(1); ubicacionAnterior=(Long)r.getObject(2);
                    }
                }
                if(java.util.Objects.equals(loteAnterior,loteId) && java.util.Objects.equals(ubicacionAnterior,ubicacionId))
                    throw new IllegalArgumentException("Selecciona un lote o ubicación diferente.");
                try(PreparedStatement s=c.prepareStatement("UPDATE animales SET lote_id=?,ubicacion_id=? WHERE id=?")) {
                    ponerId(s,1,loteId); ponerId(s,2,ubicacionId); s.setLong(3,animalId); s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO movimientos_animales(animal_id,ubicacion_origen_id,ubicacion_destino_id,lote_origen_id,lote_destino_id) VALUES (?,?,?,?,?)")) {
                    s.setLong(1,animalId); ponerId(s,2,ubicacionAnterior); ponerId(s,3,ubicacionId);
                    ponerId(s,4,loteAnterior); ponerId(s,5,loteId); s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException fallo) { e.addSuppressed(fallo); }
                throw e;
            }
        }
    }
    private static void ponerId(PreparedStatement s,int posicion,Long id) throws SQLException {
        if(id==null) s.setNull(posicion,Types.BIGINT); else s.setLong(posicion,id);
    }
}
