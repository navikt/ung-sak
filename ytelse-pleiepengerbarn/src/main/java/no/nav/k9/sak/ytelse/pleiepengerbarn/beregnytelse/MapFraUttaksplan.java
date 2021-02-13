package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

class MapFraUttaksplan {

    private static UttakResultatPeriode toUttakResultatPeriode(LocalDate fom, LocalDate tom, UttaksperiodeInfo uttak) {
        switch (uttak.getUtfall()) {
            case OPPFYLT:
                return new UttakResultatPeriode(fom, tom, toUttakAktiviteter(uttak), false);
            case IKKE_OPPFYLT:
                return new UttakResultatPeriode(fom, tom, null, true); // TODO: indikerer opphold,bør ha med avslagsårsaker?
            default:
                throw new UnsupportedOperationException("Støtter ikke uttaksplanperiode av type: " + uttak);
        }
    }

    private static List<UttakAktivitet> toUttakAktiviteter(UttaksperiodeInfo uttaksplanperiode) {
        return uttaksplanperiode.getUtbetalingsgrader().stream()
            .map(MapFraUttaksplan::mapTilUttaksAktiviteter)
            .collect(Collectors.toList());
    }

    private static UttakAktivitet mapTilUttaksAktiviteter(Utbetalingsgrader data) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Pleiepenger barn, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent
        BigDecimal utbetalingsgrad = data.getUtbetalingsgrad();

        var arb = data.getArbeidsforhold();
        final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
        if (arb.getOrganisasjonsnummer() != null) {
            arbeidsforholdBuilder.medOrgnr(arb.getOrganisasjonsnummer());
        } else if (arb.getAktørId() != null) {
            arbeidsforholdBuilder.medAktørId(arb.getAktørId());
        }

        var arbeidsforhold = arbeidsforholdBuilder.build();
        return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, UttakArbeidType.fraKode(data.getArbeidsforhold().getType()), erGradering);
    }

    private static LocalDateTimeline<UttaksperiodeInfo> getTimeline(Uttaksplan uttaksplan) {
        return new LocalDateTimeline<>(uttaksplan.getPerioder().entrySet().stream().map(e -> toSegment(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    private static LocalDateSegment<UttaksperiodeInfo> toSegment(LukketPeriode periode, UttaksperiodeInfo value) {
        return new LocalDateSegment<>(periode.getFom(), periode.getTom(), value);
    }

    List<UttakResultatPeriode> mapFra(Uttaksplan uttaksplan) {
        var uttakTimeline = getTimeline(uttaksplan);
        List<UttakResultatPeriode> res = new ArrayList<>();

        uttakTimeline.toSegments().forEach(seg -> {
            res.add(toUttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue()));
        });
        return res;
    }


}
