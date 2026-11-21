package ni.edu.uam.granja.model;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoFinanciero(long id,LocalDate fecha,String tipo,String categoria,
                                  String concepto,BigDecimal importe,Long ventaId) {}
