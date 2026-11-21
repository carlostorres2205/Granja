package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.database.ConexionBaseDatos;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReproduccionRepository {
    public record Servicio(long id, long hembraId, LocalDate fecha, String tipo, Long machoId) {
        @Override public String toString() { return fecha + " · " + tipo + " · #" + id; }
    }
    public record Cria(String arete, String sexo, BigDecimal peso, boolean viva) {}
    public record Evento(LocalDate fecha, String categoria, String detalle) {}

    public List<Servicio> servicios(long hembraId) throws SQLException {
        List<Servicio> lista = new ArrayList<>();
        try (Connection c = ConexionBaseDatos.abrir();
             PreparedStatement s = c.prepareStatement("SELECT id, hembra_id, fecha, tipo, macho_id FROM servicios_reproductivos WHERE hembra_id=? ORDER BY fecha DESC, id DESC")) {
            s.setLong(1, hembraId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) lista.add(new Servicio(r.getLong(1), r.getLong(2), r.getDate(3).toLocalDate(), r.getString(4), (Long) r.getObject(5)));
            }
        }
        return lista;
    }
    public void guardarServicio(long hembra, LocalDate fecha, String tipo, Long macho, String semen, String notas) throws SQLException {
        try (Connection c = ConexionBaseDatos.abrir();
             PreparedStatement s = c.prepareStatement("INSERT INTO servicios_reproductivos (hembra_id, fecha, tipo, macho_id, identificacion_semen, observaciones) VALUES (?,?,?,?,?,?)")) {
            s.setLong(1, hembra); s.setDate(2, Date.valueOf(fecha)); s.setString(3, tipo);
            if (macho == null) s.setNull(4, Types.BIGINT); else s.setLong(4, macho);
            s.setString(5, semen); s.setString(6, notas); s.executeUpdate();
        }
    }
    public void guardarDiagnostico(long hembra, Long servicio, LocalDate fecha, String resultado, LocalDate estimada, String notas) throws SQLException {
        try (Connection c = ConexionBaseDatos.abrir();
             PreparedStatement s = c.prepareStatement("INSERT INTO diagnosticos_gestacion (hembra_id, servicio_id, fecha, resultado, fecha_parto_estimada, observaciones) VALUES (?,?,?,?,?,?)")) {
            s.setLong(1, hembra); if (servicio == null) s.setNull(2, Types.BIGINT); else s.setLong(2, servicio);
            s.setDate(3, Date.valueOf(fecha)); s.setString(4, resultado);
            if (estimada == null) s.setNull(5, Types.DATE); else s.setDate(5, Date.valueOf(estimada));
            s.setString(6, notas); s.executeUpdate();
        }
    }
    public void guardarParto(long madre, Long servicio, LocalDate fecha, String dificultad, String notas, List<Cria> crias) throws SQLException {
        try (Connection c = ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                Long padre = null;
                if (servicio != null) {
                    try (PreparedStatement s = c.prepareStatement("SELECT macho_id FROM servicios_reproductivos WHERE id=? AND hembra_id=? AND fecha<=?")) {
                        s.setLong(1, servicio); s.setLong(2, madre); s.setDate(3, Date.valueOf(fecha));
                        try (ResultSet r = s.executeQuery()) {
                            if (!r.next()) throw new SQLException("Servicio inválido para la madre o fecha.");
                            padre = (Long) r.getObject(1);
                        }
                    }
                }
                String especie;
                try (PreparedStatement s = c.prepareStatement("SELECT nombre FROM animales WHERE id=? AND sexo='H' AND estado='ACTIVO'")) {
                    s.setLong(1, madre);
                    try (ResultSet r = s.executeQuery()) {
                        if (!r.next()) throw new SQLException("La madre no está disponible.");
                        especie = r.getString(1);
                    }
                }
                long partoId;
                try (PreparedStatement s = c.prepareStatement("INSERT INTO partos (madre_id, servicio_id, fecha, dificultad, observaciones) VALUES (?,?,?,?,?) RETURNING id")) {
                    s.setLong(1, madre); if (servicio == null) s.setNull(2, Types.BIGINT); else s.setLong(2, servicio);
                    s.setDate(3, Date.valueOf(fecha)); s.setString(4, dificultad); s.setString(5, notas);
                    try (ResultSet r = s.executeQuery()) { r.next(); partoId = r.getLong(1); }
                }
                for (Cria cria : crias) {
                    long criaId;
                    try (PreparedStatement s = c.prepareStatement("INSERT INTO animales (arete,nombre,sexo,fecha_nacimiento,fecha_ingreso,origen,estado,madre_id,padre_id,peso_inicial_kg) VALUES (?,?,?,?,?,'NACIDO',?,?,?,?) RETURNING id")) {
                        s.setString(1, cria.arete()); s.setString(2, especie); s.setString(3, cria.sexo());
                        s.setDate(4, Date.valueOf(fecha)); s.setDate(5, Date.valueOf(fecha));
                        s.setString(6, cria.viva() ? "ACTIVO" : "FALLECIDO"); s.setLong(7, madre);
                        if (padre == null) s.setNull(8, Types.BIGINT); else s.setLong(8, padre);
                        if (cria.peso() == null) s.setNull(9, Types.NUMERIC); else s.setBigDecimal(9, cria.peso());
                        try (ResultSet r = s.executeQuery()) { r.next(); criaId = r.getLong(1); }
                    }
                    try (PreparedStatement s = c.prepareStatement("INSERT INTO crias_parto (parto_id,cria_id,peso_nacimiento_kg,estado_nacimiento) VALUES (?,?,?,?)")) {
                        s.setLong(1, partoId); s.setLong(2, criaId);
                        if (cria.peso() == null) s.setNull(3, Types.NUMERIC); else s.setBigDecimal(3, cria.peso());
                        s.setString(4, cria.viva() ? "VIVO" : "FALLECIDO"); s.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
    public List<Evento> historial(long hembra) throws SQLException {
        String sql = "SELECT fecha, 'Servicio' AS categoria, tipo || COALESCE(' · ' || observaciones,'') AS detalle FROM servicios_reproductivos WHERE hembra_id=? "
            + "UNION ALL SELECT fecha, 'Diagnóstico', resultado || COALESCE(' · parto estimado: ' || fecha_parto_estimada::text,'') || COALESCE(' · ' || observaciones,'') FROM diagnosticos_gestacion WHERE hembra_id=? "
            + "UNION ALL SELECT p.fecha, 'Parto', COALESCE(p.dificultad,'') || ' · crías: ' || (SELECT count(*) FROM crias_parto c WHERE c.parto_id=p.id)::text || COALESCE(' · ' || p.observaciones,'') FROM partos p WHERE p.madre_id=? ORDER BY fecha DESC";
        List<Evento> lista = new ArrayList<>();
        try (Connection c = ConexionBaseDatos.abrir(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i=1; i<=3; i++) s.setLong(i, hembra);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) lista.add(new Evento(r.getDate(1).toLocalDate(), r.getString(2), r.getString(3)));
            }
        }
        return lista;
    }
}
