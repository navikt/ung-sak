package no.nav.ung.sak.kontrakt.aksjonspunkt;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;

/**
 * Informasjon om aksjonspunktstilstanden i behandlingen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public record AksjonspunktTilstandDto(
    @JsonProperty(value = "aksjonspunktKode", required = true)
    @NotNull String aksjonspunktKode,

    @JsonProperty(value = "status", required = true)
    @NotNull AksjonspunktStatus status,

    @JsonProperty(value = "venteårsak")
    Venteårsak venteårsak,

    @JsonProperty(value = "ansvarligSaksbehandler")
    String ansvarligSaksbehandler,

    @JsonProperty(value = "fristTid")
    LocalDateTime fristTid,
    @JsonProperty(value = "opprettetTidspunkt")
    LocalDateTime opprettetTidspunkt,

    @JsonProperty(value = "endretTidspunkt")
    LocalDateTime endretTidspunkt) {

    public AksjonspunktTilstandDto(AksjonspunktTilstandDto kopierFra) {
        this(kopierFra.aksjonspunktKode, kopierFra.status, kopierFra.venteårsak, kopierFra.ansvarligSaksbehandler, kopierFra.fristTid, kopierFra.opprettetTidspunkt, kopierFra.endretTidspunkt);
    }

    @Override
    public String toString() {
        return "AksjonspunktTilstandDto{" +
            "aksjonspunktKode='" + aksjonspunktKode + '\'' +
            ", status='" + status + '\'' +
            ", venteårsak='" + venteårsak + '\'' +
            ", ansvarligSaksbehandler='" + ansvarligSaksbehandler + '\'' +
            ", fristTid=" + fristTid +
            ", opprettetTidspunkt=" + opprettetTidspunkt +
            ", endretTidspunkt=" + endretTidspunkt +
            '}';
    }
}
