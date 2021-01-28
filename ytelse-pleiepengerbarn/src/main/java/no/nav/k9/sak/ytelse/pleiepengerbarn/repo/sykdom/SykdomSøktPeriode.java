package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.*;
import java.time.LocalDate;

@Entity(name = "SykdomSøktPeriode")
@Table(name = "SYKDOM_SOEKT_PERIODE")
public class SykdomSøktPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_SOEKT_PERIODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_GRUNNLAG_ID", nullable = false)
    private SykdomGrunnlag sykdomGrunnlag;

    @Column(name = "FOM", nullable = false)
    private LocalDate fom;

    @Column(name = "TOM", nullable = false)
    private LocalDate tom;

    public SykdomSøktPeriode() {
        // Hibernate
    }

    public SykdomSøktPeriode(SykdomGrunnlag sykdomGrunnlag, LocalDate fom, LocalDate tom) {
        this.sykdomGrunnlag = sykdomGrunnlag;
        this.fom = fom;
        this.tom = tom;
    }

    public SykdomGrunnlag getSykdomGrunnlag() {
        return sykdomGrunnlag;
    }

    public void setSykdomGrunnlag(SykdomGrunnlag sykdomGrunnlag) {
        this.sykdomGrunnlag = sykdomGrunnlag;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
