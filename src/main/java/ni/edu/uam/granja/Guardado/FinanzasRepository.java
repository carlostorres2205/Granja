package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.model.MovimientoFinanciero;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FinanzasRepository {
    public record LineaVenta(long animalId,String arete,BigDecimal pesoKg,BigDecimal precio) {}
    public record Comprador(long id,String nombre) {
        @Override public String toString() { return nombre; }
    }
    public record VentaResumen(long id,LocalDate fecha,String comprador,int animales,BigDecimal total) {}
    public List<VentaResumen> ventas(LocalDate desde,LocalDate hasta) throws SQLException {
        String sql="SELECT v.id,v.fecha,COALESCE(c.nombre,'Sin comprador'),COUNT(d.id),v.total "
            +"FROM ventas v LEFT JOIN contactos c ON c.id=v.comprador_id "
            +"LEFT JOIN detalle_ventas d ON d.venta_id=v.id WHERE v.fecha BETWEEN ? AND ? "
            +"GROUP BY v.id,v.fecha,c.nombre,v.total ORDER BY v.fecha DESC,v.id DESC";
        List<VentaResumen> lista=new ArrayList<>();
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setDate(1,Date.valueOf(desde));s.setDate(2,Date.valueOf(hasta));
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) lista.add(new VentaResumen(r.getLong(1),r.getDate(2).toLocalDate(),
                    r.getString(3),r.getInt(4),r.getBigDecimal(5)));
            }
        }
        return lista;
    }
    public List<Comprador> compradores() throws SQLException {
        List<Comprador> lista=new ArrayList<>();
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(
            "SELECT id,nombre FROM contactos WHERE tipo='COMPRADOR' ORDER BY nombre");
            ResultSet r=s.executeQuery()) {
            while(r.next()) lista.add(new Comprador(r.getLong(1),r.getString(2)));
        }
        return lista;
    }
    public Comprador crearComprador(String nombre) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(
            "INSERT INTO contactos(nombre,tipo) VALUES(?,'COMPRADOR') RETURNING id")) {
            s.setString(1,nombre);
            try(ResultSet r=s.executeQuery()) { r.next(); return new Comprador(r.getLong(1),nombre); }
        }
    }
    public List<MovimientoFinanciero> movimientos(LocalDate desde,LocalDate hasta) throws SQLException {
        List<MovimientoFinanciero> lista=new ArrayList<>();
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(
            "SELECT id,fecha,tipo,categoria,concepto,importe,venta_id FROM movimientos_financieros WHERE fecha BETWEEN ? AND ? ORDER BY fecha DESC,id DESC")) {
            s.setDate(1,Date.valueOf(desde));s.setDate(2,Date.valueOf(hasta));
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) lista.add(new MovimientoFinanciero(r.getLong(1),r.getDate(2).toLocalDate(),
                    r.getString(3),r.getString(4),r.getString(5),r.getBigDecimal(6),(Long)r.getObject(7)));
            }
        }
        return lista;
    }
    public void guardarMovimiento(LocalDate fecha,String tipo,String categoria,String concepto,BigDecimal importe,long responsable) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir(); PreparedStatement s=c.prepareStatement(
            "INSERT INTO movimientos_financieros(fecha,tipo,categoria,concepto,importe,responsable_id) VALUES(?,?,?,?,?,?)")) {
            s.setDate(1,Date.valueOf(fecha));s.setString(2,tipo);s.setString(3,categoria);
            s.setString(4,concepto);s.setBigDecimal(5,importe);s.setLong(6,responsable);s.executeUpdate();
        }
    }
    public void corregirMovimiento(long id,LocalDate fecha,String tipo,String categoria,String concepto,BigDecimal importe) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir();PreparedStatement s=c.prepareStatement(
            "UPDATE movimientos_financieros SET fecha=?,tipo=?,categoria=?,concepto=?,importe=? WHERE id=? AND venta_id IS NULL")) {
            s.setDate(1,Date.valueOf(fecha));s.setString(2,tipo);s.setString(3,categoria);
            s.setString(4,concepto);s.setBigDecimal(5,importe);s.setLong(6,id);
            if(s.executeUpdate()!=1) throw new IllegalArgumentException("Solo puedes corregir movimientos manuales. Las ventas se corrigen desde su registro.");
        }
    }
    public long vender(LocalDate fecha,Long compradorId,List<LineaVenta> lineas,long responsable) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                BigDecimal total=BigDecimal.ZERO;
                for(LineaVenta linea:lineas) {
                    try(PreparedStatement s=c.prepareStatement("SELECT estado,fecha_ingreso FROM animales WHERE id=? FOR UPDATE")) {
                        s.setLong(1,linea.animalId());
                        try(ResultSet r=s.executeQuery()) {
                            if(!r.next() || !"ACTIVO".equals(r.getString(1)))
                                throw new IllegalArgumentException("El animal "+linea.arete()+" ya no está activo.");
                            if(fecha.isBefore(r.getDate(2).toLocalDate()))
                                throw new IllegalArgumentException("La venta no puede ser anterior al ingreso de "+linea.arete()+".");
                        }
                    }
                    try(PreparedStatement s=c.prepareStatement(
                        "SELECT EXISTS(SELECT 1 FROM eventos_sanitarios WHERE animal_id=? AND fecha<=? AND fecha_fin_retiro>=?)")) {
                        s.setLong(1,linea.animalId());s.setDate(2,Date.valueOf(fecha));s.setDate(3,Date.valueOf(fecha));
                        try(ResultSet r=s.executeQuery()) {
                            r.next();
                            if(r.getBoolean(1)) throw new IllegalArgumentException(
                                "El animal "+linea.arete()+" sigue en período de retiro en la fecha de venta.");
                        }
                    }
                    total=total.add(linea.precio());
                }
                long ventaId;
                try(PreparedStatement s=c.prepareStatement("INSERT INTO ventas(fecha,comprador_id,total) VALUES(?,?,?) RETURNING id")) {
                    s.setDate(1,Date.valueOf(fecha)); ponerId(s,2,compradorId);s.setBigDecimal(3,total);
                    try(ResultSet r=s.executeQuery()) { r.next();ventaId=r.getLong(1); }
                }
                for(LineaVenta linea:lineas) {
                    try(PreparedStatement s=c.prepareStatement(
                        "INSERT INTO detalle_ventas(venta_id,animal_id,peso_venta_kg,precio) VALUES(?,?,?,?)")) {
                        s.setLong(1,ventaId);s.setLong(2,linea.animalId());
                        if(linea.pesoKg()==null)s.setNull(3,Types.NUMERIC);else s.setBigDecimal(3,linea.pesoKg());
                        s.setBigDecimal(4,linea.precio());s.executeUpdate();
                    }
                    try(PreparedStatement s=c.prepareStatement(
                        "UPDATE animales SET estado='VENDIDO',lote_id=NULL,ubicacion_id=NULL WHERE id=?")) {
                        s.setLong(1,linea.animalId());s.executeUpdate();
                    }
                }
                try(PreparedStatement s=c.prepareStatement(
                    "INSERT INTO movimientos_financieros(fecha,tipo,categoria,concepto,importe,venta_id,responsable_id) VALUES(?,'INGRESO','VENTA_GANADO',?,?,?,?)")) {
                    s.setDate(1,Date.valueOf(fecha));s.setString(2,"Venta de "+lineas.size()+" animal(es)");
                    s.setBigDecimal(3,total);s.setLong(4,ventaId);s.setLong(5,responsable);s.executeUpdate();
                }
                c.commit();return ventaId;
            } catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException fallo) { e.addSuppressed(fallo); }
                throw e;
            }
        }
    }
    private static void ponerId(PreparedStatement s,int posicion,Long id) throws SQLException {
        if(id==null)s.setNull(posicion,Types.BIGINT);else s.setLong(posicion,id);
    }
}
