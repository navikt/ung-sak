package no.nav.ung.sak.økonomi.simulering.klient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class OppdragXmlDto {

    @JsonProperty("behandlingUuid")
    private String behandlingUuid;

    @JsonProperty("opprettetTidspunkt")
    private LocalDateTime opprettetTidspunkt;

    @JsonProperty("oppdragXml")
    private String base64oppdragXml;

    @JsonProperty("erRegeneret")
    private boolean erRegenerert;

    @JsonProperty("kvittering")
    private OppdragKvitteringDto oppdragKvitteringDto;

    public String getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(String behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public String getBase64oppdragXml() {
        return base64oppdragXml;
    }

    public void setBase64oppdragXml(String base64oppdragXml) {
        this.base64oppdragXml = base64oppdragXml;
    }

    public boolean isErRegenerert() {
        return erRegenerert;
    }

    public void setErRegenerert(boolean erRegenerert) {
        this.erRegenerert = erRegenerert;
    }

    public OppdragKvitteringDto getOppdragKvitteringDto() {
        return oppdragKvitteringDto;
    }

    public void setOppdragKvitteringDto(OppdragKvitteringDto oppdragKvitteringDto) {
        this.oppdragKvitteringDto = oppdragKvitteringDto;
    }
}
