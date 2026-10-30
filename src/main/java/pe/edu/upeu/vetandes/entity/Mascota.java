package pe.edu.upeu.vetandes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mascota con restricción BD: (nombre, propietario_id) único — RN-02.
 */
@Entity
@Table(
        name = "mascotas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mascotas_nombre_propietario",
                columnNames = {"nombre", "propietario_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    /** CANINO | FELINO | AVIAR | EXOTICO */
    @Column(nullable = false, length = 20)
    private String especie;

    @Column(length = 80)
    private String raza;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "peso_kg", precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Propietario propietario;

    @Column(nullable = false)
    private Boolean estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @OneToMany(mappedBy = "mascota", fetch = FetchType.LAZY)
    private List<Atencion> atenciones = new ArrayList<>();

    @PrePersist
    void prePersist() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null) estado = true;
    }

    @PreUpdate
    void preUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}