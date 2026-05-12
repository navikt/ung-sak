package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class AktivitetspengerFørstegangsbehandlingScenarioer {

    /**
     * 24 år, blir 25 år etter 15 dager i programmet.
     * Får både lav og høy sats i førstegangsbehandlingen.
     * Ingen inntektsgradering og ingen barn.
     */
    public static AktivitetspengerTestScenario innvilget24årBle25årførsteMåned(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);
        LocalDate tom25årmnd = tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth());

        return AktivitetspengerTestScenario.builder(fom)
            .medSatsGrunnlagTidslinjeFyllerTjuefem(tjuvefemårsdag)
            .medStandardBeregningsgrunnlag()
            .medTilkjentPeriode(new LocalDateInterval(fom, tom25årmnd))
            .medNySøktPeriodeTrigger()
            .build();
    }

    /**
     * Avslag pga bostedsvilkåret ikke oppfylt.
     * Bruker er 20 år, har søkt fra fom, men bor utenfor EØS.
     */
    public static AvslagScenario avslåttBosted(LocalDate fom) {
        return avslagScenario(fom,
            no.nav.ung.kodeverk.vilkår.VilkårType.BOSTEDSVILKÅR,
            no.nav.ung.kodeverk.vilkår.Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
    }

    /**
     * Avslag pga bistandsvilkåret ikke oppfylt.
     * Bruker er 20 år, har søkt fra fom, men mangler 14a-vedtak.
     */
    public static AvslagScenario avslåttBistand(LocalDate fom) {
        return avslagScenario(fom,
            no.nav.ung.kodeverk.vilkår.VilkårType.BISTANDSVILKÅR,
            no.nav.ung.kodeverk.vilkår.Avslagsårsak.IKKE_14A_VEDTAK);
    }

    /**
     * Avslag pga bostedsvilkåret ikke oppfylt - folkeregistrert eller bostedsadresse.
     * Bruker er 20 år, men har verken bosted eller folkeregistrert adresse i Trondheim.
     */
    public static AvslagScenario avslåttBostedFolkeregistrertEllerBostedsadresse(LocalDate fom) {
        return avslagScenario(fom,
            no.nav.ung.kodeverk.vilkår.VilkårType.BOSTEDSVILKÅR,
            no.nav.ung.kodeverk.vilkår.Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
    }

    /**
     * Avslag pga bostedsvilkåret ikke oppfylt - arbeidssted eller studiested.
     * Bruker er 20 år, men studie- eller arbeidsstedet er utenfor Trondheim.
     */
    public static AvslagScenario avslåttArbeidsstedStudiested(LocalDate fom) {
        return avslagScenario(fom,
            no.nav.ung.kodeverk.vilkår.VilkårType.BOSTEDSVILKÅR,
            no.nav.ung.kodeverk.vilkår.Avslagsårsak.YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED);
    }

    private static AvslagScenario avslagScenario(LocalDate fom,
                                                  no.nav.ung.kodeverk.vilkår.VilkårType vilkårType,
                                                  no.nav.ung.kodeverk.vilkår.Avslagsårsak avslagsårsak) {
        var tom = fom.plusWeeks(52).minusDays(1);

        var testScenario = AktivitetspengerTestScenario.builder(fom)
            .medNySøktPeriodeTrigger()
            .build();

        return new AvslagScenario(testScenario, new Periode(fom, tom), vilkårType, avslagsårsak);
    }

    public record AvslagScenario(
        AktivitetspengerTestScenario testScenario,
        Periode vilkårPeriode,
        no.nav.ung.kodeverk.vilkår.VilkårType vilkårType,
        no.nav.ung.kodeverk.vilkår.Avslagsårsak avslagsårsak
    ) {}
}
