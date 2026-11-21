package ni.edu.uam.granja.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Pesaje(long id, long animalId, LocalDate fecha, BigDecimal pesoKg, String observaciones) {}
