package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
//Besluttet = Godkjent av saksbehandler med beslutterrolle. Begrepet fordrer forklaring for nye folk.
// Klarer vi å finne på noe mer beskrivende?
@Entity(name = "PleietrengendeSykdomVurderingVersjonBesluttet")
@Table(name = "PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_BESLUTTET")
public class PleietrengendeSykdomVurderingVersjonBesluttet {

    @Id
    @Column(name = "PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ID", nullable = false)
    private Long id;

    @DiffIgnore
    @Column(name = "ENDRET_AV")
    private String endretAv;

    @DiffIgnore
    @Column(name = "ENDRET_TID")
    private LocalDateTime endretTidspunkt; // NOSONAR

    @OneToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ID")
    private PleietrengendeSykdomVurderingVersjon pleietrengendeSykdomVurderingVersjon;

    PleietrengendeSykdomVurderingVersjonBesluttet() {
        // hibernate
    }

    public PleietrengendeSykdomVurderingVersjonBesluttet(String endretAv, LocalDateTime endretTidspunkt, PleietrengendeSykdomVurderingVersjon pleietrengendeSykdomVurderingVersjon) {
        this.endretAv = endretAv;
        this.endretTidspunkt = endretTidspunkt;
        this.pleietrengendeSykdomVurderingVersjon = pleietrengendeSykdomVurderingVersjon;
        this.id = pleietrengendeSykdomVurderingVersjon.getId();
    }

    public Long getId() {
        return id;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }

    public PleietrengendeSykdomVurderingVersjon getPleietrengendeSykdomVurderingVersjon() {
        return pleietrengendeSykdomVurderingVersjon;
    }
}
