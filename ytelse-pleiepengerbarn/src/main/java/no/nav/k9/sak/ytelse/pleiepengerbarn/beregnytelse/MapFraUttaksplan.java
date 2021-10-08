package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

class MapFraUttaksplan {

    private static final Comparator<UttakResultatPeriode> COMP_PERIODE = Comparator.comparing(UttakResultatPeriode::getPeriode,
        Comparator.nullsFirst(Comparator.naturalOrder()));

    private static UttakAktivitet mapTilUttaksAktiviteter(Utbetalingsgrader data) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Pleiepenger barn, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent
        BigDecimal utbetalingsgrad = data.getUtbetalingsgrad();

        var type = UttakArbeidType.fraKode(data.getArbeidsforhold().getType());
        if (UttakArbeidType.IKKE_YRKESAKTIV.equals(type)) {
            type = UttakArbeidType.ARBEIDSTAKER;
        }
        Arbeidsforhold arbeidsforhold = buildArbeidsforhold(type, data);
        return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, type, erGradering);
    }

    private static Arbeidsforhold buildArbeidsforhold(UttakArbeidType type, Utbetalingsgrader data) {
        switch (type) {
            case IKKE_YRKESAKTIV:
                throw new IllegalArgumentException("IKKE_YRKESAKTIV skal mappes til ARBEIDSTAKER");
            case ARBEIDSTAKER:
                var arb = data.getArbeidsforhold();
                final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
                if (arb.getOrganisasjonsnummer() != null) {
                    arbeidsforholdBuilder.medOrgnr(arb.getOrganisasjonsnummer());
                } else if (arb.getAktørId() != null) {
                    arbeidsforholdBuilder.medAktørId(arb.getAktørId());
                }
                arbeidsforholdBuilder.medArbeidsforholdId(arb.getArbeidsforholdId());
                return arbeidsforholdBuilder.build();
            case FRILANSER:
                return Arbeidsforhold.frilansArbeidsforhold();
            default:
                return null;
        }
    }

    private static List<UttakResultatPeriode> getTimeline(Uttaksplan uttaksplan, Utfall utfall) {
        var segmenter = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .filter(it -> utfall.equals(it.getValue().getUtfall()))
            .map(e -> e.getValue().getUtbetalingsgrader().stream().map(it -> new LocalDateSegment<>(e.getKey().getFom(), e.getKey().getTom(), mapTilUttaksAktiviteter(it))).collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        var timeline = LocalDateTimeline.buildGroupOverlappingSegments(segmenter);

        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue(), !Utfall.OPPFYLT.equals(utfall))));
        return res;
    }

    List<UttakResultatPeriode> mapFra(Uttaksplan uttaksplan) {
        var innvilgetTimeline = getTimeline(uttaksplan, Utfall.OPPFYLT);
        var avslåttTimeline = getTimeline(uttaksplan, Utfall.IKKE_OPPFYLT);

        List<UttakResultatPeriode> res = new ArrayList<>();
        res.addAll(innvilgetTimeline);
        res.addAll(avslåttTimeline);
        res.sort(COMP_PERIODE);

        return res;
    }


}
