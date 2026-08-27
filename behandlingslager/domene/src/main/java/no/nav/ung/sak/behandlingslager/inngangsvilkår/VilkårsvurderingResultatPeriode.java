package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Felles egenskaper for resultatperiodene til inngangsvilkårene, slik at de kan behandles likt
 * når de kobles sammen i en tidslinje per {@link VilkårType}.
 */
public interface VilkårsvurderingResultatPeriode {

    VilkårType getVilkårType();

    DatoIntervallEntitet getPeriode();

    boolean isGodkjent();

    IkkeOppfyltÅrsak getIkkeOppfyltÅrsak();

    String getBegrunnelse();

    String getFritekstVurderingBrev();
}
