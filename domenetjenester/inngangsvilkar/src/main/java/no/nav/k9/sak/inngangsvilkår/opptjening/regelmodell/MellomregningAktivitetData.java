package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

/**
 * Mellomregning per aktivitet.
 */
class MellomregningAktivitetData {

    static final LocalDateTimeline<Boolean> EMPTY = LocalDateTimeline.empty();
    private final Aktivitet aktivitet;
    /**
     * Forbered tidslinjer for mellomregning av aktiviteter.
     */
    private final LocalDateTimeline<Boolean> aktivitetTidslinjer;
    private LocalDateTimeline<Boolean> akseptertMellomliggendePerioder = EMPTY;
    /**
     * antatt godkjente perioder
     */
    private LocalDateTimeline<Boolean> aktivitetAntattGodkjent = EMPTY;

    /**
     * Manuelt godkjent av saksbehandler.
     */
    private LocalDateTimeline<Boolean> aktivitetManueltGodkjent = EMPTY;

    /**
     * Manuelt underkjent av saksbehandler.
     */
    private LocalDateTimeline<Boolean> aktivitetManueltUnderkjent = EMPTY;

    /**
     * Resulterende underkjentePerioder
     */
    private LocalDateTimeline<Boolean> aktivitetUnderkjent = EMPTY;

    /**
     * Inntekt per aktivitet.
     */
    private LocalDateTimeline<Long> inntektTidslinjer = LocalDateTimeline.empty();

    MellomregningAktivitetData(Aktivitet a) {
        this(a, EMPTY);
    }

    MellomregningAktivitetData(Aktivitet aktivitet, LocalDateTimeline<Boolean> aktivitetTidslinje) {
        this.aktivitet = aktivitet;
        this.aktivitetTidslinjer = aktivitetTidslinje;
    }

    public LocalDateTimeline<Boolean> getAktivitetTidslinje(boolean medAntattGodkjentePerioder, boolean medIkkebekreftedeGodkjentePerioder) {
        LocalDateTimeline<Boolean> tidslinje = aktivitetTidslinjer;
        tidslinje = tidslinje.crossJoin(akseptertMellomliggendePerioder, StandardCombinators::alwaysTrueForMatch);

        if (!medAntattGodkjentePerioder) {
            tidslinje = tidslinje.disjoint(aktivitetAntattGodkjent, StandardCombinators::leftOnly);
        }

        if (!medIkkebekreftedeGodkjentePerioder) {
            tidslinje = tidslinje.disjoint(aktivitetUnderkjent, StandardCombinators::leftOnly);
        }

        return hensyntaManuelle(tidslinje);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + aktivitet + ">";
    }

    private LocalDateTimeline<Boolean> hensyntaManuelle(LocalDateTimeline<Boolean> tidslinje) {
        if (!aktivitetManueltUnderkjent.isEmpty()) {
            tidslinje = tidslinje.disjoint(aktivitetManueltUnderkjent, StandardCombinators::leftOnly);
        }
        if (!aktivitetManueltGodkjent.isEmpty()) {
            tidslinje = tidslinje.crossJoin(aktivitetManueltGodkjent, StandardCombinators::alwaysTrueForMatch);
            aktivitetUnderkjent = aktivitetUnderkjent.disjoint(aktivitetManueltGodkjent, StandardCombinators::leftOnly);
            aktivitetAntattGodkjent = aktivitetAntattGodkjent.disjoint(aktivitetManueltGodkjent, StandardCombinators::leftOnly);
        }
        return tidslinje;
    }

    LocalDateTimeline<Boolean> getAkseptertMellomliggendePerioder() {
        return akseptertMellomliggendePerioder;
    }

    void setAkseptertMellomliggendePerioder(LocalDateTimeline<Boolean> akseptertMellomliggendePerioder) {
        this.akseptertMellomliggendePerioder = akseptertMellomliggendePerioder;
    }

    Aktivitet getAktivitet() {
        return aktivitet;
    }

    LocalDateTimeline<Boolean> getAktivitetAntattGodkjent() {
        return aktivitetAntattGodkjent;
    }

    void setAktivitetAntattGodkjent(LocalDateTimeline<Boolean> aktivitetAntattGodkjent) {
        this.aktivitetAntattGodkjent = aktivitetAntattGodkjent;
    }

    LocalDateTimeline<Boolean> getAktivitetTidslinjer() {
        return aktivitetTidslinjer;
    }

    LocalDateTimeline<Boolean> getAktivitetUnderkjent() {
        return aktivitetUnderkjent;
    }

    void setAktivitetUnderkjent(LocalDateTimeline<Boolean> aktivitetUnderkjent) {
        this.aktivitetUnderkjent = aktivitetUnderkjent;
    }

    LocalDateTimeline<Long> getInntektTidslinjer() {
        return inntektTidslinjer;
    }

    void setInntektTidslinjer(LocalDateTimeline<Long> inntektTidslinjer) {
        this.inntektTidslinjer = inntektTidslinjer;
    }

}
