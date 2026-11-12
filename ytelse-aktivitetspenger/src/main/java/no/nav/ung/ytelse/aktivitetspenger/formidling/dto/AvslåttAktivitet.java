package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.typer.Periode;

public record AvslåttAktivitet(
    Aktivitetsårsak årsak,
    String fritekstBrev,
    Periode periode
) {
    public enum Aktivitetsårsak {
        ANNEN_ÅRSAK
    }
}
