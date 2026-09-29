package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.typer.Periode;

public record AvslåttBistand(
    Bistandsårsak årsak,
    String fritekstBrev,
    Periode periode
) {
    public enum Bistandsårsak {
        KOMMET_I_ARBEID,
        KOMMET_I_UTDANNING,
        ANNEN_ÅRSAK
    }
}
