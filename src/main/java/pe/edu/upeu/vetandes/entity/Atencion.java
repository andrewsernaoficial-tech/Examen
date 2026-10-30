package pe.edu.upeu.vetandes.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.edu.upeu.vetandes.enums.EstadoAtencion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de atención veterinaria.
 *
 * <ul>
 *   <li>Número: AT-000001 (generado en el servicio)</li>
 *   <li>Estado inicial: REGISTRADA</li>
 *   <li>Descuento 10 % automático si ≥ 3 prestaciones distintas (RN-03/04)</li>
 * </ul>
 */
@Entity
@Table(name = "atenciones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Atencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** AT-000001, AT-000002 … */
    @Column(nullable = false, length = 10, unique = true)
    private String numero;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Peso de la mascota al momento de la atención (> 0) */
    @Column(name = "peso_mascota_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoMascotaKg;

    @Column(length = 500)
    private String observaciones;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAtencion estado;

    /** RN-05: motivo de anulación (10-200 caracteres) */
    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @OneToMany(
            mappedBy    = "atencion",
            cascade     = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DetalleAtencion> detalles = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
        if (estado    == null) estado    = EstadoAtencion.REGISTRADA;
    }

    public void agregarDetalle(DetalleAtencion detalle) {
        detalles.add(detalle);
        detalle.setAtencion(this);
    }
}