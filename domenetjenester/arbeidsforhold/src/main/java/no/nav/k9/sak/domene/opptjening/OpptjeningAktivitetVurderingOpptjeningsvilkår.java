package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;

public class OpptjeningAktivitetVurderingOpptjeningsvilkår implements OpptjeningAktivitetVurdering {

    OpptjeningAktivitetArbeidVurderer arbeidVurderer = new OpptjeningAktivitetArbeidVurderer();

    /** Returnerer VurderingsStatus for en opptjeningsaktivitet. Statusen brukes av maskinell vurdering av opptjeningsvilkåret
     *
     * En opptjeningsaktivitet kan utledes fra:
     *  1) oppgitte opplysninger
     *  2) registeropplysninger
     *  3) manuell vurdering (kun for arbeidsforhold)
     */
    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        return switch (input.getType()) {
            case ARBEID -> arbeidVurderer.vurderArbeid(input);
            case NÆRING, FRILANS -> VurderingsStatus.TIL_VURDERING;
            // Ytelser til livsopphold fra NAV likestilles med arbeidsaktivitet
            case ARBEIDSAVKLARING, DAGPENGER, FORELDREPENGER, FRISINN, OMSORGSPENGER, OPPLÆRINGSPENGER, PLEIEPENGER, SVANGERSKAPSPENGER, SYKEPENGER, SYKEPENGER_AV_DAGPENGER -> VurderingsStatus.TIL_VURDERING;
            // Alle andre opptjeningsaktiviteter underkjennes mht automatisk vilkårsvurdering
            default -> VurderingsStatus.UNDERKJENT;
        };
    }
}
