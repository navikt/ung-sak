package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.time.LocalDateTime;

/**
 * Felles egenskaper for resultatperiodene til inngangsvilkårene, slik at de kan behandles likt
 * når de kobles sammen i en tidslinje per {@link VilkårType}.
 * Inneholder alle feltene som trengs for å opprette en ny resultatperiode-entitet, slik at et
 * resultat kan brukes som input i entitetenes kopikonstruktør.
 */
public record VilkårsvurderingResultat(
    VilkårType vilkårType,
    boolean godkjent,
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak,
    boolean manuellVurdering,
    String begrunnelse,
    String fritekstVurderingBrev,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
