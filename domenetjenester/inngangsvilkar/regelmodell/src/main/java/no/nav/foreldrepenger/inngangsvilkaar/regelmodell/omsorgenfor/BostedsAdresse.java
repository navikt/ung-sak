package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor;

import java.util.Objects;

public class BostedsAdresse {
    private final String aktørId;
    private final String adresseLinje1;
    private final String adresselinje2;
    private final String adresselinje3;
    private final String postnummer;
    private final String land;

    public BostedsAdresse(String aktørId, String adresselinje1, String adresselinje2, String adresselinje3, String postnummer, String land) {
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

    public boolean erSammeAdresse(BostedsAdresse that) {
        return Objects.equals(this.adresseLinje1, that.adresseLinje1)
            && Objects.equals(this.postnummer, that.postnummer)
            && Objects.equals(this.land, that.land);
    }

    @Override
    public String toString() {
        return "BostedsAdresse{" +
            "aktørId='" + aktørId + '\'' +
            ", adresseLinje1='" + adresseLinje1 + '\'' +
            ", adresselinje2='" + adresselinje2 + '\'' +
            ", adresselinje3='" + adresselinje3 + '\'' +
            ", postnummer='" + postnummer + '\'' +
            ", land='" + land + '\'' +
            '}';
    }
}
