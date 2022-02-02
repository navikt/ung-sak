package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/**
 * Informasjon om aksjonspunktstilstanden i behandlingen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public record AksjonspunktTilstandDto(String aksjonspunktKode, String status, String venteårsak, LocalDateTime fristTid) {
    @JsonCreator
    public AksjonspunktTilstandDto(
        @JsonProperty(value = "kode", required = true)
        @NotNull
            String aksjonspunktKode,
        @JsonProperty(value = "status", required = true)
        @NotNull
            String status,
        @JsonProperty(value = "venteårsak")
            String venteårsak,
        @JsonProperty(value = "fristTid")
        LocalDateTime fristTid) {
        this.aksjonspunktKode = aksjonspunktKode;
        this.status = status;
        this.venteårsak = venteårsak;
        this.fristTid = fristTid;
    }

    public AksjonspunktTilstandDto(AksjonspunktTilstandDto kopierFra) {
        this(kopierFra.aksjonspunktKode, kopierFra.status, kopierFra.venteårsak, kopierFra.fristTid);
    }

    @Override
    public String toString() {
        return "AksjonspunktTilstandDto{" +
            "aksjonspunktKode='" + aksjonspunktKode + '\'' +
            ", status='" + status + '\'' +
            ", venteårsak='" + venteårsak + '\'' +
            ", fristTid=" + fristTid +
            '}';
    }
}
