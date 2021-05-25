package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtledetEtablertTilsyn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapTilsyn {

    public Map<LukketPeriode, Duration> map(LocalDateTimeline<UtledetEtablertTilsyn> etablertTilsyntidslinje) {
        final var result = new HashMap<LukketPeriode, Duration>();
        for (LocalDateSegment<UtledetEtablertTilsyn> segment : etablertTilsyntidslinje.compress().toSegments()) {
            result.put(new LukketPeriode(segment.getFom(), segment.getTom()), segment.getValue().getVarighet());
        }

        return result;
    }
}
