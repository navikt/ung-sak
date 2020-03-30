package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class KontinuerligTilsynBuilder {

    private final KontinuerligTilsyn kladd;
    private boolean bygget = false;
    private LocalDateTimeline<GradOgBegrunnelse> kontinuerligTilsynTidslinje;
    private LocalDateTimeline<GradOgBegrunnelse> utvidetTilsynTidslinje;

    KontinuerligTilsynBuilder(KontinuerligTilsyn kontinuerligTilsyn) {
        Objects.requireNonNull(kontinuerligTilsyn);
        this.kladd = new KontinuerligTilsyn(kontinuerligTilsyn);
        this.kontinuerligTilsynTidslinje = new LocalDateTimeline<>(kladd.getPerioder()
            .stream()
            .filter(it -> it.getGrad() == 100)
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), new GradOgBegrunnelse(it.getGrad(), it.getBegrunnelse(), it.getÅrsaksammenheng(), it.getÅrsaksammenhengBegrunnelse())))
            .collect(Collectors.toList()));
        this.utvidetTilsynTidslinje = new LocalDateTimeline<>(kladd.getPerioder()
            .stream()
            .filter(it -> it.getGrad() == 200)
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), new GradOgBegrunnelse(it.getGrad(), it.getBegrunnelse())))
            .collect(Collectors.toList()));
    }

    public static KontinuerligTilsynBuilder builder() {
        return new KontinuerligTilsynBuilder(new KontinuerligTilsyn());
    }

    public static KontinuerligTilsynBuilder builder(KontinuerligTilsyn eksisterende) {
        Objects.requireNonNull(eksisterende, "Må ha eksisterende");
        return new KontinuerligTilsynBuilder(eksisterende);
    }

    public KontinuerligTilsynBuilder tilbakeStill(DatoIntervallEntitet periode) {
        validerBuilder();
        final var segment = new LocalDateSegment<GradOgBegrunnelse>(periode.getFomDato(), periode.getTomDato(), null);
        final var other = new LocalDateTimeline<>(List.of(segment));
        kontinuerligTilsynTidslinje = kontinuerligTilsynTidslinje.combine(other,
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();
        utvidetTilsynTidslinje = utvidetTilsynTidslinje.combine(other,
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();

        return this;
    }

    public KontinuerligTilsynBuilder leggTil(KontinuerligTilsynPeriode periode) {
        validerBuilder();
        final var segment = new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new GradOgBegrunnelse(periode.getGrad(), periode.getBegrunnelse(), periode.getÅrsaksammenheng(), periode.getÅrsaksammenhengBegrunnelse()));
        final var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        if (periode.getGrad() == 100) {
            this.kontinuerligTilsynTidslinje = kontinuerligTilsynTidslinje.combine(periodeTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        if (periode.getGrad() == 200) {
            this.utvidetTilsynTidslinje = utvidetTilsynTidslinje.combine(periodeTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return this;
    }

    private void validerBuilder() {
        if (bygget) {
            throw new IllegalStateException("Builder allerede bygget, opprett ny.");
        }
    }

    KontinuerligTilsyn build() {
        validerBuilder();
        bygget = true;
        final var perioder = kontinuerligTilsynTidslinje.compress()
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(it -> new KontinuerligTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue().getBegrunnelse(), it.getValue().getGrad(), it.getValue().getÅrsaksammenhengBegrunnelse(), it.getValue().getÅrsaksammenheng()))
            .collect(Collectors.toCollection(ArrayList::new));

        perioder.addAll(utvidetTilsynTidslinje.compress()
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(it -> new KontinuerligTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue().getBegrunnelse(), it.getValue().getGrad()))
            .collect(Collectors.toList()));
        kladd.setPerioder(perioder);

        return kladd;
    }

}
