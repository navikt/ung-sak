package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell;

import java.util.Objects;

import no.nav.k9.sak.typer.Periode;

public class BostedsAdresse {

    private final Periode periode;
    private final String aktørId;
    private final String adresseLinje1;
    private final String adresselinje2;
    private final String adresselinje3;
    private final String postnummer;
    private final String land;

    public BostedsAdresse(Periode periode, String aktørId, String adresselinje1, String adresselinje2, String adresselinje3, String postnummer, String land) {
        this.periode = periode;
        this.aktørId = aktørId;
        this.adresseLinje1 = adresselinje1;
        this.adresselinje2 = adresselinje2;
        this.adresselinje3 = adresselinje3;
        this.postnummer = postnummer;
        this.land = land;
    }

    public String getAktørId() {
        return aktørId;
    }

    public boolean erSammeAdresseOgOverlapperTidsmessig(BostedsAdresse annen) {
        return periode.overlaps(annen.periode)
            && Objects.equals(this.adresseLinje1, annen.adresseLinje1)
            && Objects.equals(this.postnummer, annen.postnummer)
            && Objects.equals(this.land, annen.land);
    }

    @Override
    public String toString() {
        return "BostedsAdresse{" +
            "aktørId='" + aktørId + '\'' +
            ", periode='" + periode + '\'' +
            ", adresseLinje1='" + adresseLinje1 + '\'' +
            ", adresselinje2='" + adresselinje2 + '\'' +
            ", adresselinje3='" + adresselinje3 + '\'' +
            ", postnummer='" + postnummer + '\'' +
            ", land='" + land + '\'' +
            '}';
    }
}
