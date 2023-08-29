package no.nav.k9.sak.ytelse.beregning.adapter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.InfotrygdFeriepengegrunnlag;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

public class MapBeregningsresultatFeriepengerFraVLTilRegel {

    private MapBeregningsresultatFeriepengerFraVLTilRegel() {
        //Skal ikke instansieres
    }


    public static BeregningsresultatFeriepengerRegelModell mapFra(BeregningsresultatEntitet beregningsresultat, LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> andelerSomKanGiFeriepengerForRelevaneSaker, InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag, int antallDagerFeriepenger, boolean feriepengeopptjeningForHelg, boolean ubegrensedeDagerVedRefusjon) {

        List<BeregningsresultatPeriode> beregningsresultatPerioder = mapBeregningsresultat(beregningsresultat);
        Set<Inntektskategori> inntektskategorier = mapInntektskategorier(beregningsresultat);

        return BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(beregningsresultatPerioder)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(andelerSomKanGiFeriepengerForRelevaneSaker)
            .medInfotrygdFeriepengegrunnlag(infotrygdFeriepengegrunnlag)
            .medInntektskategorier(inntektskategorier)
            .medAntallDagerFeriepenger(antallDagerFeriepenger)
            .medFeriepengeopptjeningForHelg(feriepengeopptjeningForHelg)
            .medUbegrensetFeriepengedagerVedRefusjon(ubegrensedeDagerVedRefusjon)
            .build();
    }

    public static List<BeregningsresultatPeriode> mapBeregningsresultat(BeregningsresultatEntitet beregningsresultat) {
        return beregningsresultat.getBeregningsresultatPerioder().stream()
            .map(MapBeregningsresultatFeriepengerFraVLTilRegel::mapBeregningsresultatPerioder)
            .collect(Collectors.toList());
    }

    private static Set<Inntektskategori> mapInntektskategorier(BeregningsresultatEntitet beregningsresultat) {
        return beregningsresultat.getBeregningsresultatPerioder().stream()
            .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
            .map(no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel::getInntektskategori)
            .map(InntektskategoriMapper::fraVLTilRegel)
            .collect(Collectors.toSet());
    }

    private static BeregningsresultatPeriode mapBeregningsresultatPerioder(no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode inputPeriode) {
        BeregningsresultatPeriode periode = new BeregningsresultatPeriode(
                inputPeriode.getBeregningsresultatPeriodeFom(),
            inputPeriode.getBeregningsresultatPeriodeTom(),
            inputPeriode.getInntektGraderingsprosent(),
            inputPeriode.getTotalUtbetalingsgradFraUttak(),
            inputPeriode.getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt(),
            inputPeriode.getGraderingsfaktorTid(),
            inputPeriode.getGraderingsfaktorInntekt()
        );
        inputPeriode.getBeregningsresultatAndelList().forEach(andel -> mapBeregningsresultatAndel(andel, periode));
        return periode;
    }

    private static void mapBeregningsresultatAndel(no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel andel, BeregningsresultatPeriode periode) {
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(andel.erBrukerMottaker())
            .medDagsats((long) andel.getDagsats())
            .medDagsatsFraBg((long) andel.getDagsatsFraBg())
            .medAktivitetStatus(AktivitetStatusMapper.fraVLTilRegel(andel.getAktivitetStatus()))
            .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(andel.getInntektskategori()))
            .medArbeidsforhold(mapArbeidsforhold(andel))
            .build(periode);
    }

    private static Arbeidsforhold mapArbeidsforhold(no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel andel) {
        if (andel.getAktivitetStatus().erFrilanser()) {
            return Arbeidsforhold.frilansArbeidsforhold();
        } else if (andel.getArbeidsgiver().isEmpty()) {
            return null;
        } else {
            return lagArbeidsforholdHosArbeidsgiver(andel.getArbeidsgiver().get(), andel.getArbeidsforholdRef());
        }
    }

    private static Arbeidsforhold lagArbeidsforholdHosArbeidsgiver(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        if (arbeidsgiver.erAktørId()) {
            return arbeidsforholdRef == null
                ? Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getIdentifikator())
                : Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getIdentifikator(), arbeidsforholdRef.getReferanse());
        } else if (arbeidsgiver.getErVirksomhet()) {
            return arbeidsforholdRef == null
                ? Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getIdentifikator())
                : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getIdentifikator(), arbeidsforholdRef.getReferanse());
        }
        throw new IllegalStateException("Arbeidsgiver har ingen av de forventede identifikatorene");
    }
}
