package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity(name = "SykdomVurderingVersjonBesluttet")
@Table(name = "SYKDOM_VURDERING_VERSJON_BESLUTTET")
public class SykdomVurderingVersjonBesluttet {

    @Id
    @Column(name = "SYKDOM_VURDERING_VERSJON_ID", nullable = false)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "SYKDOM_VURDERING_VERSJON_ID")
    private SykdomVurderingVersjon sykdomVurderingVersjon;

    SykdomVurderingVersjonBesluttet() {
        // hibernate
    }

    public SykdomVurderingVersjon getSykdomVurderingVersjon() {
        return sykdomVurderingVersjon;
    }
}
