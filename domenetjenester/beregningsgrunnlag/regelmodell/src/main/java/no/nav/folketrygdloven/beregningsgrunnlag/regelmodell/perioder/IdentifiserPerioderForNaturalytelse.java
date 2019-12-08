package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.perioder;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.ArbeidsforholdOgInntektsmelding;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.PeriodeSplittData;

class IdentifiserPerioderForNaturalytelse {
    private IdentifiserPerioderForNaturalytelse() {
        // skjul public constructor
    }

    static Set<PeriodeSplittData> identifiserPerioderForNaturalytelse(ArbeidsforholdOgInntektsmelding inntektsmelding, LocalDate skjæringstidspunkt) {
        Set<PeriodeSplittData> set = new HashSet<>();
        inntektsmelding.getNaturalYtelser().forEach(naturalYtelse -> {
            LocalDate naturalYtelseFom = naturalYtelse.getFom();
            PeriodeSplittData.Builder builder = PeriodeSplittData.builder().medInntektsmelding(inntektsmelding);
            if (naturalYtelseFom.isAfter(skjæringstidspunkt)) {
                PeriodeSplittData splittData = builder.medPeriodeÅrsak(PeriodeÅrsak.NATURALYTELSE_TILKOMMER)
                    .medFom(naturalYtelseFom)
                    .build();
                set.add(splittData);
            }
            LocalDate naturalYtelseTom = naturalYtelse.getTom();
            LocalDate opphørsdato = naturalYtelseTom.plusDays(1);
            if (!naturalYtelseTom.equals(TIDENES_ENDE) && opphørsdato.isAfter(skjæringstidspunkt)) {
                PeriodeSplittData splittData = builder.medPeriodeÅrsak(PeriodeÅrsak.NATURALYTELSE_BORTFALT)
                    .medFom(opphørsdato)
                    .build();
                set.add(splittData);
            }
        });
        return set;
    }
}
