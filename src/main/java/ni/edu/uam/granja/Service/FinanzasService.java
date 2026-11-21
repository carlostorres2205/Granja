package ni.edu.uam.granja.Service;

import ni.edu.uam.granja.Guardado.FinanzasRepository;
import ni.edu.uam.granja.Guardado.FinanzasRepository.LineaVenta;
import ni.edu.uam.granja.model.Animal;
import ni.edu.uam.granja.model.MovimientoFinanciero;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FinanzasService {
    private final FinanzasRepository repo=new FinanzasRepository();
    public FinanzasRepository repositorio() { return repo; }
    public List<MovimientoFinanciero> movimientos(LocalDate desde,LocalDate hasta) throws SQLException {
        if(desde==null||hasta==null||desde.isAfter(hasta)) throw new IllegalArgumentException("Selecciona un rango de fechas válido.");
        return repo.movimientos(desde,hasta);
    }
    public void guardarMovimiento(LocalDate fecha,String tipo,String categoria,String concepto,BigDecimal importe,long responsable) throws SQLException {
        validarMovimiento(fecha,tipo,categoria,concepto,importe);
        repo.guardarMovimiento(fecha,tipo,categoria,concepto.trim(),importe,responsable);
    }
    public void corregirMovimiento(long id,LocalDate fecha,String tipo,String categoria,String concepto,BigDecimal importe) throws SQLException {
        validarMovimiento(fecha,tipo,categoria,concepto,importe);
        repo.corregirMovimiento(id,fecha,tipo,categoria,concepto.trim(),importe);
    }
    private void validarMovimiento(LocalDate fecha,String tipo,String categoria,String concepto,BigDecimal importe) {
        fechaValida(fecha);
        if(!List.of("INGRESO","GASTO").contains(tipo)) throw new IllegalArgumentException("Tipo de movimiento inválido.");
        if(!List.of("COMPRA_GANADO","ALIMENTACION","SALUD","MANTENIMIENTO","SERVICIOS","OTRO").contains(categoria))
            throw new IllegalArgumentException("Categoría inválida.");
        if(concepto==null||concepto.isBlank()||concepto.trim().length()>200)
            throw new IllegalArgumentException("Escribe un concepto de hasta 200 caracteres.");
        importeValido(importe);
    }
    public long vender(LocalDate fecha,Long comprador,List<LineaVenta> lineas,long responsable) throws SQLException {
        fechaValida(fecha);
        if(lineas.isEmpty()) throw new IllegalArgumentException("Agrega al menos un animal a la venta.");
        Set<Long> ids=new HashSet<>();
        BigDecimal total=BigDecimal.ZERO;
        for(LineaVenta linea:lineas) {
            if(!ids.add(linea.animalId())) throw new IllegalArgumentException("Hay animales repetidos en la venta.");
            if(linea.pesoKg()!=null && (linea.pesoKg().signum()<=0||linea.pesoKg().scale()>2
                ||linea.pesoKg().compareTo(new BigDecimal("999999.99"))>0))
                throw new IllegalArgumentException("El peso de venta debe ser positivo y tener hasta dos decimales.");
            importeValido(linea.precio()); total=total.add(linea.precio());
        }
        if(total.compareTo(new BigDecimal("999999999999.99"))>0) throw new IllegalArgumentException("El total supera el máximo permitido.");
        return repo.vender(fecha,comprador,lineas.stream().sorted(Comparator.comparingLong(LineaVenta::animalId)).toList(),responsable);
    }
    private static void fechaValida(LocalDate fecha) {
        if(fecha==null||fecha.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha no puede ser futura.");
    }
    private static void importeValido(BigDecimal valor) {
        if(valor==null||valor.signum()<=0||valor.scale()>2||valor.compareTo(new BigDecimal("999999999999.99"))>0)
            throw new IllegalArgumentException("El importe debe ser positivo y tener hasta dos decimales.");
    }
}
