package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.sak.typer.Saksnummer;

final class MapUnntakFraAktivitetGenerering {

    private MapUnntakFraAktivitetGenerering() {
    }

    static Map<Saksnummer, Set<LocalDate>> mapUnntak(String konfigString) {
        if (konfigString == null || konfigString.isEmpty()) {
            return Map.of();
        }

        var result = new HashMap<Saksnummer, Set<LocalDate>>();
        var splittedString = konfigString.split(";");

        for (String s : splittedString) {
            var split = s.split("\\|");
            if (split.length != 2) {
                continue; //  Ugyldig, next please
            }
            var saknummer = new Saksnummer(split[0]);

            var datoer = Arrays.stream(split[1].split(",")).map(String::trim).map(LocalDate::parse).collect(Collectors.toSet());

            result.put(saknummer, datoer);
        }

        return result;
    }
}
