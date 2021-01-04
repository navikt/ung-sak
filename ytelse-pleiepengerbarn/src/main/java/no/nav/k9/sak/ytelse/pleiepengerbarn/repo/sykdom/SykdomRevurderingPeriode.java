package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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

    public SykdomRevurderingPeriode(SykdomGrunnlag sykdomGrunnlag, LocalDate fom, LocalDate tom) {
        this.sykdomGrunnlag = sykdomGrunnlag;
        this.fom = fom;
        this.tom = tom;
    }

    public Long getId() {
        return id;
    }
    
    public SykdomGrunnlag getSykdomGrunnlag() {
        return sykdomGrunnlag;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
