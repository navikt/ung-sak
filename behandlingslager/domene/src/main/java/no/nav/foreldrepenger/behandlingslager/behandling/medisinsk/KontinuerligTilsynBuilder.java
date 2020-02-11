package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class KontinuerligTilsynBuilder {

    private final KontinuerligTilsyn kladd;
    private boolean bygget = false;
    private LocalDateTimeline<GradOgBegrunnelse> tilsynstidslinje;

    public static KontinuerligTilsynBuilder builder() {
        return new KontinuerligTilsynBuilder(new KontinuerligTilsyn());
    }

    public static KontinuerligTilsynBuilder builder(KontinuerligTilsyn eksisterende) {
        Objects.requireNonNull(eksisterende, "Må ha eksisterende");
        return new KontinuerligTilsynBuilder(eksisterende);
    }

    KontinuerligTilsynBuilder(KontinuerligTilsyn kontinuerligTilsyn) {
        Objects.requireNonNull(kontinuerligTilsyn);
        this.kladd = new KontinuerligTilsyn(kontinuerligTilsyn);
        this.tilsynstidslinje = new LocalDateTimeline<>(kladd.getPerioder().stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), new GradOgBegrunnelse(it.getGrad(), it.getBegrunnelse())))
            .collect(Collectors.toList()));
    }

    public KontinuerligTilsynBuilder tilbakeStill(DatoIntervallEntitet periode) {
        validerBuilder();
        final var segment = new LocalDateSegment<GradOgBegrunnelse>(periode.getFomDato(), periode.getTomDato(), null);
        final var other = new LocalDateTimeline<>(List.of(segment));
        tilsynstidslinje = tilsynstidslinje.combine(other,
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();

        return this;
    }

    public KontinuerligTilsynBuilder leggTil(KontinuerligTilsynPeriode periode) {
        validerBuilder();
        final var segment = new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new GradOgBegrunnelse(periode.getGrad(), periode.getBegrunnelse()));
        final var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        this.tilsynstidslinje = tilsynstidslinje.combine(periodeTidslinje,
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.CROSS_JOIN);

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
        final var perioder = tilsynstidslinje.compress()
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(it -> new KontinuerligTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue().getBegrunnelse(), it.getValue().getGrad()))
            .collect(Collectors.toList());
        kladd.setPerioder(perioder);

        return kladd;
    }

}
