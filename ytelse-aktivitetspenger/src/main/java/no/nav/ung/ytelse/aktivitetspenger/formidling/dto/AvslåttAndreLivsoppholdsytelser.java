package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

public record AvslåttAndreLivsoppholdsytelser(
    Livsoppholdsårsak årsak,
    // Bøyd ytelsesnavn, f.eks. "dagpenger". Null når ytelsen ikke navngis i brevet.
    String ytelse,
    String fritekstBrev
) {
    public static AvslåttAndreLivsoppholdsytelser av(Livsoppholdsårsak årsak, String fritekstBrev) {
        return new AvslåttAndreLivsoppholdsytelser(årsak, årsak.ytelse, fritekstBrev);
    }

    public enum Livsoppholdsårsak {
        MOTTAR_ARBEIDSAVKLARINGSPENGER("arbeidsavklaringspenger"),
        MOTTAR_TILTAKSPENGER("tiltakspenger"),
        MOTTAR_KVALIFISERINGSSTØNAD("kvalifiseringsstønad"),
        MOTTAR_DAGPENGER("dagpenger"),
        MOTTAR_FORELDREPENGER("foreldrepenger"),
        MOTTAR_SVANGERSKAPSPENGER("svangerskapspenger"),
        MOTTAR_UFØRETRYGD("uføretrygd"),
        MOTTAR_INTRODUKSJONSSTØNAD("introduksjonsstønad"),
        MOTTAR_BARNEPENSJON("barnepensjon"),
        // Ytelsen navngis ikke av kodeverket - den står i friteksten saksbehandler skriver.
        MOTTAR_ANNEN_YTELSE(null);

        // Bøyd ytelsesnavn slik det leses i setningen "du får {{ytelse}}". Samme ordvalg som varselet til deltakeren.
        private final String ytelse;

        Livsoppholdsårsak(String ytelse) {
            this.ytelse = ytelse;
        }
    }
}
