package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaringForeslått;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BostedsAvklaringDataMapper {

    public static BostedVarselInnhold mapTilBostedAvklaringInnhold(BostedsPeriodeAvklaring bostedsPeriodeAvklaring) {
        return new BostedVarselInnhold(
            bostedsPeriodeAvklaring.getPeriode().tilPeriode(),
            bostedsPeriodeAvklaring.getIkkeOppfyltÅrsak(),
            bostedsPeriodeAvklaring.skalSendeVarsel(),
            bostedsPeriodeAvklaring.getFritekstTilVarsel(),
            bostedsPeriodeAvklaring.getKilde(),
            bostedsPeriodeAvklaring.getKildeFritekst(),
            bostedsPeriodeAvklaring.getAvklaringtype()
        );
    }

    public static BostedsPeriodeAvklaringForeslått mapTilBostedsPeriodeAvklaring(BostedAvklaring bostedAvklaring) {
        var innhold = bostedAvklaring.innhold();
        return new BostedsPeriodeAvklaringForeslått(
            innhold.hentPeriodeSomDatoIntervallEntitet(),
            innhold.ikkeOppfyltÅrsak(),
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
