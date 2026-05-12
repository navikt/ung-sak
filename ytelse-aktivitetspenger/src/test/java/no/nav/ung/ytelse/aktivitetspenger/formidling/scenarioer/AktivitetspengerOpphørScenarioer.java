package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

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
        LocalDate fødselsdato = fom.minusYears(20);
        var tom = fom.plusWeeks(52).minusDays(1);
        var p = new LocalDateInterval(fom, tom);

        var lavSats = lavSatsBuilder(fom).build();
        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, new AktivitetspengerSatsPeriode(p, lavSats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, lavSats)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        LocalDate opphørDato = fom.plusMonths(3);
        var opphørtVilkårPeriode = new Periode(opphørDato, tom);

        var opphørScenario = new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, tom)),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), new LocalDateInterval(fom, opphørDato.minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(p))),
            Collections.emptyList(),
            null,
            null);

        return new OpphørScenario(opphørScenario, vilkårType, avslagsårsak, opphørtVilkårPeriode);
    }
}

