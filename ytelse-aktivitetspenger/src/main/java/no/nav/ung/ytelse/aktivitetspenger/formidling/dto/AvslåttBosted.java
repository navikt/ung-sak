package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.typer.Periode;

public record AvslåttBosted(
    Bostedsårsak årsak,
    String fritekstBrev,
    Periode periode
) {
    public enum Bostedsårsak {
        YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED,
        YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED,
        ANNEN_ÅRSAK
    }
}
