package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

public record AvslåttAndreLivsoppholdsytelser(
    Livsoppholdsårsak årsak,
    // Ytelsesnavn i ubestemt form, f.eks. "dagpenger". Null når ytelsen ikke navngis i brevet.
    String ytelseNavn,
    String fritekstBrev
) {
    public static AvslåttAndreLivsoppholdsytelser av(Livsoppholdsårsak årsak, String fritekstBrev) {
        return new AvslåttAndreLivsoppholdsytelser(årsak, årsak.ytelseNavn, fritekstBrev);
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
        MOTTAR_ANNEN_YTELSE(null);

        // Ytelsesnavn i ubestemt form slik det leses i setningen "du får {{ytelseNavn}}"
        private final String ytelseNavn;

        Livsoppholdsårsak(String ytelseNavn) {
            this.ytelseNavn = ytelseNavn;
        }
    }
}
