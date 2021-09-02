package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;

public class OpptjeningAktivitetVurderingOpptjeningsvilkår implements OpptjeningAktivitetVurdering {

    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type) {
        return switch (type) {
            // Automatisk godkjente opptjeningsaktiviteter mht opptjeningsvilkåret
            // - Arbeidsforhold registrert eller overstyrt gjennom AP 5080
            // - Egen næring oppgitt eller registrert
            // - Frilans oppgitt eller registrert
            case ARBEID, NÆRING, FRILANS -> VurderingsStatus.FERDIG_VURDERT_GODKJENT;
            // Alle andre opptjeningsaktiviteter underkjennes mht automatisk vilkårsvurdering
            default -> VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        };
    }

    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        return vurderStatus(input.getType()
        );
    }
}
