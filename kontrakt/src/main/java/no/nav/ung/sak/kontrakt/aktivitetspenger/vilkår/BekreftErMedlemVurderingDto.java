package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.typer.Periode;

import java.util.Collections;
import java.util.List;

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

    @NotNull
    @Size(min = 1)
    @JsonProperty(required = true)
    private List<@Valid @NotNull Periode> perioderVurdert;

    @Size(min = 3, max = 10000)
    @Pattern(regexp = Patterns.FRITEKSTBREV, message = Patterns.FRITEKST_MISMATCH_MELDING)
    private String fritekstVurderingBrev;

    public BekreftErMedlemVurderingDto() {
        //Jackson
    }

    public BekreftErMedlemVurderingDto(String begrunnelse, Boolean erVilkårInnvilget, MedlemskapAvslagsÅrsakType avslagsårsak, List<Periode> perioderVurdert, String fritekstVurderingBrev) {
        super(begrunnelse);
        this.erVilkårInnvilget = erVilkårInnvilget;
        this.avslagsårsak = avslagsårsak;
        this.perioderVurdert = perioderVurdert;
        this.fritekstVurderingBrev = fritekstVurderingBrev;
    }

    public Boolean getErVilkårInnvilget() {
        return erVilkårInnvilget;
    }

    public MedlemskapAvslagsÅrsakType getAvslagsårsak() {
        return avslagsårsak;
    }

    public List<Periode> getPerioderVurdert() {
        return Collections.unmodifiableList(perioderVurdert);
    }

    @AssertTrue(message = "avslagsårsak må være satt hvis erVilkarOk er false")
    public boolean isAvslagsårsakSattHvisVilkårIkkeOk() {
        return erVilkårInnvilget || avslagsårsak != null;
    }

    @AssertTrue(message = "fritekstVurderingBrev må være satt hvis erVilkårInnvilget er false")
    public boolean isFritekstVurderingBrevSattHvisVilkårIkkeOk() {
        return erVilkårInnvilget || (fritekstVurderingBrev != null && !fritekstVurderingBrev.isBlank());
    }

    public String getFritekstVurderingBrev() {
        return fritekstVurderingBrev;
    }

}
