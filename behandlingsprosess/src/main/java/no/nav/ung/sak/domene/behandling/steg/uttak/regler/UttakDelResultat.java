package no.nav.ung.sak.domene.behandling.steg.uttak.regler;


import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;

import java.util.List;
import java.util.Map;

public record UttakDelResultat(
    List<UngdomsytelseUttakPeriode> resultatPerioder,
    LocalDateTimeline<Boolean> restTidslinjeTilVurdering,
    Map<String, String> regelSporing
) {}
