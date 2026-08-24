package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BostedsAvklaringDataMapper {

    public static BostedAvklaringInnhold mapTilBostedAvklaringInnhold(BostedsPeriodeAvklaring bostedsPeriodeAvklaring) {
        return new BostedAvklaringInnhold(
            bostedsPeriodeAvklaring.getPeriode().tilPeriode(),
            bostedsPeriodeAvklaring.getIkkeOppfyltÅrsak(),
            bostedsPeriodeAvklaring.getBegrunnelse(),
            bostedsPeriodeAvklaring.skalSendeVarsel(),
            bostedsPeriodeAvklaring.getFritekstTilVarsel(),
            bostedsPeriodeAvklaring.getBegrunnelseIkkeVarsel(),
            bostedsPeriodeAvklaring.getAvklaringtype()
        );
    }

    public static BostedsPeriodeAvklaring mapTilBostedsPeriodeAvklaring(BostedAvklaringInnhold bostedAvklaringInnhold, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        return new BostedsPeriodeAvklaring(
            bostedAvklaringInnhold.hentPeriodeSomDatoIntervallEntitet(),
            bostedAvklaringInnhold.ikkeOppfyltÅrsak(),
            bostedAvklaringInnhold.begrunnelse(),
            bostedAvklaringInnhold.skalSendeVarsel(),
            bostedAvklaringInnhold.fritekstTilVarsel(),
            bostedAvklaringInnhold.begrunnelseIkkeVarsel(),
            vurdertAv,
            vurdertTidspunkt,
            bostedAvklaringInnhold.avklaringtype()
        );
    }

    public static BostedAvklaringInnhold mapTilBostedAvklaringInnhold(BostedFaktaavklaringPeriodeDto dto, LocalDate maksDatoFraVilkårsperiode) {
        var avklaringtype = dto.periode().getTom() != null ? Avklaringtype.AVSLAG : Avklaringtype.OPPHØR;
        var fom = dto.periode().getFom();
        // Konverterer opphør til en lukket periode, slik at det i ettertid er tydelig hvilken periode opphøret er utført på.
        var tom = dto.periode().getTom() != null ? dto.periode().getTom() : maksDatoFraVilkårsperiode;

        return new BostedAvklaringInnhold(
            new Periode(fom, tom),
            dto.vurdering().fraflyttingsÅrsak(),
            dto.vurdering().begrunnelse(),
            dto.skalSendeVarsel(),
            dto.vurdering().fritekstTilVarsel(),
            dto.vurdering().begrunnelseIkkeVarsel(),
            avklaringtype
        );
    }

}
