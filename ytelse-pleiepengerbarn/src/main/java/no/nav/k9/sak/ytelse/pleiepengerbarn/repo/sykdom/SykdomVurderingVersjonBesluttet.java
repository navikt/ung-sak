package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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
