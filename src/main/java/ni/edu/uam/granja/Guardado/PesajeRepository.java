package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.model.Pesaje;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PesajeRepository {
    public void corregir(Pesaje pesaje) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir();PreparedStatement s=c.prepareStatement(
            "UPDATE pesajes SET fecha=?,peso_kg=?,observaciones=? WHERE id=? AND animal_id=?")) {
            s.setDate(1,Date.valueOf(pesaje.fecha()));s.setBigDecimal(2,pesaje.pesoKg());
            s.setString(3,pesaje.observaciones());s.setLong(4,pesaje.id());s.setLong(5,pesaje.animalId());
            if(s.executeUpdate()!=1) throw new IllegalArgumentException("El pesaje ya no está disponible.");
        }
    }
    public List<Pesaje> listarPorAnimal(long animalId) throws SQLException {
        String sql = "SELECT id, animal_id, fecha, peso_kg, observaciones FROM pesajes WHERE animal_id = ? ORDER BY fecha DESC, id DESC";
        List<Pesaje> resultado = new ArrayList<>();
        try (Connection conexion = ConexionBaseDatos.abrir();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, animalId);
            try (ResultSet filas = consulta.executeQuery()) {
                while (filas.next()) resultado.add(new Pesaje(filas.getLong("id"),
                    filas.getLong("animal_id"), filas.getDate("fecha").toLocalDate(),
                    filas.getBigDecimal("peso_kg"), filas.getString("observaciones")));
            }
        }
        return resultado;
    }

    public void guardar(Pesaje pesaje) throws SQLException {
        String sql = "INSERT INTO pesajes (animal_id, fecha, peso_kg, observaciones) VALUES (?, ?, ?, ?)";
        try (Connection conexion = ConexionBaseDatos.abrir();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, pesaje.animalId());
            consulta.setDate(2, Date.valueOf(pesaje.fecha()));
            consulta.setBigDecimal(3, pesaje.pesoKg());
            consulta.setString(4, pesaje.observaciones());
            consulta.executeUpdate();
        }
    }
}
