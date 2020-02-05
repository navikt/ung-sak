package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE)
public class VurderTilbaketrekkDto extends BekreftetAksjonspunktDto {

    private Boolean hindreTilbaketrekk;

    VurderTilbaketrekkDto() {
        // For Jackson
    }

    public VurderTilbaketrekkDto(String begrunnelse, boolean hindreTilbaketrekk) {
        super(begrunnelse);
        this.hindreTilbaketrekk = hindreTilbaketrekk;
    }

    public void setHindreTilbaketrekk(Boolean hindreTilbaketrekk) {
        this.hindreTilbaketrekk = hindreTilbaketrekk;
    }


    public boolean skalHindreTilbaketrekk() {
        return hindreTilbaketrekk;
    }
}
