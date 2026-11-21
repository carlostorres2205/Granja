package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.Service.Permisos;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteRepository {
    public record Fila(LocalDate fecha,String tipo,String detalle,String cantidad,String importe) {}
    public record Finanzas(List<Fila> filas,BigDecimal ingresos,BigDecimal gastos) {}

    public Finanzas finanzas(LocalDate desde,LocalDate hasta) throws SQLException {
        Permisos.exigirReportes();
        String sql="SELECT fecha,tipo,categoria||' · '||concepto,importe FROM movimientos_financieros "
            +"WHERE fecha BETWEEN ? AND ? ORDER BY fecha DESC,id DESC";
        List<Fila> filas=new ArrayList<>();
        BigDecimal ingresos=BigDecimal.ZERO,gastos=BigDecimal.ZERO;
        try(Connection c=ConexionBaseDatos.abrir();PreparedStatement s=c.prepareStatement(sql)) {
            fechas(s,desde,hasta);
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    BigDecimal valor=r.getBigDecimal(4);
                    if("INGRESO".equals(r.getString(2))) ingresos=ingresos.add(valor);
                    else gastos=gastos.add(valor);
                    filas.add(new Fila(r.getDate(1).toLocalDate(),r.getString(2),r.getString(3),"",valor.toPlainString()));
                }
            }
        }
        return new Finanzas(filas,ingresos,gastos);
    }
    public List<Fila> entradasSalidas(LocalDate desde,LocalDate hasta) throws SQLException {
        Permisos.exigirReportes();
        String sql="SELECT a.fecha_ingreso AS fecha, CASE a.origen WHEN 'NACIDO' THEN 'Nacimiento' WHEN 'COMPRADO' THEN 'Compra' ELSE 'Otra entrada' END AS tipo,"
            +"a.arete||' · '||COALESCE(a.nombre,'Tipo sin registrar') AS detalle,'' AS cantidad,'' AS importe "
            +"FROM animales a WHERE a.fecha_ingreso BETWEEN ? AND ? "
            +"UNION ALL SELECT v.fecha,'Venta',a.arete||' · '||COALESCE(a.nombre,'Tipo sin registrar'),"
            +"COALESCE(d.peso_venta_kg::text||' kg',''),d.precio::text "
            +"FROM detalle_ventas d JOIN ventas v ON v.id=d.venta_id JOIN animales a ON a.id=d.animal_id "
            +"WHERE v.fecha BETWEEN ? AND ? "
            +"UNION ALL SELECT b.fecha,'Fallecimiento',a.arete||' · '||COALESCE(a.nombre,'Tipo sin registrar')||' · '||b.motivo,"
            +"'','' FROM bajas_animales b JOIN animales a ON a.id=b.animal_id "
            +"WHERE b.fecha BETWEEN ? AND ? ORDER BY fecha DESC,tipo,detalle";
        return consultar(sql,desde,hasta,3);
    }
    public List<Fila> traslados(LocalDate desde,LocalDate hasta) throws SQLException {
        Permisos.exigirReportes();
        String sql="SELECT m.fecha::date AS fecha,'Traslado' AS tipo,"
            +"a.arete||' · lote: '||COALESCE(lo.nombre,'Sin lote')||' → '||COALESCE(ld.nombre,'Sin lote')"
            +"||' · ubicación: '||COALESCE(uo.nombre,'Sin ubicación')||' → '||COALESCE(ud.nombre,'Sin ubicación') AS detalle,"
            +"'' AS cantidad,'' AS importe FROM movimientos_animales m JOIN animales a ON a.id=m.animal_id "
            +"LEFT JOIN lotes lo ON lo.id=m.lote_origen_id LEFT JOIN lotes ld ON ld.id=m.lote_destino_id "
            +"LEFT JOIN ubicaciones uo ON uo.id=m.ubicacion_origen_id LEFT JOIN ubicaciones ud ON ud.id=m.ubicacion_destino_id "
            +"WHERE m.fecha::date BETWEEN ? AND ? ORDER BY fecha DESC,m.id DESC";
        return consultar(sql,desde,hasta,1);
    }
    public List<Fila> consumo(LocalDate desde,LocalDate hasta) throws SQLException {
        Permisos.exigirReportes();
        String sql="SELECT f.fecha,'Alimentación' AS tipo,i.nombre||' · lote '||l.nombre AS detalle,"
            +"f.cantidad::text||' '||i.unidad AS cantidad,COALESCE(f.costo_total::text,'') AS importe "
            +"FROM alimentaciones f JOIN insumos i ON i.id=f.insumo_id JOIN lotes l ON l.id=f.lote_id "
            +"WHERE f.fecha BETWEEN ? AND ? "
            +"UNION ALL SELECT m.fecha::date,CASE i.categoria WHEN 'MEDICAMENTO' THEN 'Medicamento' ELSE 'Otro insumo' END,"
            +"i.nombre,m.cantidad::text||' '||i.unidad,'' "
            +"FROM movimientos_inventario m JOIN insumos i ON i.id=m.insumo_id "
            +"WHERE m.tipo='SALIDA' AND i.categoria NOT IN ('ALIMENTO','SUPLEMENTO') AND m.fecha::date BETWEEN ? AND ? "
            +"ORDER BY fecha DESC,tipo,detalle";
        return consultar(sql,desde,hasta,2);
    }
    public List<Fila> salud(LocalDate desde,LocalDate hasta) throws SQLException {
        Permisos.exigirReportes();
        String sql="SELECT e.fecha,e.tipo,a.arete||' · '||e.descripcion,COALESCE(e.dosis,''),'' "
            +"FROM eventos_sanitarios e JOIN animales a ON a.id=e.animal_id "
            +"WHERE e.fecha BETWEEN ? AND ? ORDER BY e.fecha DESC,e.id DESC";
        return consultar(sql,desde,hasta,1);
    }
    private List<Fila> consultar(String sql,LocalDate desde,LocalDate hasta,int rangos) throws SQLException {
        List<Fila> filas=new ArrayList<>();
        try(Connection c=ConexionBaseDatos.abrir();PreparedStatement s=c.prepareStatement(sql)) {
            for(int i=0;i<rangos;i++) {
                s.setDate(i*2+1,Date.valueOf(desde));s.setDate(i*2+2,Date.valueOf(hasta));
            }
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) filas.add(new Fila(r.getDate(1).toLocalDate(),r.getString(2),
                    r.getString(3),r.getString(4),r.getString(5)));
            }
        }
        return filas;
    }
    private static void fechas(PreparedStatement s,LocalDate desde,LocalDate hasta) throws SQLException {
        s.setDate(1,Date.valueOf(desde));s.setDate(2,Date.valueOf(hasta));
    }
}
