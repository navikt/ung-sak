package no.nav.k9.sak.domene.opptjening;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OpptjeningAktivitetForBeregningVurdering implements OpptjeningAktivitetVurdering {

    private final OpptjeningResultat resultat;


    public OpptjeningAktivitetForBeregningVurdering(OpptjeningResultat resultat) {
        this.resultat = resultat;
    }

    /**
     * Returnerer automatisk eller manuell vurdering av opptjening for gitt aktivitet
     */
    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        var opptjening = resultat.finnOpptjening(input.getVilkårsperiode().getFomDato()).orElseThrow();
        return switch (input.getType()) {
            case ARBEID -> finnArbeidvurdering(input, opptjening);
            case MILITÆR_ELLER_SIVILTJENESTE ->
                VurderingsStatus.GODKJENT; // Skal alltid vurderes i beregning om det er oppgitt i søknad
            default -> finnStatusForType(input, opptjening);
        };
    }

    private VurderingsStatus finnStatusForType(VurderStatusInput input, Opptjening opptjening) {
        return opptjening.getOpptjeningAktivitet().stream().filter(oa -> oa.getAktivitetType().equals(input.getType()))
            .filter(oa -> input.getAktivitetPeriode().overlapper(oa.getFom(), oa.getTom()))
            .filter(oa -> DatoIntervallEntitet.fraOgMedTilOgMed(oa.getFom(), oa.getTom()).inkluderer(opptjening.getTom())) // finner vurdering dagen før skjæringstidspunktet
            .findFirst()
            .map(OpptjeningAktivitet::getKlassifisering)
            .map(OpptjeningAktivitetForBeregningVurdering::mapTilVurderingsStatus)
            .orElse(VurderingsStatus.UNDERKJENT);
    }

    private VurderingsStatus finnArbeidvurdering(VurderStatusInput input, Opptjening opptjening) {
        return opptjening.getOpptjeningAktivitet().stream().filter(oa ->
                oa.getAktivitetType().equals(OpptjeningAktivitetType.ARBEID) &&
                    oa.getAktivitetReferanse() != null &&
                    oa.getAktivitetReferanse().equals(input.getRegisterAktivitet().getArbeidsgiver().getIdentifikator()))
            .filter(oa -> input.getAktivitetPeriode().overlapper(oa.getFom(), oa.getTom()))
            .filter(oa -> DatoIntervallEntitet.fraOgMedTilOgMed(oa.getFom(), oa.getTom()).inkluderer(opptjening.getTom())) // finner vurdering dagen før skjæringstidspunktet
            .findFirst()
            .map(OpptjeningAktivitet::getKlassifisering)
            .map(OpptjeningAktivitetForBeregningVurdering::mapTilVurderingsStatus)
            .orElse(VurderingsStatus.UNDERKJENT);
    }

    private static VurderingsStatus mapTilVurderingsStatus(OpptjeningAktivitetKlassifisering klassifisering) {
        if (klassifisering.equals(OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT) || klassifisering.equals(OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)) {
            return VurderingsStatus.GODKJENT;
        } else {
            return VurderingsStatus.UNDERKJENT;
        }
    }
}
