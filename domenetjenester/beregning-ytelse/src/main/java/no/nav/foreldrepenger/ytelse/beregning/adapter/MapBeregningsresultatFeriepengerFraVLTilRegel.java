package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class MapBeregningsresultatFeriepengerFraVLTilRegel {


    MapBeregningsresultatFeriepengerFraVLTilRegel() {
        //Skal ikke instansieres
    }

    public static BeregningsresultatFeriepengerRegelModell mapFra(BeregningsresultatEntitet beregningsresultat, int antallDagerFeriepenger) {

        List<BeregningsresultatPeriode> beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(MapBeregningsresultatFeriepengerFraVLTilRegel::mapBeregningsresultatPerioder).collect(Collectors.toList());
        Set<Inntektskategori> inntektskategorier = mapInntektskategorier(beregningsresultat);

        return BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(beregningsresultatPerioder)
                .medAnnenPartsInntektskategorier(Collections.emptySet())
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medAnnenPartsBeregningsresultatPerioder(Collections.emptyList())
                .medInntektskategorier(inntektskategorier)
                .medAntallDagerFeriepenger(antallDagerFeriepenger)
                .build();
    }

    private static Set<Inntektskategori> mapInntektskategorier(BeregningsresultatEntitet beregningsresultat) {
        return beregningsresultat.getBeregningsresultatPerioder().stream()
                .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
                .map(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel::getInntektskategori)
                .map(InntektskategoriMapper::fraVLTilRegel)
                .collect(Collectors.toSet());
    }

    private static BeregningsresultatPeriode mapBeregningsresultatPerioder(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode beregningsresultatPerioder) {
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
                .medPeriode(new LocalDateInterval(beregningsresultatPerioder.getBeregningsresultatPeriodeFom(), beregningsresultatPerioder.getBeregningsresultatPeriodeTom()))
                .build();
        beregningsresultatPerioder.getBeregningsresultatAndelList().forEach(andel -> mapBeregningsresultatAndel(andel, periode));
        return periode;
    }

    private static void mapBeregningsresultatAndel(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel andel, BeregningsresultatPeriode periode) {
        BeregningsresultatAndel.builder()
                .medBrukerErMottaker(andel.erBrukerMottaker())
                .medDagsats((long) andel.getDagsats())
                .medDagsatsFraBg((long) andel.getDagsatsFraBg())
                .medAktivitetStatus(AktivitetStatusMapper.fraVLTilRegel(andel.getAktivitetStatus()))
                .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(andel.getInntektskategori()))
                .medArbeidsforhold(mapArbeidsforhold(andel))
                .build(periode);
    }

    private static Arbeidsforhold mapArbeidsforhold(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel andel) {
        if (andel.getAktivitetStatus().erFrilanser()) {
            return Arbeidsforhold.frilansArbeidsforhold();
        } else if (!andel.getArbeidsgiver().isPresent()) {
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
