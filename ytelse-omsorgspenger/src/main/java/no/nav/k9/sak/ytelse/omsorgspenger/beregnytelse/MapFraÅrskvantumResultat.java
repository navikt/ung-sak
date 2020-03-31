package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

class MapFraÅrskvantumResultat {
    private static final Comparator<UttakResultatPeriode> COMP_PERIODE = Comparator.comparing(per -> per.getPeriode(),
        Comparator.nullsFirst(Comparator.naturalOrder()));

    private static UttakAktivitet mapTilUttaksAktiviteter(UttaksperiodeOmsorgspenger uttaksperiode) {
        if (uttaksperiode.getUtfall() == OmsorgspengerUtfall.AVSLÅTT) {
            throw new IllegalArgumentException("Utvikler-feil: Støtter kun mapping av INNVILGET her, fikk= " + uttaksperiode);
        }
        
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Omsorgspenger, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent)
        
        BigDecimal utbetalingsgrad = uttaksperiode.getUtbetalingsgrad().getUtbetalingsgrad();

        var arb = uttaksperiode.getUtbetalingsgrad().getArbeidsforhold();
        var arbeidsforhold = mapArbeidsforhold(arb);
        return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, arb.getType(), erGradering);
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

    List<UttakResultatPeriode> mapFraÅrskvantum(ÅrskvantumResultat årskvantumResultat) {
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

        @SuppressWarnings({ "rawtypes", "unchecked" })
        LocalDateTimeline<List<UttakAktivitet>> timeline = new LocalDateTimeline(segmenter, StandardCombinators::allValues);
        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> {
            res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue(), false));
        });
        return res;
    }

    private static List<UttakResultatPeriode> getAvslåttTimeline(ÅrskvantumResultat årskvantumResultat) {
        List<LocalDateSegment<Boolean>> segmenter = årskvantumResultat.getUttaksperioder().stream()
            .filter(p -> p.getUtfall() == OmsorgspengerUtfall.AVSLÅTT)
            .map(e -> new LocalDateSegment<>(e.getFom(), e.getTom(), Boolean.TRUE))
            .collect(Collectors.toList());

        @SuppressWarnings({ "rawtypes", "unchecked" })
        LocalDateTimeline<List<UttakAktivitet>> timeline = new LocalDateTimeline(segmenter, StandardCombinators::allValues);
        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> {
            res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), Collections.emptyList(), true));
        });
        return res;
    }

}
