package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class Kodeverdi {
    private String kode;
    private String termnavn;

    @JsonCreator
    public Kodeverdi(@JsonProperty("kode") String kode, @JsonProperty("termnavn") String termnavn) {
        this.kode = kode;
        this.termnavn = termnavn;
    }

    public String getKode() {
        return kode;
    }

    public String getTermnavn() {
        return termnavn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kodeverdi kodeverdi = (Kodeverdi) o;
        return Objects.equals(kode, kodeverdi.kode) &&
                Objects.equals(termnavn, kodeverdi.termnavn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode, termnavn);
    }

    @Override
    public String toString() {
        return "Kodeverdi{" +
                "kode='" + kode + '\'' +
                ", termnavn='" + termnavn + '\'' +
                '}';
    }
}
