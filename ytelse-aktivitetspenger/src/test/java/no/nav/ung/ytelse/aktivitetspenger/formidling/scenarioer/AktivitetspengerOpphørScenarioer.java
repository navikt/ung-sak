package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;

import java.time.LocalDate;

public class AktivitetspengerOpphørScenarioer {

    public record OpphørScenario(
        AktivitetspengerTestScenario opphørScenario,
        VilkårType vilkårType,
        Avslagsårsak avslagsårsak,
        Periode opphørtVilkårPeriode
    ) {}

    public static OpphørScenario opphørPgaBosted(LocalDate fom) {
        return opphørMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
    }

    public static OpphørScenario opphørPgaBostedFolkeregistrert(LocalDate fom) {
        return opphørMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
    }

    public static OpphørScenario opphørPgaArbeidsstedStudiested(LocalDate fom) {
        return opphørMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED);
    }

    private static OpphørScenario opphørMedÅrsak(LocalDate fom, VilkårType vilkårType, Avslagsårsak avslagsårsak) {
        var tom = fom.plusWeeks(52).minusDays(1);
        LocalDate opphørDato = fom.plusMonths(3);
        var opphørtVilkårPeriode = new Periode(opphørDato, tom);

        var opphørScenario = AktivitetspengerTestScenario.builder(fom)
            .medSatsGrunnlagTidslinjeLav()
            .medStandardBeregningsgrunnlag()
            .medTilkjentPeriode(new LocalDateInterval(fom, opphørDato.minusDays(1)))
            .medNySøktPeriodeTrigger()
            .build();

        return new OpphørScenario(opphørScenario, vilkårType, avslagsårsak, opphørtVilkårPeriode);
    }
}
