package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.time.LocalDateTime;

public record AndreLivsoppholdsytelserVurderingResultat(
    boolean godkjent,
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak,
    boolean erManuellVurdering,
    String begrunnelse,
    String fritekstVurderingBrev,
    String ytelseNavn,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) implements VilkårsvurderingResultat {

    public static AndreLivsoppholdsytelserVurderingResultat fra(VilkårsvurderingResultat vurdering, String ytelseNavn) {
        return new AndreLivsoppholdsytelserVurderingResultat(
            vurdering.godkjent(),
            vurdering.ikkeOppfyltÅrsak(),
            vurdering.erManuellVurdering(),
            vurdering.begrunnelse(),
            vurdering.fritekstVurderingBrev(),
            ytelseNavn,
            vurdering.vurdertAv(),
            vurdering.vurdertTidspunkt()
        );
    }

    @Override
    public VilkårType vilkårType() {
        return VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
    }
}
