package no.nav.ung.sak.kontrakt.økonomi.tilbakekreving;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE)
public class VurderTilbaketrekkDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "hindreTilbaketrekk", required = true)
    @NotNull
    private Boolean hindreTilbaketrekk;

    public VurderTilbaketrekkDto() {
        //
    }

    public VurderTilbaketrekkDto(String begrunnelse, boolean hindreTilbaketrekk) {
        super(begrunnelse);
        this.hindreTilbaketrekk = hindreTilbaketrekk;
    }

    public Boolean getHindreTilbaketrekk() {
        return hindreTilbaketrekk;
    }

    public void setHindreTilbaketrekk(Boolean hindreTilbaketrekk) {
        this.hindreTilbaketrekk = hindreTilbaketrekk;
    }

    public boolean skalHindreTilbaketrekk() {
        return hindreTilbaketrekk;
    }
}
