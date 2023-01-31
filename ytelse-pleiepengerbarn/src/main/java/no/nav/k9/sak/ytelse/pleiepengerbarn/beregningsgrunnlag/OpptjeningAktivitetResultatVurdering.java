package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingOpptjeningsvilkår;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;

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
        return opptjening.getOpptjeningAktivitet().stream().filter(oa -> oa.getAktivitetType().equals(input.getType()))
            .filter(oa -> input.getAktivitetPeriode().overlapper(oa.getFom(), oa.getTom()))
            .max(Comparator.comparing(OpptjeningAktivitet::getFom)) // finner siste vurdering siden vi egentlig kun er interessert i vurdering på skjæringtidspunktet
            .map(OpptjeningAktivitet::getKlassifisering)
            .map(OpptjeningAktivitetResultatVurdering::mapTilVurderingsStatus)
            .orElse(vilkårVurdering.vurderStatus(input));
    }

    private VurderingsStatus finnArbeidvurdering(VurderStatusInput input, Opptjening opptjening) {
        return opptjening.getOpptjeningAktivitet().stream().filter(oa ->
                oa.getAktivitetType().equals(OpptjeningAktivitetType.ARBEID) &&
                    oa.getAktivitetReferanse() != null &&
                    oa.getAktivitetReferanse().equals(input.getRegisterAktivitet().getArbeidsgiver().getIdentifikator()))
            .filter(oa -> input.getAktivitetPeriode().overlapper(oa.getFom(), oa.getTom()))
            .max(Comparator.comparing(OpptjeningAktivitet::getFom)) // finner siste vurdering siden vi egentlig kun er interessert i vurdering på skjæringtidspunktet
            .map(OpptjeningAktivitet::getKlassifisering)
            .map(OpptjeningAktivitetResultatVurdering::mapTilVurderingsStatus)
            .orElse(vilkårVurdering.vurderStatus(input));
    }

    private static VurderingsStatus mapTilVurderingsStatus(OpptjeningAktivitetKlassifisering klassifisering) {
        if (klassifisering.equals(OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT) || klassifisering.equals(OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)) {
            return VurderingsStatus.GODKJENT;
        } else {
            return VurderingsStatus.UNDERKJENT;
        }
    }
}
