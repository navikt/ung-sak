package no.nav.ung.sak.kontrakt.aktivitetspenger;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP_KODE)
public class BekreftErMedlemVurderingDto extends BekreftetAksjonspunktDto {
    @NotNull
    @JsonProperty(value = "erVilkarOk", required = true)
    private Boolean erVilkarOk;

    /** Angitt avslagsårsak (dersom erVilkarOk==false) */
    @Valid
    @JsonProperty(value = "avslagsårsak")
    private MedlemskapAvslagsÅrsakType avslagsårsak;

    public BekreftErMedlemVurderingDto() {
        //Jackson
    }

    public BekreftErMedlemVurderingDto(String begrunnelse, Boolean erVilkarOk, MedlemskapAvslagsÅrsakType avslagsårsak) {
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

    @AssertTrue(message = "avslagsårsak må være satt hvis erVilkarOk er false")
    public boolean avslagsårsakSattHvisVilkårIkkeOk() {
        return erVilkarOk || avslagsårsak != null;
    }

}
