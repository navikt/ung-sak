package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.*;

@Entity(name = "SykdomVurderingVersjonBesluttet")
@Table(name = "SYKDOM_VURDERING_VERSJON_BESLUTTET")
public class SykdomVurderingVersjonBesluttet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING_VERSJON")
    private Long id;

    SykdomVurderingVersjonBesluttet() {
        // hibernate
    }

}
