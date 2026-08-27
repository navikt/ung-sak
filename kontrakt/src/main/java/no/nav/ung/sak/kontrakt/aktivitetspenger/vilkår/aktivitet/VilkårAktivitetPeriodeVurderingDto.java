package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.aktivitet;


import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.vilkår.AktivitetsvilkåretIkkeOppfyltÅrsak;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.typer.Periode;

public record VilkårAktivitetPeriodeVurderingDto(

    @Valid
    @NotNull
    Periode periode,

    @NotNull
    boolean erVilkårOppfylt,

    @Valid
    AktivitetsvilkåretIkkeOppfyltÅrsak avslagsårsak,

    @NotNull
    @Size(min = 3, max = 5000)
    @Valid
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String begrunnelse,

    @Size(max = 10000)
    @Valid
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String fritekstVurderingBrev
) {

    @AssertFalse(message = "Avslagsårsak må være satt dersom vilkåret ikke er oppfylt")
    public boolean isManglerAvslagsårsak(){
        return !erVilkårOppfylt && avslagsårsak == null;
    }

    @AssertFalse(message = "Avslagsårsak må ikke være satt dersom vilkåret er oppfylt")
    public boolean isHarAvslagsårsakSattVedInnvilgelse(){
        return erVilkårOppfylt && avslagsårsak != null;
    }
}
