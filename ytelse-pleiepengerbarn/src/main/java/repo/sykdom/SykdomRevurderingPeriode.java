package repo.sykdom;

import javax.persistence.*;
import java.time.LocalDate;

@Entity(name = "SykdomRevurderingPeriode")
@Table(name = "SYKDOM_REVURDERING_PERIODE")
public class SykdomRevurderingPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_REVURDERING_PERIODE")
    private Long id;

    @Column(name = "FOM", nullable = false)
    private LocalDate fom;

    @Column(name = "TOM", nullable = false)
    private LocalDate tom;

}
