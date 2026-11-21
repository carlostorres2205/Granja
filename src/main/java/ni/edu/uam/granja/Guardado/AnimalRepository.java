package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.model.Animal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnimalRepository {
    public void actualizar(Animal animal) throws SQLException {
        String sql="UPDATE animales SET arete=?,nombre=?,sexo=?,raza=?,fecha_nacimiento=?,fecha_ingreso=?,origen=?,peso_inicial_kg=? WHERE id=? AND estado='ACTIVO'";
        try(Connection c=ConexionBaseDatos.abrir();PreparedStatement s=c.prepareStatement(sql)) {
            s.setString(1,animal.arete());s.setString(2,animal.nombre());s.setString(3,animal.sexo());s.setString(4,animal.raza());
            if(animal.fechaNacimiento()==null)s.setNull(5,Types.DATE);else s.setDate(5,Date.valueOf(animal.fechaNacimiento()));
            s.setDate(6,Date.valueOf(animal.fechaIngreso()));s.setString(7,animal.origen());
            if(animal.pesoInicialKg()==null)s.setNull(8,Types.NUMERIC);else s.setBigDecimal(8,animal.pesoInicialKg());
            s.setLong(9,animal.id());
            if(s.executeUpdate()!=1) throw new IllegalArgumentException("Solo se pueden corregir animales activos.");
        }
    }
    public void registrarBaja(long animalId,java.time.LocalDate fecha,String motivo) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                java.time.LocalDate ingreso;
                try(PreparedStatement s=c.prepareStatement("SELECT fecha_ingreso FROM animales WHERE id=? AND estado='ACTIVO' FOR UPDATE")) {
                    s.setLong(1,animalId);
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("El animal no está activo.");
                        ingreso=r.getDate(1).toLocalDate();
                    }
                }
                if(fecha.isBefore(ingreso)) throw new IllegalArgumentException("La baja no puede ser anterior al ingreso.");
                try(PreparedStatement s=c.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM pesajes WHERE animal_id=? AND fecha>?) OR "
                    +"EXISTS(SELECT 1 FROM eventos_sanitarios WHERE animal_id=? AND fecha>?) OR "
                    +"EXISTS(SELECT 1 FROM partos WHERE madre_id=? AND fecha>?)")) {
                    s.setLong(1,animalId);s.setDate(2,Date.valueOf(fecha));
                    s.setLong(3,animalId);s.setDate(4,Date.valueOf(fecha));
                    s.setLong(5,animalId);s.setDate(6,Date.valueOf(fecha));
                    try(ResultSet r=s.executeQuery()) {
                        r.next();
                        if(r.getBoolean(1)) throw new IllegalArgumentException(
                            "Hay pesajes, eventos de salud o partos posteriores a esa fecha. Corrige la fecha de baja.");
                    }
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO bajas_animales(animal_id,fecha,motivo) VALUES(?,?,?)")) {
                    s.setLong(1,animalId);s.setDate(2,Date.valueOf(fecha));s.setString(3,motivo);s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE animales SET estado='FALLECIDO',lote_id=NULL,ubicacion_id=NULL WHERE id=?")) {
                    s.setLong(1,animalId);s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) {
                try { c.rollback(); } catch(SQLException fallo) { e.addSuppressed(fallo); }
                throw e;
            }
        }
    }
    public List<Animal> listar() throws SQLException {
        String sql = "SELECT id, arete, nombre, sexo, raza, fecha_nacimiento, fecha_ingreso, origen, estado, peso_inicial_kg FROM animales ORDER BY id DESC";
        List<Animal> resultado = new ArrayList<>();
        try (Connection conexion = ConexionBaseDatos.abrir();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet filas = consulta.executeQuery()) {
            while (filas.next()) {
                Date nacimiento = filas.getDate("fecha_nacimiento");
                resultado.add(new Animal(filas.getLong("id"), filas.getString("arete"),
                    filas.getString("nombre"), filas.getString("sexo"), filas.getString("raza"),
                    nacimiento == null ? null : nacimiento.toLocalDate(),
                    filas.getDate("fecha_ingreso").toLocalDate(), filas.getString("origen"),
                    filas.getString("estado"), filas.getBigDecimal("peso_inicial_kg")));
            }
        }
        return resultado;
    }

    public Animal guardar(Animal animal) throws SQLException {
        String sql = "INSERT INTO animales (arete, nombre, sexo, raza, fecha_nacimiento, fecha_ingreso, origen, estado, peso_inicial_kg) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conexion = ConexionBaseDatos.abrir();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, animal.arete());
            consulta.setString(2, animal.nombre());
            consulta.setString(3, animal.sexo());
            consulta.setString(4, animal.raza());
            if (animal.fechaNacimiento() == null) consulta.setNull(5, Types.DATE);
            else consulta.setDate(5, Date.valueOf(animal.fechaNacimiento()));
            consulta.setDate(6, Date.valueOf(animal.fechaIngreso()));
            consulta.setString(7, animal.origen());
            consulta.setString(8, animal.estado());
            if (animal.pesoInicialKg() == null) consulta.setNull(9, Types.NUMERIC);
            else consulta.setBigDecimal(9, animal.pesoInicialKg());
            try (ResultSet fila = consulta.executeQuery()) {
                fila.next();
                return new Animal(fila.getLong(1), animal.arete(), animal.nombre(), animal.sexo(),
                    animal.raza(), animal.fechaNacimiento(), animal.fechaIngreso(), animal.origen(),
                    animal.estado(), animal.pesoInicialKg());
            }
        }
    }
}
