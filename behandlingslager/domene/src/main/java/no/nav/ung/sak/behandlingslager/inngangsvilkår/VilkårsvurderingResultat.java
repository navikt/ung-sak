package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.time.LocalDateTime;

/**
 * Felles egenskaper for resultatperiodene til inngangsvilkårene, slik at de kan behandles likt
 * når de kobles sammen i en tidslinje per {@link VilkårType}.
 */
public record VilkårsvurderingResultat(
    VilkårType vilkårType,
    boolean godkjent,
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak,
    boolean erManuellVurdering,
    String begrunnelse,
    String fritekstVurderingBrev,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
