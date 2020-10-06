package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplanperiode;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakUtbetalingsgrad;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

class MapFraUttaksplan {

    private static UttakResultatPeriode toUttakResultatPeriode(LocalDate fom, LocalDate tom, Uttaksplanperiode uttak) {
        switch (uttak.getUtfall()) {
            case INNVILGET:
                return new UttakResultatPeriode(fom, tom, toUttakAktiviteter((InnvilgetUttaksplanperiode) uttak), false);
            case AVSLÅTT:
                return new UttakResultatPeriode(fom, tom, null, true); // TODO: indikerer opphold,bør ha med avslagsårsaker?
            default:
                throw new UnsupportedOperationException("Støtter ikke uttaksplanperiode av type: " + uttak);
        }
    }

    private static List<UttakAktivitet> toUttakAktiviteter(InnvilgetUttaksplanperiode uttaksplanperiode) {
        return uttaksplanperiode.getUtbetalingsgrader().stream()
            .map(MapFraUttaksplan::mapTilUttaksAktiviteter)
            .collect(Collectors.toList());
    }

    private static UttakAktivitet mapTilUttaksAktiviteter(UttakUtbetalingsgrad data) {
        BigDecimal stillingsgrad = BigDecimal.ZERO; // bruker ikke for Pleiepenger barn, bruker kun utbetalingsgrad
        boolean erGradering = false; // setter alltid false (bruker alltid utbetalingsgrad, framfor stillingsprosent
        BigDecimal utbetalingsgrad = data.getUtbetalingsgrad();

        var arb = data.getArbeidsforhold();
        final Arbeidsforhold.Builder arbeidsforholdBuilder = Arbeidsforhold.builder();
        if (arb.getOrganisasjonsnummer() != null) {
            arbeidsforholdBuilder.medOrgnr(arb.getOrganisasjonsnummer());
        } else if (arb.getAktørId() != null) {
            arbeidsforholdBuilder.medAktørId(arb.getAktørId().getId());
        }
        if (arb.getArbeidsforholdId() != null) {
            arbeidsforholdBuilder.medArbeidsforholdId(InternArbeidsforholdRef.ref(arb.getArbeidsforholdId()));
        }

        var arbeidsforhold = arbeidsforholdBuilder.build();
        return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, data.getArbeidsforhold().getType(), erGradering);
    }

    private static LocalDateTimeline<Uttaksplanperiode> getTimeline(Uttaksplan uttaksplan) {
        return new LocalDateTimeline<>(uttaksplan.getPerioder().entrySet().stream().map(e -> toSegment(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    private static LocalDateSegment<Uttaksplanperiode> toSegment(Periode periode, Uttaksplanperiode value) {
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
