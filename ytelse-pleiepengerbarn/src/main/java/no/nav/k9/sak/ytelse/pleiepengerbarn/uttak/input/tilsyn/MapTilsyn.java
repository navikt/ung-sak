package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeMedVarighet;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapTilsyn {

    public Map<LukketPeriode, Duration> map(List<PeriodeMedVarighet> etablertTilsynsperioder) {
        final var result = new HashMap<LukketPeriode, Duration>();
        for (PeriodeMedVarighet etablertTilsynPeriode : etablertTilsynsperioder) {
            result.put(new LukketPeriode(etablertTilsynPeriode.getPeriode().getFomDato(), etablertTilsynPeriode.getPeriode().getTomDato()), etablertTilsynPeriode.getVarighet());
        }

        return result;
    }
}
