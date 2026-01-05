package no.nav.ung.kodeverk.arbeidsforhold;

import no.nav.ung.kodeverk.api.Kodeverdi;

public enum OverordnetYtelseType implements Kodeverdi {
    SYKEPENGER("SYKEPENGER", "Sykepenger"),
    PLEIEPENGER("PLEIEPENGER", "Pleiepenger"),
    OMSORGSPENGER("OMSORGSPENGER", "Omsorgspenger"),
    OPPLÆRINGSPENGER("OPPLÆRINGSPENGER", "Opplæringspenger"),
    ARBEIDSAVKLARINGSPENGER("ARBEIDSAVKLARINGSPENGER", "Arbeidsavklaringspenger"),
    DAGPENGER("DAGPENGER", "Dagpenger"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    SVANGERSKAPSPENGER("SVANGERSKAPSPENGER", "Svangerskapspenger"),
    ENSLIG_FORSØRGER("ENSLIG_FORSØRGER", "Overgangsstønad enslig forsørger"),
    UDEFINERT("UDEFINERT", "Udefinert");

    private String kode;
    private String navn;

    OverordnetYtelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return "OVERORDNET_YTELSE_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
