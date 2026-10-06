package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.typer.Periode;

public record AvslåttAndreLivsoppholdsytelser(
    Livsoppholdsårsak årsak,
    // Ytelsesnavn i ubestemt form, f.eks. "dagpenger".
    String ytelseNavn,
    String fritekstBrev,
    Periode periode
) {
    public static AvslåttAndreLivsoppholdsytelser av(Livsoppholdsårsak årsak, String fritekstBrev, Periode periode) {
        return new AvslåttAndreLivsoppholdsytelser(årsak, årsak.ytelseNavn, fritekstBrev, periode);
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
        MOTTAR_ANNEN_YTELSE("en annen livsoppholdsytelse");

        // Ytelsesnavn i ubestemt form slik det leses i setningen "du får {{ytelseNavn}}"
        private final String ytelseNavn;

        Livsoppholdsårsak(String ytelseNavn) {
            this.ytelseNavn = ytelseNavn;
        }

        public String getYtelseNavn() {
            return ytelseNavn;
        }
    }
}
