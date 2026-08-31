package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;

/**
 * Resultat av vurdering av bistandsvilkåret for en periode.
 */
public record BistandResultatDto(
    @NotNull Boolean godkjent,
    BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    /** Om vurderingen er gjort manuelt av saksbehandler. */
    @NotNull boolean manuellVurdering,
    String begrunnelse,
    String friteksttilBrev,
    /** Brukerid på saksbehandler som valgte vurderingen. */
    @NotNull String vurdertAv
) {
}
