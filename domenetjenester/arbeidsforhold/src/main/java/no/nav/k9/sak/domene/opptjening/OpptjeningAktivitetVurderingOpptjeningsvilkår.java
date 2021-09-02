package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;

public class OpptjeningAktivitetVurderingOpptjeningsvilkår implements OpptjeningAktivitetVurdering {

    public OpptjeningAktivitetVurderingOpptjeningsvilkår() {
    }

    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type) {

        if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            // Registrert eller overstyrt arbeidsforhold (AP 5080) godkjennes. Oppgitt arbeidsforhold (innenlands) skal ikke forekomme, da det ikke lagres på IAY
            return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
        } else if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            // Både oppgitt og registrert selvstendig næringsdrivende godkjennes som opptjeningsaktivitet
            return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
        } else if (OpptjeningAktivitetType.FRILANS.equals(type)) {
            // Både oppgitt og registrert frilans godkjennes som opptjeningsaktivitet
            return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
        } else if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            // Oppgitt annen aktivitet underkjennes som opptjeningsaktivitet (mht automatisk vilkårsvurdering)
            return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        } else if (OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD.equals(type)) {
            // Oppgitt utenlandsk arbeidsforhold underkjennes som opptjeningsaktivitet (mht automatisk vilkårsvurdering)
            return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        }
        // Alle andre opptjeningsaktiviteter underkjennes (mht automatisk vilkårsvurdering)
        return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
    }

    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        return vurderStatus(input.getType()
        );
    }
}
