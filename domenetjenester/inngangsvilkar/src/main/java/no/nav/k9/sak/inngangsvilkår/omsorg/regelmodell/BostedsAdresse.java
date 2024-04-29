package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import java.util.Objects;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class BostedsAdresse {
    private final String aktørId;
    private final String adresseLinje1;
    private final String adresselinje2;
    private final String adresselinje3;
    private final String postnummer;
    private final String land;
    private final DatoIntervallEntitet periode;

    public BostedsAdresse(String aktørId, String adresselinje1, String adresselinje2, String adresselinje3, String postnummer, String land, DatoIntervallEntitet periode) {
        this.aktørId = aktørId;
        this.adresseLinje1 = adresselinje1;
        this.adresselinje2 = adresselinje2;
        this.adresselinje3 = adresselinje3;
        this.postnummer = postnummer;
        this.land = land;
        this.periode = periode;
    }

    public String getAktørId() {
        return aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
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
            ", periode=" + periode +
            '}';
    }
}
