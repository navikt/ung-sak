package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandFaktaavklaringPeriodeDto;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Mapper mellom den generiske {@link VilkårPeriodeAvklaring} (fase 0) og den bistandsspesifikke
 * {@link BistandAvklaringInnhold}. Årsaken lagres som rå kode i fellesmodellen og types tilbake her.
 */
public final class BistandAvklaringDataMapper {

    private BistandAvklaringDataMapper() {
    }

    public static BistandAvklaringInnhold mapTilBistandAvklaringInnhold(VilkårPeriodeAvklaring avklaring) {
        return new BistandAvklaringInnhold(
            avklaring.getPeriode().tilPeriode(),
            BistandsvilkårIkkeOppfyltÅrsak.fraKode(avklaring.getIkkeOppfyltÅrsakKode()),
            avklaring.getBegrunnelse(),
            avklaring.skalSendeVarsel(),
            avklaring.getFritekstTilVarsel(),
            avklaring.getBegrunnelseIkkeVarsel(),
            avklaring.getAvklaringtype()
        );
    }

    public static VilkårPeriodeAvklaringForeslått mapTilVilkårPeriodeAvklaring(BistandAvklaringInnhold innhold, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        Objects.requireNonNull(innhold.ikkeOppfyltÅrsak(), "Mangler årsak for hvorfor bistandsvilkåret ikke er oppfylt");
        return new VilkårPeriodeAvklaringForeslått(
            innhold.hentPeriodeSomDatoIntervallEntitet(),
            innhold.ikkeOppfyltÅrsak().getKode(),
            innhold.begrunnelse(),
            innhold.skalSendeVarsel(),
            innhold.fritekstTilVarsel(),
            innhold.begrunnelseIkkeVarsel(),
            vurdertAv,
            vurdertTidspunkt,
            innhold.avklaringtype()
        );
    }

    public static BistandAvklaringInnhold mapTilBistandAvklaringInnhold(BistandFaktaavklaringPeriodeDto dto, LocalDate maksDatoFraVilkårsperiode) {
        var avklaringtype = dto.periode().getTom() != null ? Avklaringtype.AVSLAG : Avklaringtype.OPPHØR;
        var fom = dto.periode().getFom();
        // Konverterer opphør til en lukket periode, slik at det i ettertid er tydelig hvilken periode opphøret er utført på.
        var tom = dto.periode().getTom() != null ? dto.periode().getTom() : maksDatoFraVilkårsperiode;

        return new BistandAvklaringInnhold(
            new Periode(fom, tom),
            dto.vurdering().ikkeOppfyltÅrsak(),
            dto.vurdering().begrunnelse(),
            dto.skalSendeVarsel(),
            dto.vurdering().fritekstTilVarsel(),
            dto.vurdering().begrunnelseIkkeVarsel(),
            avklaringtype
        );
    }

}
