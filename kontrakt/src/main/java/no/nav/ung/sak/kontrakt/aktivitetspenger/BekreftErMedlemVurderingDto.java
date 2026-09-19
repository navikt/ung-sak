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
    @JsonProperty(required = true)
    private Boolean erVilkårInnvilget;

    /** Angitt avslagsårsak (dersom erVilkårOk==false) */
    @Valid
    private MedlemskapAvslagsÅrsakType avslagsårsak;

    @Valid
    @NotNull
    @JsonProperty(required = true)
    private Periode vilkårsperiode;

    public BekreftErMedlemVurderingDto() {
        //Jackson
    }

    public BekreftErMedlemVurderingDto(String begrunnelse, Boolean erVilkårInnvilget, MedlemskapAvslagsÅrsakType avslagsårsak, Periode vilkårsperiode) {
        super(begrunnelse);
        this.erVilkårInnvilget = erVilkårInnvilget;
        this.avslagsårsak = avslagsårsak;
        this.vilkårsperiode = vilkårsperiode;
    }

    public Boolean getErVilkårInnvilget() {
        return erVilkårInnvilget;
    }

    public MedlemskapAvslagsÅrsakType getAvslagsårsak() {
        return avslagsårsak;
    }

    public Periode getVilkårsperiode() {
        return vilkårsperiode;
    }

    @AssertTrue(message = "avslagsårsak må være satt hvis erVilkarOk er false")
    public boolean avslagsårsakSattHvisVilkårIkkeOk() {
        return erVilkårInnvilget || avslagsårsak != null;
    }
}
