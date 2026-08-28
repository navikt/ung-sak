package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;

/**
 * Felles egenskaper for resultatperiodene til inngangsvilkårene, slik at de kan behandles likt
 * når de kobles sammen i en tidslinje per {@link VilkårType}.
 */
public record VilkårsvurderingResultat(
    VilkårType vilkårType,
    boolean godkjent,
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak,
    String begrunnelse,
    String fritekstVurderingBrev
) {
}
