package ni.edu.uam.granja.Guardado;

import ni.edu.uam.granja.database.ConexionBaseDatos;
import ni.edu.uam.granja.model.Usuario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepository {
    public record Credenciales(Usuario usuario, String hash) {}
    public List<Usuario> listar(long actor) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            comprobarAdministrador(c,actor);
            List<Usuario> lista=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("SELECT id,nombre,nombre_usuario,rol,activo FROM usuarios ORDER BY nombre_usuario");ResultSet r=s.executeQuery()) {
                while(r.next()) lista.add(new Usuario(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getBoolean(5)));
            }
            return lista;
        }
    }
    public void crear(long actor,String nombre,String usuario,String rol,String hash) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                bloquear(c);comprobarAdministrador(c,actor);
                try(PreparedStatement s=c.prepareStatement("INSERT INTO usuarios(nombre,nombre_usuario,rol,contrasena_hash) VALUES(?,?,?,?)")) {
                    s.setString(1,nombre);s.setString(2,usuario);s.setString(3,rol);s.setString(4,hash);s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) { try { c.rollback(); } catch(SQLException x) { e.addSuppressed(x); } throw e; }
        }
    }
    public void actualizar(long actor,long id,String nombre,String usuario,String rol,boolean activo,String hash) throws SQLException {
        try(Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                bloquear(c);comprobarAdministrador(c,actor);
                if(actor==id && (!activo || !"ADMINISTRADOR".equals(rol)))
                    throw new IllegalArgumentException("No puedes desactivar tu cuenta ni quitarte el rol de administrador.");
                try(PreparedStatement s=c.prepareStatement("UPDATE usuarios SET nombre=?,nombre_usuario=?,rol=?,activo=?,contrasena_hash=COALESCE(?,contrasena_hash) WHERE id=?")) {
                    s.setString(1,nombre);s.setString(2,usuario);s.setString(3,rol);s.setBoolean(4,activo);
                    s.setString(5,hash);s.setLong(6,id);
                    if(s.executeUpdate()!=1) throw new IllegalArgumentException("El usuario ya no existe.");
                }
                c.commit();
            } catch(SQLException | RuntimeException e) { try { c.rollback(); } catch(SQLException x) { e.addSuppressed(x); } throw e; }
        }
    }
    private static void bloquear(Connection c) throws SQLException {
        try(Statement s=c.createStatement()) { s.execute("LOCK TABLE usuarios IN EXCLUSIVE MODE"); }
    }
    private static void comprobarAdministrador(Connection c,long id) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM usuarios WHERE id=? AND activo AND rol='ADMINISTRADOR'")) {
            s.setLong(1,id);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next()) throw new IllegalStateException("Solo un administrador activo puede administrar usuarios.");
            }
        }
    }

    public boolean hayUsuarios() throws SQLException {
        try (Connection c=ConexionBaseDatos.abrir();
             PreparedStatement s=c.prepareStatement("SELECT EXISTS (SELECT 1 FROM usuarios)");
             ResultSet r=s.executeQuery()) { r.next(); return r.getBoolean(1); }
    }

    public Credenciales buscar(String nombreUsuario) throws SQLException {
        try (Connection c=ConexionBaseDatos.abrir();
             PreparedStatement s=c.prepareStatement("SELECT id,nombre,nombre_usuario,rol,activo,contrasena_hash FROM usuarios WHERE nombre_usuario=?")) {
            s.setString(1,nombreUsuario);
            try (ResultSet r=s.executeQuery()) {
                if(!r.next()) return null;
                return new Credenciales(new Usuario(r.getLong(1),r.getString(2),r.getString(3),
                    r.getString(4),r.getBoolean(5)),r.getString(6));
            }
        }
    }

    public Usuario crearPrimerAdministrador(String nombre,String usuario,String hash) throws SQLException {
        try (Connection c=ConexionBaseDatos.abrir()) {
            c.setAutoCommit(false);
            try {
                try (Statement bloqueo=c.createStatement()) { bloqueo.execute("LOCK TABLE usuarios IN EXCLUSIVE MODE"); }
                try (PreparedStatement s=c.prepareStatement("SELECT EXISTS (SELECT 1 FROM usuarios)");
                     ResultSet r=s.executeQuery()) {
                    r.next();
                    if(r.getBoolean(1)) throw new IllegalStateException("Ya existe una cuenta. Inicia sesión.");
                }
                long id;
                try (PreparedStatement s=c.prepareStatement("INSERT INTO usuarios(nombre,nombre_usuario,contrasena_hash,rol) VALUES(?,?,?,'ADMINISTRADOR') RETURNING id")) {
                    s.setString(1,nombre); s.setString(2,usuario); s.setString(3,hash);
                    try (ResultSet r=s.executeQuery()) { r.next(); id=r.getLong(1); }
                }
                c.commit();
                return new Usuario(id,nombre,usuario,"ADMINISTRADOR",true);
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
                throw e;
            }
        }
    }
}
