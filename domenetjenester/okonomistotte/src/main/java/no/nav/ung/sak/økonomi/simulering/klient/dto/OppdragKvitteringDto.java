package no.nav.ung.sak.økonomi.simulering.klient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class OppdragKvitteringDto {

    @JsonProperty("alvorlighetsgrad")
    private String alvorlighetsgrad;

    @JsonProperty("beskr_melding")
    private String beskrMelding;

    @JsonProperty("melding_kode")
    private String meldingKode;

    @JsonProperty("kvittertTidspunkt")
    private LocalDateTime kvittertTidspunkt;

    @JsonProperty("aktiv")
    private boolean aktiv;

    public String getAlvorlighetsgrad() {
        return alvorlighetsgrad;
    }

    public void setAlvorlighetsgrad(String alvorlighetsgrad) {
        this.alvorlighetsgrad = alvorlighetsgrad;
    }

    public String getBeskrMelding() {
        return beskrMelding;
    }

    public void setBeskrMelding(String beskrMelding) {
        this.beskrMelding = beskrMelding;
    }

    public String getMeldingKode() {
        return meldingKode;
    }

    public void setMeldingKode(String meldingKode) {
        this.meldingKode = meldingKode;
    }

    public LocalDateTime getKvittertTidspunkt() {
        return kvittertTidspunkt;
    }

    public void setKvittertTidspunkt(LocalDateTime kvittertTidspunkt) {
        this.kvittertTidspunkt = kvittertTidspunkt;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }
}
