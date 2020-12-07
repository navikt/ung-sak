package repo.sykdom;

import javax.persistence.*;
import java.time.LocalDate;

@Entity(name = "SykdomSøktPeriode")
@Table(name = "SYKDOM_SOEKT_PERIODE")
public class SykdomSøktPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_SOEKT_PERIODE")
    private Long id;

    @Column(name = "FOM", nullable = false)
    private LocalDate fom;

    @Column(name = "TOM", nullable = false)
    private LocalDate tom;

}
