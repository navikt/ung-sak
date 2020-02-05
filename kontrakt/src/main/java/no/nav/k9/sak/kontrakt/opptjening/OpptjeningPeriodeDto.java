package no.nav.k9.sak.kontrakt.opptjening;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpptjeningPeriodeDto {

    @JsonProperty(value = "måneder")
    private int måneder;

    @JsonProperty(value = "dager")
    private int dager;

    public OpptjeningPeriodeDto() {
        // trengs for deserialisering av JSON
        this.måneder = 0;
        this.dager = 0;
    }

    public OpptjeningPeriodeDto(int måneder, int dager) {
        this.måneder = måneder;
        this.dager = dager;
    }

    public int getMåneder() {
        return måneder;
    }

    public void setMåneder(int måneder) {
        this.måneder = måneder;
    }

    public int getDager() {
        return dager;
    }

    public void setDager(int dager) {
        this.dager = dager;
    }
}
