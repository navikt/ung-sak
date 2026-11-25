package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.AktivitetsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AktivitetsvilkåretIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.aktivitet.AktivitetFaktaavklaringPeriodeDto;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class AktivitetAvklaringDataMapper {

    private AktivitetAvklaringDataMapper() {
    }

    public static AktivitetVarselInnhold mapTilAktivitetAvklaringInnhold(VilkårPeriodeAvklaring avklaring) {
        return new AktivitetVarselInnhold(
            avklaring.getPeriode().tilPeriode(),
            AktivitetsvilkåretIkkeOppfyltÅrsak.fraKode(avklaring.getIkkeOppfyltÅrsakKode()),
            avklaring.skalSendeVarsel(),
            avklaring.getFritekstTilVarsel(),
            AktivitetsavklaringKildeType.fraKode(avklaring.getKildeKode()),
            avklaring.getKildeFritekst(),
            avklaring.getAvklaringtype()
        );
    }

    public static VilkårPeriodeAvklaringForeslått mapTilVilkårPeriodeAvklaring(AktivitetAvklaring avklaring, UUID referanse) {
        var innhold = avklaring.innhold();
        Objects.requireNonNull(innhold.ikkeOppfyltÅrsak(), "Mangler årsak for hvorfor aktivitetsvilkåret ikke er oppfylt");
        return new VilkårPeriodeAvklaringForeslått(
            referanse,
            innhold.hentPeriodeSomDatoIntervallEntitet(),
            innhold.ikkeOppfyltÅrsak().getKode(),
            avklaring.begrunnelse(),
            innhold.skalSendeVarsel(),
            innhold.fritekstTilVarsel(),
            avklaring.begrunnelseIkkeVarsel(),
            innhold.kilde(),
            innhold.kildeFritekst(),
            avklaring.vurdertAv(),
            avklaring.vurdertTidspunkt(),
            innhold.avklaringtype()
        );
    }

    public static AktivitetAvklaring mapTilAktivitetAvklaring(AktivitetFaktaavklaringPeriodeDto dto, LocalDate maksDatoFraVilkårsperiode, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        var avklaringtype = dto.periode().getTom() != null ? Avklaringtype.AVSLAG : Avklaringtype.OPPHØR;
        var fom = dto.periode().getFom();
        // Konverterer opphør til en lukket periode, slik at det i ettertid er tydelig hvilken periode opphøret er utført på.
        var tom = dto.periode().getTom() != null ? dto.periode().getTom() : maksDatoFraVilkårsperiode;

        var innhold = new AktivitetVarselInnhold(
            new Periode(fom, tom),
            dto.avklaring().ikkeOppfyltÅrsak(),
            dto.avklaring().skalSendeVarsel(),
            dto.avklaring().fritekstTilVarsel(),
            dto.avklaring().kilde(),
            dto.avklaring().kildeFritekst(),
            avklaringtype
        );

        return new AktivitetAvklaring(innhold, dto.avklaring().begrunnelse(), dto.avklaring().begrunnelseIkkeVarsel(), vurdertAv, vurdertTidspunkt);
    }

}
