package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.time.LocalDateTime;

public sealed interface VilkårsvurderingResultat permits GenereltVilkårsvurderingResultat, AndreLivsoppholdsytelserVurderingResultat {
    VilkårType vilkårType();

    boolean godkjent();

    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak();

    boolean erManuellVurdering();

    String begrunnelse();

    String fritekstVurderingBrev();

    String vurdertAv();

    LocalDateTime vurdertTidspunkt();
}
