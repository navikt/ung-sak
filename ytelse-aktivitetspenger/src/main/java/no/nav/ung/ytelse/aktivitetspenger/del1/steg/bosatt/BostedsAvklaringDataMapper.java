package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class BostedsAvklaringDataMapper {

    private BostedsAvklaringDataMapper() {
    }

    public static BostedVarselInnhold mapTilBostedAvklaringInnhold(VilkårPeriodeAvklaring avklaring) {
        return new BostedVarselInnhold(
            avklaring.getPeriode().tilPeriode(),
            ikkeOppfyltÅrsak(avklaring),
            avklaring.skalSendeVarsel(),
            avklaring.getFritekstTilVarsel(),
            kilde(avklaring),
            avklaring.getKildeFritekst(),
            avklaring.getAvklaringtype()
        );
    }

    /**
     * Felles-modellen lagrer kodeverket som tekst, siden hvert vilkår har sitt eget utvalg av årsaker.
     */
    public static BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak(VilkårPeriodeAvklaring avklaring) {
        return BostedsvilkårIkkeOppfyltÅrsak.fraKode(avklaring.getIkkeOppfyltÅrsakKode());
    }

    public static BostedsavklaringKildeType kilde(VilkårPeriodeAvklaring avklaring) {
        return BostedsavklaringKildeType.fraKode(avklaring.getKildeKode());
    }

    public static VilkårPeriodeAvklaringForeslått mapTilVilkårPeriodeAvklaring(BostedAvklaring bostedAvklaring, UUID referanse) {
        var innhold = bostedAvklaring.innhold();
        Objects.requireNonNull(innhold.ikkeOppfyltÅrsak(), "Mangler årsak for hvorfor bostedsvilkåret ikke er oppfylt");
        if (innhold.skalSendeVarsel() && innhold.ikkeOppfyltÅrsak().kreverFritekst()) {
            Objects.requireNonNull(innhold.fritekstTilVarsel(), "fritekstTilVarsel må være satt når årsak=" + innhold.ikkeOppfyltÅrsak().getKode());
        }
        return new VilkårPeriodeAvklaringForeslått(
            referanse,
            innhold.hentPeriodeSomDatoIntervallEntitet(),
            innhold.ikkeOppfyltÅrsak().getKode(),
            bostedAvklaring.begrunnelse(),
            innhold.skalSendeVarsel(),
            innhold.fritekstTilVarsel(),
            bostedAvklaring.begrunnelseIkkeVarsel(),
            innhold.kilde(),
            innhold.kildeFritekst(),
            bostedAvklaring.vurdertAv(),
            bostedAvklaring.vurdertTidspunkt(),
            innhold.avklaringtype()
        );
    }

    public static BostedAvklaring mapTilBostedAvklaring(BostedFaktaavklaringPeriodeDto dto, LocalDate maksDatoFraVilkårsperiode, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        var avklaringtype = dto.periode().getTom() != null ? Avklaringtype.AVSLAG : Avklaringtype.OPPHØR;
        var fom = dto.periode().getFom();
        // Konverterer opphør til en lukket periode, slik at det i ettertid er tydelig hvilken periode opphøret er utført på.
        var tom = dto.periode().getTom() != null ? dto.periode().getTom() : maksDatoFraVilkårsperiode;

        var innhold = new BostedVarselInnhold(
            new Periode(fom, tom),
            dto.vurdering().fraflyttingsÅrsak(),
            dto.skalSendeVarsel(),
            dto.vurdering().fritekstTilVarsel(),
            dto.vurdering().kilde(),
            dto.vurdering().kildeFritekst(),
            avklaringtype
        );

        return new BostedAvklaring(innhold, dto.vurdering().begrunnelse(), dto.vurdering().begrunnelseIkkeVarsel(), vurdertAv, vurdertTidspunkt);
    }
}
