package ni.edu.uam.granja.Service;

import ni.edu.uam.granja.Guardado.UsuarioRepository;
import ni.edu.uam.granja.model.Usuario;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import ni.edu.uam.granja.GranjaApplication;

public class AutenticacionService {
    private static final int ITERACIONES=310_000;
    private final UsuarioRepository usuarios=new UsuarioRepository();
    public boolean requierePrimerAdministrador() throws SQLException { return !usuarios.hayUsuarios(); }
    private long administrador() {
        Usuario sesion=GranjaApplication.sesion();
        if(sesion==null || !"ADMINISTRADOR".equals(sesion.rol())) throw new IllegalStateException("Solo un administrador puede gestionar usuarios.");
        return sesion.id();
    }
    public List<Usuario> listarUsuarios() throws SQLException { return usuarios.listar(administrador()); }
    public void crearUsuario(String nombre,String usuario,String rol,char[] clave,char[] confirmacion) throws SQLException {
        validarDatos(nombre,usuario,rol);validarClave(clave,confirmacion);
        usuarios.crear(administrador(),nombre.trim(),usuario.trim(),rol,hash(clave));
    }
    public void actualizarUsuario(long id,String nombre,String usuario,String rol,boolean activo,char[] clave,char[] confirmacion) throws SQLException {
        validarDatos(nombre,usuario,rol);
        String nuevoHash=null;
        if(clave.length>0 || confirmacion.length>0) { validarClave(clave,confirmacion);nuevoHash=hash(clave); }
        usuarios.actualizar(administrador(),id,nombre.trim(),usuario.trim(),rol,activo,nuevoHash);
    }
    private static void validarDatos(String nombre,String usuario,String rol) {
        if(nombre==null || nombre.isBlank() || nombre.trim().length()>120) throw new IllegalArgumentException("Escribe un nombre de hasta 120 caracteres.");
        validarUsuario(usuario);
        if(!List.of("ADMINISTRADOR","ENCARGADO","TRABAJADOR").contains(rol)) throw new IllegalArgumentException("Selecciona un rol válido.");
    }
    private static void validarClave(char[] clave,char[] confirmacion) {
        if(clave.length<8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        if(!Arrays.equals(clave,confirmacion)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
    }

    public Usuario crearPrimerAdministrador(String nombre,String usuario,char[] clave,char[] confirmacion) throws SQLException {
        if(nombre==null || nombre.isBlank() || nombre.trim().length()>120)
            throw new IllegalArgumentException("Escribe un nombre de hasta 120 caracteres.");
        validarUsuario(usuario);
        if(clave.length<8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        if(!Arrays.equals(clave,confirmacion))
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        return usuarios.crearPrimerAdministrador(nombre.trim(),usuario.trim(),hash(clave));
    }
    public Usuario entrar(String nombre,char[] clave) throws SQLException {
        if(nombre==null || nombre.isBlank() || clave.length==0)
            throw new IllegalArgumentException("Escribe tu usuario y contraseña.");
        UsuarioRepository.Credenciales datos=usuarios.buscar(nombre.trim());
        if(datos==null || !datos.usuario().activo() || !verificar(clave,datos.hash()))
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        return datos.usuario();
    }
    private static void validarUsuario(String valor) {
        if(valor==null || !valor.matches("[A-Za-z0-9._-]{3,60}"))
            throw new IllegalArgumentException("El usuario debe tener entre 3 y 60 caracteres: letras, números, punto, guion o guion bajo.");
    }
    private static String hash(char[] clave) {
        byte[] sal=new byte[16]; new SecureRandom().nextBytes(sal);
        return "pbkdf2-sha256$"+ITERACIONES+"$"+Base64.getEncoder().encodeToString(sal)+"$"+
            Base64.getEncoder().encodeToString(derivar(clave,sal,ITERACIONES));
    }
    private static boolean verificar(char[] clave,String almacenado) {
        try {
            String[] partes=almacenado.split("\\$");
            if(partes.length!=4 || !partes[0].equals("pbkdf2-sha256")) return false;
            int iteraciones=Integer.parseInt(partes[1]);
            if(iteraciones<100_000 || iteraciones>1_000_000) return false;
            byte[] sal=Base64.getDecoder().decode(partes[2]);
            byte[] esperado=Base64.getDecoder().decode(partes[3]);
            return sal.length==16 && esperado.length==32 &&
                MessageDigest.isEqual(esperado,derivar(clave,sal,iteraciones));
        } catch (IllegalArgumentException e) { return false; }
    }
    private static byte[] derivar(char[] clave,byte[] sal,int iteraciones) {
        PBEKeySpec spec=new PBEKeySpec(clave,sal,iteraciones,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception e) { throw new IllegalStateException("No se pudo proteger la contraseña.",e); }
        finally { spec.clearPassword(); }
    }
}
