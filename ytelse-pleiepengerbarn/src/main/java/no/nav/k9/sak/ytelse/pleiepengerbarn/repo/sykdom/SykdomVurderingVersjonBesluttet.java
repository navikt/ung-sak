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
@Entity(name = "SykdomVurderingVersjonBesluttet")
@Table(name = "SYKDOM_VURDERING_VERSJON_BESLUTTET")
public class SykdomVurderingVersjonBesluttet {

    @Id
    @Column(name = "SYKDOM_VURDERING_VERSJON_ID", nullable = false)
    private Long id;

    @DiffIgnore
    @Column(name = "ENDRET_AV")
    private String endretAv;

    @DiffIgnore
    @Column(name = "ENDRET_TID")
    private LocalDateTime endretTidspunkt; // NOSONAR

    @OneToOne
    @JoinColumn(name = "SYKDOM_VURDERING_VERSJON_ID")
    private SykdomVurderingVersjon sykdomVurderingVersjon;

    SykdomVurderingVersjonBesluttet() {
        // hibernate
    }

    public SykdomVurderingVersjonBesluttet(String endretAv, LocalDateTime endretTidspunkt, SykdomVurderingVersjon sykdomVurderingVersjon) {
        this.endretAv = endretAv;
        this.endretTidspunkt = endretTidspunkt;
        this.sykdomVurderingVersjon = sykdomVurderingVersjon;
        this.id = sykdomVurderingVersjon.getId();
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

    public SykdomVurderingVersjon getSykdomVurderingVersjon() {
        return sykdomVurderingVersjon;
    }
}
