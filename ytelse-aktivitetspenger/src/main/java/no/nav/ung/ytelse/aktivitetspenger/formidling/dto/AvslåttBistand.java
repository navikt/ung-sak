package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.typer.Periode;

public record AvslåttBistand(
    Bistandsårsak årsak,
    String fritekstBrev,
    Periode periode
) {
    public enum Bistandsårsak {
        HAR_IKKE_14A_VEDTAK
    }
}
