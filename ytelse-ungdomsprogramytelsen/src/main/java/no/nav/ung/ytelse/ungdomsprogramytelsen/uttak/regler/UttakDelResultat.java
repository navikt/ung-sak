package no.nav.ung.ytelse.ungdomsprogramytelsen.uttak.regler;


import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.util.Map;

public record UttakDelResultat(
    LocalDateTimeline<UttakResultat> resultatTidslinje,
    LocalDateTimeline<Boolean> restTidslinjeTilVurdering,
    Map<String, String> regelSporing
) {}
