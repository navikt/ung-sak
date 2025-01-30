package no.nav.ung.sak.domene.behandling.steg.uttak;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.VurderAntallDagerResultat;

class VurderUttakTjeneste {


    static Optional<UngdomsytelseUttakPerioder> vurderUttak(LocalDateTimeline<Boolean> godkjentePerioder,
                                                            LocalDateTimeline<Boolean> ungdomsprogramtidslinje,
                                                            Optional<LocalDate> søkersDødsdato) {
        if (godkjentePerioder.isEmpty()) {
            return Optional.empty();
        }

        var levendeBrukerTidslinje = søkersDødsdato.map(d -> new LocalDateTimeline<>(TIDENES_BEGYNNELSE, d, true)).orElse(new LocalDateTimeline<>(TIDENES_BEGYNNELSE, TIDENES_ENDE, true));

        final var regelSporingBuilder = new RegelSporingBuilder();

        // Finner tidslinje med nok dager tilgjengelig
        var perioderMedNokDagerResultat = FinnForbrukteDager.finnForbrukteDager(ungdomsprogramtidslinje);
        regelSporingBuilder.medPeriodeNokDager(perioderMedNokDagerResultat.tidslinjeNokDager());
        regelSporingBuilder.medAntallForbrukteDager(perioderMedNokDagerResultat.forbrukteDager());

        // VURDERING AV PERIODER MED NOK DAGER: Perioder med nok dager får 100% utbetaling enn så lenge
        final var nokDagerDelresultat = finnResultatNokDager(perioderMedNokDagerResultat.tidslinjeNokDager(), godkjentePerioder, levendeBrukerTidslinje);
        final var uttakPerioder = new ArrayList<>(nokDagerDelresultat.resultatPerioder());

        // VURDERING AV AVSLAG ETTER DØD: Perioder med nok dager etter søkers dødsfall får 0% i utbetaling
        final var uttakAvslagEtterSøkersDødDelResultat = finnUttaksperioderAvslagEtterDød(nokDagerDelresultat.restTidslinjeTilVurdering(), levendeBrukerTidslinje, regelSporingBuilder);
        uttakPerioder.addAll(uttakAvslagEtterSøkersDødDelResultat.resultatPerioder());

        // VURDERING AV OPPBRUKT KVOTE: Perioder etter kvote er brukt opp får 0% utbetaling
        final var ikkeNokDagerPeriodeDelResultat = finnIkkeNokDagerPerioder(uttakAvslagEtterSøkersDødDelResultat.restTidslinjeTilVurdering(), perioderMedNokDagerResultat.tidslinjeNokDager(), regelSporingBuilder);
        uttakPerioder.addAll(ikkeNokDagerPeriodeDelResultat.resultatPerioder());


        var ungdomsytelseUttakPerioder = new UngdomsytelseUttakPerioder(uttakPerioder);
        ungdomsytelseUttakPerioder.setRegelInput(lagRegelInput(godkjentePerioder, ungdomsprogramtidslinje, søkersDødsdato));
        ungdomsytelseUttakPerioder.setRegelSporing(regelSporingBuilder.buildRegelSporing());
        return Optional.of(ungdomsytelseUttakPerioder);
    }

    private static UttaksDelResultat finnResultatNokDager(LocalDateTimeline<Boolean> tidslinjeNokdager, LocalDateTimeline<Boolean> godkjentePerioder, LocalDateTimeline<Boolean> levendeBrukerTidslinje) {
        final var tidslinjeNokDagerTilVurdering = tidslinjeNokdager.intersection(godkjentePerioder);
        var tidslinjeNokDagerOgUtbetaling = finnTidslinjeNokDagerOgUtbetaling(tidslinjeNokDagerTilVurdering, levendeBrukerTidslinje);
        var uttakPerioder = mapTilUttakPerioderMedNokDagerOgUtbetaling(tidslinjeNokDagerOgUtbetaling);
        return new UttaksDelResultat(uttakPerioder, godkjentePerioder.disjoint(tidslinjeNokDagerOgUtbetaling));
    }

    private static UttaksDelResultat finnUttaksperioderAvslagEtterDød(LocalDateTimeline<Boolean> resterendeTidslinjeTilVurdering, LocalDateTimeline<Boolean> levendeBrukerTidslinje, RegelSporingBuilder regelSporingBuilder) {
        var avslåttEtterSøkersDødTidslinje = resterendeTidslinjeTilVurdering.disjoint(levendeBrukerTidslinje);
        regelSporingBuilder.medPerioderAvslåttEtterSøkersDød(avslåttEtterSøkersDødTidslinje);
        final var uttakPeriodeAvslåttEtterSøkersDød = mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(avslåttEtterSøkersDødTidslinje);
        return new UttaksDelResultat(uttakPeriodeAvslåttEtterSøkersDød, resterendeTidslinjeTilVurdering.disjoint(avslåttEtterSøkersDødTidslinje));
    }

    private static UttaksDelResultat finnIkkeNokDagerPerioder(LocalDateTimeline<Boolean> tidslinjeTilVurdering, LocalDateTimeline<Boolean> tidslinjeNokDager, RegelSporingBuilder regelSporingBuilder) {
        var perioderEtterOppbrukteDager = tidslinjeTilVurdering.disjoint(tidslinjeNokDager);
        regelSporingBuilder.medPerioderEtterOppbrukteDager(perioderEtterOppbrukteDager);
        final var ikkeNokDagerPeriode = perioderEtterOppbrukteDager
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
        return new UttaksDelResultat(ikkeNokDagerPeriode, tidslinjeTilVurdering.disjoint(perioderEtterOppbrukteDager));
    }

    private static List<UngdomsytelseUttakPeriode> mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(LocalDateTimeline<Boolean> avslåttEtterSøkersDødTidslinje) {
        return avslåttEtterSøkersDødTidslinje
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<UngdomsytelseUttakPeriode> mapTilUttakPerioderMedNokDagerOgUtbetaling(LocalDateTimeline<Boolean> tidslinjeNokDagerOgUtbetaling) {
        return tidslinjeNokDagerOgUtbetaling
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(BigDecimal.valueOf(100), DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeNokDagerOgUtbetaling(LocalDateTimeline<Boolean> tidslinjeNokDagerTilVurdering,
                                                                                LocalDateTimeline<Boolean> levendeBrukerTidslinje) {
        return tidslinjeNokDagerTilVurdering
            .intersection(levendeBrukerTidslinje);
    }

    private static String lagRegelInput(LocalDateTimeline<Boolean> godkjentePerioder, LocalDateTimeline<Boolean> ungdomsprogramtidslinje, Optional<LocalDate> søkersDødsdato) {
        return """
            {
                "godkjentePerioder": ":godkjentePerioder",
                "ungdomsprogramtidslinje": ":ungdomsprogramtidslinje",
                "søkersDødsdato": :søkersDødsdato
            }
            """.stripLeading()
            .replaceFirst(":godkjentePerioder", godkjentePerioder.getLocalDateIntervals().toString())
            .replaceFirst(":ungdomsprogramtidslinje", ungdomsprogramtidslinje.getLocalDateIntervals().toString())
            .replaceFirst(":søkersDødsdato", søkersDødsdato.map(Objects::toString).map(it -> "\"" + it + "\"").orElse("null"));
    }

    private record UttaksDelResultat(
        List<UngdomsytelseUttakPeriode> resultatPerioder,
        LocalDateTimeline<Boolean> restTidslinjeTilVurdering
    ) {}


    private static class RegelSporingBuilder {
        private LocalDateTimeline<Boolean> perioderMedNokDager;
        private LocalDateTimeline<Boolean> perioderEtterOppbrukteDager;
        private LocalDateTimeline<Boolean> perioderAvslåttEtterSøkersDød;
        private long antallForbrukteDager;

        private RegelSporingBuilder medPeriodeNokDager(LocalDateTimeline<Boolean> perioderMedNokDager) {
            this.perioderMedNokDager = perioderMedNokDager;
            return this;
        }

        private RegelSporingBuilder medPerioderEtterOppbrukteDager(LocalDateTimeline<Boolean> perioderEtterOppbrukteDager) {
            this.perioderEtterOppbrukteDager = perioderEtterOppbrukteDager;
            return this;
        }

        private RegelSporingBuilder medPerioderAvslåttEtterSøkersDød(LocalDateTimeline<Boolean> perioderAvslåttEtterSøkersDød) {
            this.perioderAvslåttEtterSøkersDød = perioderAvslåttEtterSøkersDød;
            return this;
        }

        private RegelSporingBuilder medAntallForbrukteDager(long antallForbrukteDager) {
            this.antallForbrukteDager = antallForbrukteDager;
            return this;
        }

        private String buildRegelSporing() {
            return """
            { "perioderMedNokDager": ":perioderMedNokDager", "perioderEtterOppbrukteDager": ":perioderEtterOppbrukteDager", "antallForbrukteDager": ":antallForbrukteDager",
            "perioderAvslåttEtterSøkersDød": :perioderAvslåttEtterSøkersDød }""".stripLeading()
                .replaceFirst(":perioderMedNokDager", perioderMedNokDager.getLocalDateIntervals().toString())
                .replaceFirst(":perioderEtterOppbrukteDager", perioderEtterOppbrukteDager.getLocalDateIntervals().toString())
                .replaceFirst(":antallForbrukteDager", Long.toString(antallForbrukteDager))
                .replaceFirst(":perioderAvslåttEtterSøkersDød", perioderAvslåttEtterSøkersDød.getLocalDateIntervals().toString());
        }



    }

}
