package no.nav.ung.sak.kontrakt.aktivitetspenger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP_KODE)
public class BekreftErMedlemVurderingDto extends BekreftetAksjonspunktDto {
    @NotNull
    private final Boolean erVilkarOk;

    /** Angitt avslagsårsak (dersom erVilkarOk==false) */
    @Valid
    private final MedlemskapAvslagsÅrsakType avslagsårsak;

    @JsonCreator
    public BekreftErMedlemVurderingDto(
        @JsonProperty("begrunnelse") String begrunnelse,
        @JsonProperty(value = "erVilkarOk", required = true) Boolean erVilkarOk,
        @JsonProperty(value = "avslagsårsak") MedlemskapAvslagsÅrsakType avslagsårsak) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.avslagsårsak = avslagsårsak;
    }

    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public MedlemskapAvslagsÅrsakType getAvslagsårsak() {
        return avslagsårsak;
    }
}
