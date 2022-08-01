package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

//TODO: SykdomVurderingVersjonPeriode? Denne tilhører VurderingVersjon og ikke Vurdering
@Entity(name = "PleietrengendeSykdomVurderingPeriode")
@Table(name = "PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_PERIODE")
public class PleietrengendeSykdomVurderingVersjonPeriode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pleietrengende_sykdom_vurdering_versjon_periode")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ID")
    private PleietrengendeSykdomVurderingVersjon vurderingVersjon;

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


    PleietrengendeSykdomVurderingVersjonPeriode() {

    }

    public PleietrengendeSykdomVurderingVersjonPeriode(LocalDate fom, LocalDate tom, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this.fom = fom;
        this.tom = tom;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public PleietrengendeSykdomVurderingVersjonPeriode(PleietrengendeSykdomVurderingVersjon vurderingVersjon, LocalDate fom, LocalDate tom, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this(fom, tom, opprettetAv, opprettetTidspunkt);
        this.vurderingVersjon = vurderingVersjon;
    }


    public Long getId() {
        return id;
    }

    public PleietrengendeSykdomVurderingVersjon getVurderingVersjon() {
        return vurderingVersjon;
    }

    void setVurderingVersjon(PleietrengendeSykdomVurderingVersjon vurderingVersjon) {
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
