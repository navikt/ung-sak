package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;

public class OpptjeningAktivitetVurderingBeregning implements OpptjeningAktivitetVurdering {

    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        return VurderingsStatus.GODKJENT;
    }
}
