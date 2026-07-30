package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

/**
 * Bostedvurderingsresultat for en periode.
 */
public record BostedResultatDto(
    @NotNull Boolean erBosatt,
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    /** Om vurderingen er gjort manuelt av saksbehandler. */
    @NotNull boolean manuellVurdering,
    String begrunnelse,
    String friteksttilBrev,
    /** Brukerid på saksbehandler som valgte vurderingen. */
    @NotNull String vurdertAv
) {
}

