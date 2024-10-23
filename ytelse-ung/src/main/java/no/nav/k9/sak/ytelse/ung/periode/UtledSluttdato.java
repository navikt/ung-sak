package no.nav.k9.sak.ytelse.ung.periode;

import java.time.LocalDate;

/**
 * Utleder sluttdato for ungdomsytelse der vi ikke har sluttdato for ungdomsprogrammet
 */
public class UtledSluttdato {

    public static LocalDate utledSluttdato(LocalDate startDato, LocalDate sluttDato) {

        var faktiskSluttdato = sluttDato == null ? startDato.plus(PeriodeKonstanter.MAKS_PERIODE) : sluttDato;

        if (faktiskSluttdato.isAfter(LocalDate.now().plusYears(5))) {
            // Hvis dette skulle bli nødvendig i fremtiden kan denne sjekken fjernes.
            throw new IllegalArgumentException("Fagsak kan ikke være mer enn 5 år inn i fremtiden.");
        }

        return faktiskSluttdato;
    }

}
