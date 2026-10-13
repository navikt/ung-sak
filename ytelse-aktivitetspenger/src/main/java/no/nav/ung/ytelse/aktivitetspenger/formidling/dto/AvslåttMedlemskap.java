package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import java.time.LocalDate;

public record AvslåttMedlemskap(
    LocalDate fom,
    String fritekstBrev
) {
}
