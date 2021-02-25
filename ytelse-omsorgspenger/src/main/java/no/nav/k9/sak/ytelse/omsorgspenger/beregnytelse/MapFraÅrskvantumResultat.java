package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

class MapFraÅrskvantumResultat {
    private static final Comparator<UttakResultatPeriode> COMP_PERIODE = Comparator.comparing(per -> per.getPeriode(),
        Comparator.nullsFirst(Comparator.naturalOrder()));

    private static UttakAktivitet mapTilUttaksAktiviteter(Uttaksperiode uttaksperiode, no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold uttakArbeidsforhold) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Omsorgspenger, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent)

        if (uttaksperiode.getUtbetalingsgrad() == null) {
            return new UttakAktivitet(stillingsgrad, BigDecimal.ZERO, null, null, false);
        } else {
            var utbetalingsgrad = uttaksperiode.getUtbetalingsgrad();
            var arbeidsforhold = mapArbeidsforhold(uttakArbeidsforhold);
            return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, UttakArbeidType.fraKode(uttakArbeidsforhold.getType()), erGradering);
        }

    }

    static Arbeidsforhold mapArbeidsforhold(no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold arb) {
        final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
        if (arb.getOrganisasjonsnummer() != null) {
            arbeidsforholdBuilder.medOrgnr(arb.getOrganisasjonsnummer());
        } else if (arb.getAktørId() != null) {
            arbeidsforholdBuilder.medAktørId(arb.getAktørId());
        }
        arbeidsforholdBuilder.medArbeidsforholdId(arb.getArbeidsforholdId());
        arbeidsforholdBuilder.medFrilanser("FL".equals(arb.getType()));
        return arbeidsforholdBuilder.build();
    }

    private static List<UttakResultatPeriode> getInnvilgetTimeline(List<Aktivitet> aktiviteter) {
        List<LocalDateSegment<UttakAktivitet>> segmenter = new ArrayList<>();
        for (Aktivitet aktivitet : aktiviteter) {
            for (Uttaksperiode p : aktivitet.getUttaksperioder()) {
                if (p.getUtfall() == Utfall.INNVILGET) {
                    LocalDateSegment<UttakAktivitet> uttakAktivitetLocalDateSegment =
                        new LocalDateSegment<>(p.component1().getFom(), p.getPeriode().getTom(), mapTilUttaksAktiviteter(p, aktivitet.getArbeidsforhold()));
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

    private static List<UttakResultatPeriode> getAvslåttTimeline(List<Aktivitet> aktiviteter) {
        List<LocalDateSegment<UttakAktivitet>> segmenter = new ArrayList<>();
        for (Aktivitet aktivitet : aktiviteter) {
            for (Uttaksperiode p : aktivitet.getUttaksperioder()) {
                if (p.getUtfall() == Utfall.AVSLÅTT) {
                    LocalDateSegment<UttakAktivitet> uttakAktivitetLocalDateSegment =
                        new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), mapTilUttaksAktiviteter(p, aktivitet.getArbeidsforhold()));
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

    List<UttakResultatPeriode> mapFra(List<Aktivitet> aktiviteter) {
        List<UttakResultatPeriode> res = new ArrayList<>();
        res.addAll(getInnvilgetTimeline(aktiviteter));
        res.addAll(getAvslåttTimeline(aktiviteter));
        res.sort(COMP_PERIODE);
        return res;
    }

}
