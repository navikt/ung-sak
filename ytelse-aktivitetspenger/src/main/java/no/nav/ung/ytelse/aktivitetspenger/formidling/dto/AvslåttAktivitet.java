package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.typer.Periode;

public record AvslåttAktivitet(
    Aktivitetsårsak årsak,
    String fritekstBrev,
    Periode periode
) {
    public enum Aktivitetsårsak {
        FRAVÆR_FRA_PLANLAGT_AKTIVITET,
        AVSTÅR_FRA_TILBUD_OM_ARBEID_UTEN_RIMELIG_GRUNN,
        ANNEN_ÅRSAK
    }
}
