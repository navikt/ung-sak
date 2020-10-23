package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;

public interface OpptjeningAktivitetVurdering {

    VurderingsStatus vurderStatus(VurderStatusInput input);
}
