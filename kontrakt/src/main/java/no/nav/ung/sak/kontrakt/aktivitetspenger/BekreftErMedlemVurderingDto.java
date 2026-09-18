package no.nav.ung.sak.kontrakt.aktivitetspenger;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP_KODE)
public class BekreftErMedlemVurderingDto extends BekreftetAksjonspunktDto {
    @NotNull
    @JsonProperty(value = "erVilkårOk", required = true)
    private Boolean erVilkårOk;

    /** Angitt avslagsårsak (dersom erVilkårOk==false) */
    @Valid
    @JsonProperty(value = "avslagsårsak")
    private MedlemskapAvslagsÅrsakType avslagsårsak;

    @Valid
    @NotNull
    @JsonProperty(value = "vilkårsperiode", required = true)
    private Periode vilkårsperiode;

    public BekreftErMedlemVurderingDto() {
        //Jackson
    }

    public BekreftErMedlemVurderingDto(String begrunnelse, Boolean erVilkårOk, MedlemskapAvslagsÅrsakType avslagsårsak, Periode vilkårsperiode) {
        super(begrunnelse);
        this.erVilkårOk = erVilkårOk;
        this.avslagsårsak = avslagsårsak;
        this.vilkårsperiode = vilkårsperiode;
    }

    public Boolean getErVilkårOk() {
        return erVilkårOk;
    }

    public MedlemskapAvslagsÅrsakType getAvslagsårsak() {
        return avslagsårsak;
    }

    public Periode getVilkårsperiode() {
        return vilkårsperiode;
    }

    @AssertTrue(message = "avslagsårsak må være satt hvis erVilkarOk er false")
    public boolean avslagsårsakSattHvisVilkårIkkeOk() {
        return erVilkårOk || avslagsårsak != null;
    }
}
