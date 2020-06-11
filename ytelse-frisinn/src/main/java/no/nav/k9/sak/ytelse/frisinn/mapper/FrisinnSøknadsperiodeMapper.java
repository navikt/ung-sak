package no.nav.k9.sak.ytelse.frisinn.mapper;

import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.typer.Periode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Returnerer en liste med perioder som representerer søknadsperioder for FRISINN, dvs ikke splittet på FL / SN.
 */
public class FrisinnSøknadsperiodeMapper {

    private FrisinnSøknadsperiodeMapper() {
        // Skjuler default
    }

    public static List<Periode> map(UttakAktivitet uttak) {
        Set<UttakAktivitetPeriode> uttakPerioder = uttak.getPerioder();

        // OBS: Her vil tom være key og fom være value
        Map<LocalDate, LocalDate> tomOgFomMap = uttakPerioder.stream()
            .collect(Collectors.toMap(per -> per.getPeriode().getTomDato(), per -> per.getPeriode().getFomDato(), ((d1, d2) -> d1.isAfter(d2) ? d2 : d1)));

        return tomOgFomMap.entrySet()
            .stream()
            .map(entry -> new Periode(entry.getValue(), entry.getKey()))
            .collect(Collectors.toList());
    }

}
