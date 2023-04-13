package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Comparator;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingOpptjeningsvilkår;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Periode;

public class OpptjeningAktivitetResultatVurdering implements OpptjeningAktivitetVurdering {

    private final OpptjeningResultat resultat;

    private OpptjeningAktivitetVurderingOpptjeningsvilkår vilkårVurdering = new OpptjeningAktivitetVurderingOpptjeningsvilkår();

    public OpptjeningAktivitetResultatVurdering(OpptjeningResultat resultat) {
        this.resultat = resultat;
    }

    /**
     * Returnerer automatisk eller manuell vurdering av opptjening for gitt aktivitet
     */
    @Override
    public VurderingsStatus vurderStatus(VurderStatusInput input) {
        var opptjening = resultat.finnOpptjening(input.getVilkårsperiode().getFomDato()).orElseThrow();
        if (input.getType().equals(OpptjeningAktivitetType.ARBEID)) {
            return finnArbeidvurdering(input, opptjening);
        } else {
            return finnStatusForType(input, opptjening);
        }
    }

    private VurderingsStatus finnStatusForType(VurderStatusInput input, Opptjening opptjening) {
        var opptjeningsperiodeTom = opptjening.getTom();
        return opptjening.getOpptjeningAktivitet().stream().filter(oa -> oa.getAktivitetType().equals(input.getType()))
            .filter(oa -> input.getAktivitetPeriode().overlapper(oa.getFom(), oa.getTom()))
            .filter(oa -> DatoIntervallEntitet.fraOgMedTilOgMed(oa.getFom(), oa.getTom()).inkluderer(opptjeningsperiodeTom)) // finner vurdering dagen før skjæringstidspunktet
            .findFirst()
            .map(OpptjeningAktivitet::getKlassifisering)
            .map(OpptjeningAktivitetResultatVurdering::mapTilVurderingsStatus)
            .orElse(finnStatusUtenOpptjeningsaktivitet(input));
    }

    private VurderingsStatus finnStatusUtenOpptjeningsaktivitet(VurderStatusInput input) {
        var inputVurdering = vilkårVurdering.vurderStatus(input);
        return inputVurdering.equals(VurderingsStatus.UNDERKJENT) ? VurderingsStatus.UNDERKJENT : VurderingsStatus.GODKJENT;
    }

    private VurderingsStatus finnArbeidvurdering(VurderStatusInput input, Opptjening opptjening) {
        var opptjeningsperiodeTom = opptjening.getTom();
        return opptjening.getOpptjeningAktivitet().stream().filter(oa ->
                oa.getAktivitetType().equals(OpptjeningAktivitetType.ARBEID) &&
                    oa.getAktivitetReferanse() != null &&
                    oa.getAktivitetReferanse().equals(input.getRegisterAktivitet().getArbeidsgiver().getIdentifikator()))
            .filter(oa -> input.getAktivitetPeriode().overlapper(oa.getFom(), oa.getTom()))
            .filter(oa -> DatoIntervallEntitet.fraOgMedTilOgMed(oa.getFom(), oa.getTom()).inkluderer(opptjeningsperiodeTom)) // finner vurdering dagen før skjæringstidspunktet
            .findFirst()
            .map(OpptjeningAktivitet::getKlassifisering)
            .map(OpptjeningAktivitetResultatVurdering::mapTilVurderingsStatus)
            .orElse(finnStatusUtenOpptjeningsaktivitet(input));
    }

    private static VurderingsStatus mapTilVurderingsStatus(OpptjeningAktivitetKlassifisering klassifisering) {
        if (klassifisering.equals(OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT) || klassifisering.equals(OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)) {
            return VurderingsStatus.GODKJENT;
        } else {
            return VurderingsStatus.UNDERKJENT;
        }
    }
}
