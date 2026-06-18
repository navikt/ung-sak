package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

/**
 * Bostedvurderingsresultat for en periode.
 */
public record BostedResultatDto(
    /** Om vilkåret er oppfylt (bruker er bosatt i Trondheim). */
    @NotNull Boolean erBosatt,
    /** Årsak til at vilkåret ikke er oppfylt. Null dersom vilkåret er oppfylt. */
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    /** Om vurderingen er gjort manuelt av saksbehandler. */
    @NotNull boolean manuellVurdering,
    /** Saksbehandlers begrunnelse for vurderingen. */
    String begrunnelse,
    /** Fritekst som skal med i brev. */
    String friteksttilBrev,
    /** Brukerid på saksbehandler som valgte vurderingen. */
    @NotNull String vurdertAv
) {
}

