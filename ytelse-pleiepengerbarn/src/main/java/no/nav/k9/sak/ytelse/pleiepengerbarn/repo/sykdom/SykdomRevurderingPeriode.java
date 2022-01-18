package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "SykdomRevurderingPeriode")
@Table(name = "SYKDOM_REVURDERING_PERIODE")
public class SykdomRevurderingPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_REVURDERING_PERIODE")
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "SYKDOM_GRUNNLAG_ID", nullable = false)
    private SykdomGrunnlag sykdomGrunnlag;

    @Column(name = "FOM", nullable = false)
    private LocalDate fom;

    @Column(name = "TOM", nullable = false)
    private LocalDate tom;

    SykdomRevurderingPeriode() {
        //Hibernate
    }

    public SykdomRevurderingPeriode(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public Long getId() {
        return id;
    }
    
    public SykdomGrunnlag getSykdomGrunnlag() {
        return sykdomGrunnlag;
    }
    
    void setSykdomGrunnlag(SykdomGrunnlag sykdomGrunnlag) {
        this.sykdomGrunnlag = sykdomGrunnlag;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
