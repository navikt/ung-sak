package no.nav.k9.sak.kontrakt.aksjonspunkt;

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
public record AksjonspunktTilstandDto(String aksjonspunktKode, String status, String venteårsak) {
    @JsonCreator
    public AksjonspunktTilstandDto(
        @JsonProperty(value = "kode", required = true)
        @NotNull
            String aksjonspunktKode,
        @JsonProperty(value = "status", required = true)
        @NotNull
            String status,
        @JsonProperty(value = "venteårsak")
            String venteårsak) {
        this.aksjonspunktKode = aksjonspunktKode;
        this.status = status;
        this.venteårsak = venteårsak;
    }

    public AksjonspunktTilstandDto(AksjonspunktTilstandDto kopierFra) {
        this(kopierFra.aksjonspunktKode, kopierFra.status, kopierFra.venteårsak);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "kode=" + aksjonspunktKode
            + ", status=" + status
            + (venteårsak == null ? "" : ", venteårsak=" + venteårsak)
            + ">";
    }
}
