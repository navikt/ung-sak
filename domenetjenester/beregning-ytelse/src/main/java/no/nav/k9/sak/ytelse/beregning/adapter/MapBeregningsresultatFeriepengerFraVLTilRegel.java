package no.nav.k9.sak.ytelse.beregning.adapter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

public class MapBeregningsresultatFeriepengerFraVLTilRegel {


    MapBeregningsresultatFeriepengerFraVLTilRegel() {
        //Skal ikke instansieres
    }

    public static BeregningsresultatFeriepengerRegelModell mapFra(BeregningsresultatEntitet beregningsresultat, int antallDagerFeriepenger, boolean feriepengeopptjeningForHelg, boolean ubegrensedeDagerVedRefusjon) {

        List<BeregningsresultatPeriode> beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(MapBeregningsresultatFeriepengerFraVLTilRegel::mapBeregningsresultatPerioder).collect(Collectors.toList());
        Set<Inntektskategori> inntektskategorier = mapInntektskategorier(beregningsresultat);

        return BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(beregningsresultatPerioder)
                .medInntektskategorier(inntektskategorier)
                .medAntallDagerFeriepenger(antallDagerFeriepenger)
                .medFeriepengeopptjeningForHelg(feriepengeopptjeningForHelg)
                .medUbegrensetFeriepengedagerVedRefusjon(ubegrensedeDagerVedRefusjon)
                .build();
    }

    private static Set<Inntektskategori> mapInntektskategorier(BeregningsresultatEntitet beregningsresultat) {
        return beregningsresultat.getBeregningsresultatPerioder().stream()
                .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
                .map(no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel::getInntektskategori)
                .map(InntektskategoriMapper::fraVLTilRegel)
                .collect(Collectors.toSet());
    }

    private static BeregningsresultatPeriode mapBeregningsresultatPerioder(no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode perioder) {
        BeregningsresultatPeriode periode = new BeregningsresultatPeriode(perioder.getBeregningsresultatPeriodeFom(), perioder.getBeregningsresultatPeriodeTom());
        perioder.getBeregningsresultatAndelList().forEach(andel -> mapBeregningsresultatAndel(andel, periode));
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
