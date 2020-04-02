package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Map;
import java.util.Set;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public class OpptjeningsaktiviteterPerYtelse {

    private static final Map<FagsakYtelseType, Set<OpptjeningAktivitetType>> EKSKLUDERTE_AKTIVITETER_PER_YTELSE = Map.of(
        FagsakYtelseType.FORELDREPENGER, Set.of(
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD),
        FagsakYtelseType.SVANGERSKAPSPENGER, Set.of(
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
            OpptjeningAktivitetType.DAGPENGER,
            OpptjeningAktivitetType.ARBEIDSAVKLARING,
            OpptjeningAktivitetType.VENTELØNN_VARTPENGER,
            OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE),
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN, Set.of(
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
            OpptjeningAktivitetType.ARBEIDSAVKLARING),
        FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, Set.of(
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
            OpptjeningAktivitetType.ARBEIDSAVKLARING),
        FagsakYtelseType.OPPLÆRINGSPENGER, Set.of(
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
            OpptjeningAktivitetType.ARBEIDSAVKLARING),
        FagsakYtelseType.OMSORGSPENGER, Set.of(
            OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
            OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
            OpptjeningAktivitetType.ARBEIDSAVKLARING)
        );

    // FIXME K9 Oopptjening fra foreldrepenger skal ekskluderes hvis denne ble godkjent kun på basis av AAP.

    private Set<OpptjeningAktivitetType> ekskluderteAktiviteter;

    public OpptjeningsaktiviteterPerYtelse(FagsakYtelseType fagsakYtelseType) {
        ekskluderteAktiviteter = EKSKLUDERTE_AKTIVITETER_PER_YTELSE.get(fagsakYtelseType);
    }

    public boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (OpptjeningAktivitetType.FRILANS.equals(opptjeningAktivitetType)) {
            return harOppgittFrilansISøknad(iayGrunnlag);
        }
        return erRelevantAktivitet(opptjeningAktivitetType);
    }

    boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType) {
        return !ekskluderteAktiviteter.contains(opptjeningAktivitetType);
    }

    private boolean harOppgittFrilansISøknad(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getOppgittOpptjening().stream()
            .flatMap(oppgittOpptjening -> oppgittOpptjening.getAnnenAktivitet().stream())
            .anyMatch(annenAktivitet -> annenAktivitet.getArbeidType().equals(ArbeidType.FRILANSER));
    }
}
