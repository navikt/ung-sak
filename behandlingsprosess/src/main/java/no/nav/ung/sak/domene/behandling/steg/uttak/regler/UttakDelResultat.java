package no.nav.ung.sak.domene.behandling.steg.uttak.regler;


import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.util.Map;

public record UttakDelResultat(
    LocalDateTimeline<UttakAvslagResultat> resultatTidslinje,
    LocalDateTimeline<Boolean> restTidslinjeTilVurdering,
    Map<String, String> regelSporing
) {}
