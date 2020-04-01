package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
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
import java.util.stream.Collectors;

class MapFraÅrskvantumResultat {
    private static final Comparator<UttakResultatPeriode> COMP_PERIODE = Comparator.comparing(per -> per.getPeriode(),
        Comparator.nullsFirst(Comparator.naturalOrder()));

    private static UttakAktivitet mapTilUttaksAktiviteter(UttaksperiodeOmsorgspenger uttaksperiode) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Omsorgspenger, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent)

        if (uttaksperiode.getUtbetalingsgrad() == null) {
            return new UttakAktivitet(stillingsgrad, BigDecimal.ZERO, null, null, false);
        } else {
            var utbetalingsgrad = uttaksperiode.getUtbetalingsgrad().getUtbetalingsgrad();
            var arb = uttaksperiode.getUttakArbeidsforhold();
            var arbeidsforhold = mapArbeidsforhold(arb);
            return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, arb.getType(), erGradering);
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
        List<LocalDateSegment<UttakAktivitet>> segmenter = årskvantumResultat.getUttaksperioder().stream()
            .filter(p -> p.getUtfall() == OmsorgspengerUtfall.INNVILGET)
            .map(e -> new LocalDateSegment<>(e.getFom(), e.getPeriode().getTom(), mapTilUttaksAktiviteter(e)))
            .collect(Collectors.toList());

        var timeline = LocalDateTimeline.buildGroupOverlappingSegments(segmenter);

        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> {
            res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue(), false));
        });
        return res;
    }

    private static List<UttakResultatPeriode> getAvslåttTimeline(ÅrskvantumResultat årskvantumResultat) {
        List<LocalDateSegment<UttakAktivitet>> segmenter = årskvantumResultat.getUttaksperioder().stream()
            .filter(p -> p.getUtfall() == OmsorgspengerUtfall.AVSLÅTT)
            .map(e -> new LocalDateSegment<>(e.getFom(), e.getPeriode().getTom(), mapTilUttaksAktiviteter(e)))
            .collect(Collectors.toList());

        var timeline = LocalDateTimeline.buildGroupOverlappingSegments(segmenter);

        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> {
            res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue(), true));
        });
        return res;
    }

}
