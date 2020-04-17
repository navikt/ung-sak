package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.UttaksPlanOmsorgspengerAktivitet;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class MapFraÅrskvantumResultat {
    private static final Comparator<UttakResultatPeriode> COMP_PERIODE = Comparator.comparing(per -> per.getPeriode(),
        Comparator.nullsFirst(Comparator.naturalOrder()));

    private static UttakAktivitet mapTilUttaksAktiviteter(UttaksperiodeOmsorgspenger uttaksperiode, UttakArbeidsforhold uttakArbeidsforhold) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Omsorgspenger, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent)

        if (uttaksperiode.getUtbetalingsgrad() == null) {
            return new UttakAktivitet(stillingsgrad, BigDecimal.ZERO, null, null, false);
        } else {
            var utbetalingsgrad = uttaksperiode.getUtbetalingsgrad();
            var arbeidsforhold = mapArbeidsforhold(uttakArbeidsforhold);
            return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, uttakArbeidsforhold.getType(), erGradering);
        }

    }

    private static Arbeidsforhold mapArbeidsforhold(UttakArbeidsforhold arb) {
        final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
        if (arb.getOrganisasjonsnummer() != null) {
            arbeidsforholdBuilder.medOrgnr(arb.getOrganisasjonsnummer());
        } else if (arb.getAktørId() != null) {
            arbeidsforholdBuilder.medAktørId(arb.getAktørId().getId());
        }

        var arbeidsforhold = arbeidsforholdBuilder.build();
        return arbeidsforhold;
    }

    List<UttakResultatPeriode> mapFra(ÅrskvantumResultat årskvantumResultat) {
        List<UttakResultatPeriode> res = new ArrayList<>();
        res.addAll(getInnvilgetTimeline(årskvantumResultat));
        res.addAll(getAvslåttTimeline(årskvantumResultat));
        Collections.sort(res, COMP_PERIODE);
        return res;
    }

    private static List<UttakResultatPeriode> getInnvilgetTimeline(ÅrskvantumResultat årskvantumResultat) {
        List<LocalDateSegment<UttakAktivitet>> segmenter = new ArrayList<>();
        for (UttaksPlanOmsorgspengerAktivitet aktivitet : årskvantumResultat.getUttaksplan().getAktiviteter()) {
            for (UttaksperiodeOmsorgspenger p : aktivitet.getUttaksperioder()) {
                if (p.getUtfall() == OmsorgspengerUtfall.INNVILGET) {
                    LocalDateSegment<UttakAktivitet> uttakAktivitetLocalDateSegment =
                        new LocalDateSegment<>(p.getFom(), p.getPeriode().getTom(), mapTilUttaksAktiviteter(p, aktivitet.getArbeidsforhold()));
                    segmenter.add(uttakAktivitetLocalDateSegment);
                }
            }
        }

        var timeline = LocalDateTimeline.buildGroupOverlappingSegments(segmenter);

        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> {
            res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue(), false));
        });
        return res;
    }

    private static List<UttakResultatPeriode> getAvslåttTimeline(ÅrskvantumResultat årskvantumResultat) {
        List<LocalDateSegment<UttakAktivitet>> segmenter = new ArrayList<>();
        for (UttaksPlanOmsorgspengerAktivitet aktivitet : årskvantumResultat.getUttaksplan().getAktiviteter()) {
            for (UttaksperiodeOmsorgspenger p : aktivitet.getUttaksperioder()) {
                if (p.getUtfall() == OmsorgspengerUtfall.AVSLÅTT) {
                    LocalDateSegment<UttakAktivitet> uttakAktivitetLocalDateSegment =
                        new LocalDateSegment<>(p.getFom(), p.getPeriode().getTom(), mapTilUttaksAktiviteter(p, aktivitet.getArbeidsforhold()));
                    segmenter.add(uttakAktivitetLocalDateSegment);
                }
            }
        }

        var timeline = LocalDateTimeline.buildGroupOverlappingSegments(segmenter);

        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> {
            res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue(), true));
        });
        return res;
    }

}
