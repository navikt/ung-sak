package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandFaktaavklaringPeriodeDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public final class BistandAvklaringDtoMapper {

    private BistandAvklaringDtoMapper() {
    }

    public static VilkårPeriodeAvklaringForeslått mapTilVilkårPeriodeAvklaring(BistandFaktaavklaringPeriodeDto dto,
                                                                                LocalDate maksDatoFraVilkårsperiode,
                                                                                String vurdertAv,
                                                                                LocalDateTime vurdertTidspunkt) {
        var vurdering = dto.vurdering();
        IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak = Objects.requireNonNull(vurdering.ikkeOppfyltÅrsak(),
            "Mangler årsak for hvorfor bistandsvilkåret ikke er oppfylt");

        if (!ikkeOppfyltÅrsak.erGyldigAvklaringsårsak()) {
            throw new IllegalArgumentException("Årsaken " + ikkeOppfyltÅrsak.getKode() + " er ikke en gyldig avklaringsårsak");
        }
        if (ikkeOppfyltÅrsak.krevesFritekst() && dto.skalSendeVarsel()
            && (vurdering.fritekstTilVarsel() == null || vurdering.fritekstTilVarsel().isBlank())) {
            throw new IllegalArgumentException("Årsaken " + ikkeOppfyltÅrsak.getKode() + " krever fritekst til varsel");
        }

        var avklaringtype = dto.periode().getTom() != null ? Avklaringtype.AVSLAG : Avklaringtype.OPPHØR;
        var fom = dto.periode().getFom();
        // Konverterer opphør til en lukket periode, slik at det i ettertid er tydelig hvilken periode opphøret er utført på.
        var tom = dto.periode().getTom() != null ? dto.periode().getTom() : maksDatoFraVilkårsperiode;

        return new VilkårPeriodeAvklaringForeslått(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            ikkeOppfyltÅrsak.getKode(),
            vurdering.begrunnelse(),
            dto.skalSendeVarsel(),
            vurdering.fritekstTilVarsel(),
            vurdering.begrunnelseIkkeVarsel(),
            vurdertAv,
            vurdertTidspunkt,
            avklaringtype
        );
    }

}
