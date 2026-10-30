package pe.edu.upeu.vetandes.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.edu.upeu.vetandes.enums.EstadoPrestacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestaciones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Prestacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código único. Ej: PRE-001 */
    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /**
     * Especie a la que aplica el servicio.
     * Valores: TODAS | CANINO | FELINO | AVIAR | EXOTICO
     */
    @Column(name = "especie_aplicable", nullable = false, length = 20)
    private String especieAplicable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPrestacion estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    void prePersist() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null) estado = EstadoPrestacion.ACTIVA;
    }

    @PreUpdate
    void preUpdate() {
        fechaModificacion = LocalDateTime.now();
    }

    public boolean estaActiva() {
        return EstadoPrestacion.ACTIVA == estado;
    }

    /** RN-03: compatible con la especie de la mascota */
    public boolean esCompatibleCon(String especie) {
        return "TODAS".equalsIgnoreCase(especieAplicable)
                || especieAplicable.equalsIgnoreCase(especie);
    }
}