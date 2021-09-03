package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;

public class OpptjeningAktivitetVurderingOpptjeningsvilkår implements OpptjeningAktivitetVurdering {

    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        return switch (input.getType()) {
            // Automatisk godkjente opptjeningsaktiviteter mht opptjeningsvilkåret
            // - Arbeidsforhold registrert eller overstyrt gjennom AP 5080
            // - Egen næring oppgitt eller registrert
            // - Frilans oppgitt eller registrert
            case ARBEID, NÆRING, FRILANS -> VurderingsStatus.TIL_VURDERING;
            // Alle andre opptjeningsaktiviteter underkjennes mht automatisk vilkårsvurdering
            default -> VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        };
    }
}
