package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "SykdomVurderingPeriode")
@Table(name = "SYKDOM_VURDERING_PERIODE")
public class SykdomVurderingPeriode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING_PERIODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERING_VERSJON_ID")
    private SykdomVurderingVersjon vurderingVersjon;

    @Column(name = "FOM", nullable = false)
    private LocalDate fom;

    @Column(name = "TOM", nullable = false)
    private LocalDate tom;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR


    SykdomVurderingPeriode() {

    }

    public SykdomVurderingPeriode(LocalDate fom, LocalDate tom, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this.fom = fom;
        this.tom = tom;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public SykdomVurderingPeriode(SykdomVurderingVersjon vurderingVersjon, LocalDate fom, LocalDate tom, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this(fom, tom, opprettetAv, opprettetTidspunkt);
        this.vurderingVersjon = vurderingVersjon;
    }


    public Long getId() {
        return id;
    }

    public SykdomVurderingVersjon getVurderingVersjon() {
        return vurderingVersjon;
    }

    void setVurderingVersjon(SykdomVurderingVersjon vurderingVersjon) {
        this.vurderingVersjon = vurderingVersjon;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
