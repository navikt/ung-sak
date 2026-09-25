package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

public record AvslåttBosted(
    Bostedsårsak årsak,
    String fritekstBrev
) {
    public enum Bostedsårsak {
        YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED,
        YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE,
        YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED,
        ANNEN_ÅRSAK
    }
}
