package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class EtablertPleiebehovBuilder {

    private final EtablertPleieperioder kladd;
    private boolean bygget = false;
    private LocalDateTimeline<Pleiegrad> tidslinje;

    EtablertPleiebehovBuilder(EtablertPleieperioder kontinuerligTilsyn) {
        Objects.requireNonNull(kontinuerligTilsyn);
        this.kladd = new EtablertPleieperioder(kontinuerligTilsyn);
        this.tidslinje = new LocalDateTimeline<>(kladd.getPerioder()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it.getGrad()))
            .collect(Collectors.toList()));
    }

    public static EtablertPleiebehovBuilder builder() {
        return new EtablertPleiebehovBuilder(new EtablertPleieperioder());
    }

    public static EtablertPleiebehovBuilder builder(EtablertPleieperioder eksisterende) {
        Objects.requireNonNull(eksisterende, "Må ha eksisterende");
        return new EtablertPleiebehovBuilder(eksisterende);
    }

    public EtablertPleiebehovBuilder tilbakeStill(DatoIntervallEntitet periode) {
        validerBuilder();
        final var segment = new LocalDateSegment<Pleiegrad>(periode.getFomDato(), periode.getTomDato(), null);
        final var other = new LocalDateTimeline<>(List.of(segment));
        tidslinje = tidslinje.combine(other,
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();

        return this;
    }

    public EtablertPleiebehovBuilder leggTil(EtablertPleieperiode periode) {
        validerBuilder();
        final var segment = new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), periode.getGrad());
        final var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));
        this.tidslinje = tidslinje.combine(periodeTidslinje,
            StandardCombinators::coalesceRightHandSide,
            LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return this;
    }

    private void validerBuilder() {
        if (bygget) {
            throw new IllegalStateException("Builder allerede bygget, opprett ny.");
        }
    }

    EtablertPleieperioder build() {
        validerBuilder();
        bygget = true;
        final var perioder = tidslinje.compress()
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(it -> new EtablertPleieperiode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue()))
            .collect(Collectors.toList());
        kladd.setPerioder(perioder);

        return kladd;
    }

}
