package no.nav.ung.ytelse.aktivitetspenger.testdata;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;

import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

/**
 * Hjelpeobjekt for å populere databasen med diverse aktivitetspenger-data. Brukes av TestScenarioBuilder
 */
public record AktivitetspengerTestScenario(
    String navn,
    List<Periode> søknadsperioder,
    LocalDateTimeline<AktivitetspengerSatsPeriode> satsperioder,
    LocalDateTimeline<Beregningsgrunnlag> beregningsgrunnlag,
    LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder,
    LocalDateTimeline<Utfall> aldersvilkår,
    LocalDate fødselsdato,
    Set<Trigger> behandlingTriggere,
    List<PersonInformasjon> barn,
    LocalDate dødsdato,
    LocalDateTimeline<KontrollertInntektPeriode> kontrollerInntektPerioder) {

    public static Builder builder(LocalDate fom) {
        return new Builder(fom);
    }

    public static class Builder {
        private final LocalDate fom;
        private LocalDate tom;
        private String navn = DEFAULT_NAVN;
        private LocalDate fødselsdato;
        private LocalDateTimeline<AktivitetspengerSatsPeriode> satsperioder;
        private LocalDateTimeline<Beregningsgrunnlag> beregningsgrunnlag;
        private LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder;
        private LocalDateTimeline<Utfall> aldersvilkår;
        private Set<Trigger> behandlingTriggere = new LinkedHashSet<>();
        private List<PersonInformasjon> barn = Collections.emptyList();
        private LocalDate dødsdato;
        private LocalDateTimeline<KontrollertInntektPeriode> kontrollerInntektPerioder;

        private LocalDateTimeline<AktivitetspengerSatsGrunnlag> satsGrunnlagTidslinje;
        private LocalDateInterval tilkjentPeriodeOverride;

        public Builder(LocalDate fom) {
            this.fom = fom;
            this.tom = fom.plusWeeks(52).minusDays(1);
            this.fødselsdato = fom.minusYears(20);
        }

        public Builder medNavn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            this.tom = tom;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public Builder medBarn(List<PersonInformasjon> barn) {
            this.barn = barn;
            return this;
        }

        public Builder medTriggere(Set<Trigger> triggere) {
            this.behandlingTriggere = new LinkedHashSet<>(triggere);
            return this;
        }

        public Builder medTrigger(BehandlingÅrsakType årsak, LocalDate triggerFom, LocalDate triggerTom) {
            this.behandlingTriggere.add(new Trigger(årsak, DatoIntervallEntitet.fra(triggerFom, triggerTom)));
            return this;
        }

        public Builder medTrigger(BehandlingÅrsakType årsak, LocalDateInterval interval) {
            this.behandlingTriggere.add(new Trigger(årsak, DatoIntervallEntitet.fra(interval)));
            return this;
        }

        public Builder medSatsperioder(LocalDateTimeline<AktivitetspengerSatsPeriode> satsperioder) {
            this.satsperioder = satsperioder;
            return this;
        }

        public Builder medBeregningsgrunnlag(LocalDateTimeline<Beregningsgrunnlag> beregningsgrunnlag) {
            this.beregningsgrunnlag = beregningsgrunnlag;
            return this;
        }

        public Builder medTilkjentYtelsePerioder(LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder) {
            this.tilkjentYtelsePerioder = tilkjentYtelsePerioder;
            return this;
        }

        public Builder medAldersvilkår(LocalDateTimeline<Utfall> aldersvilkår) {
            this.aldersvilkår = aldersvilkår;
            return this;
        }

        public Builder medKontrollerInntektPerioder(LocalDateTimeline<KontrollertInntektPeriode> kontrollerInntektPerioder) {
            this.kontrollerInntektPerioder = kontrollerInntektPerioder;
            return this;
        }

        public Builder medSatsGrunnlagTidslinjeLav() {
            var lavSats = lavSatsBuilder(fom).build();
            this.satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, tom, lavSats)
            ));
            this.satsperioder = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, tom, new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, tom), lavSats))
            ));
            return this;
        }

        public Builder medSatsGrunnlagTidslinjeFyllerTjuefem(LocalDate tjuvefemårsdag) {
            this.fødselsdato = tjuvefemårsdag.minusYears(25);
            var lavSats = lavSatsBuilder(fom).build();
            var høySats = høySatsBuilder(tjuvefemårsdag).build();

            this.satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), lavSats),
                new LocalDateSegment<>(tjuvefemårsdag, tom, høySats)
            ));
            this.satsperioder = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, tjuvefemårsdag.minusDays(1)), lavSats)),
                new LocalDateSegment<>(tjuvefemårsdag, tom, new AktivitetspengerSatsPeriode(new LocalDateInterval(tjuvefemårsdag, tom), høySats))
            ));
            return this;
        }

        public Builder medSatsGrunnlagTidslinjeLavMedBarn(LocalDate barnFødselsdato, int antallBarn) {
            var satsUtenBarn = lavSatsBuilder(fom).build();
            var satsMedBarn = lavSatsMedBarnBuilder(barnFødselsdato, antallBarn).build();

            this.satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), satsUtenBarn),
                new LocalDateSegment<>(barnFødselsdato, tom, satsMedBarn)
            ));
            this.satsperioder = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, barnFødselsdato.minusDays(1)), satsUtenBarn)),
                new LocalDateSegment<>(barnFødselsdato, tom, new AktivitetspengerSatsPeriode(new LocalDateInterval(barnFødselsdato, tom), satsMedBarn))
            ));
            return this;
        }

        public Builder medSatsGrunnlagTidslinje(LocalDateTimeline<AktivitetspengerSatsGrunnlag> satsGrunnlagTidslinje) {
            this.satsGrunnlagTidslinje = satsGrunnlagTidslinje;
            return this;
        }

        public Builder medStandardBeregningsgrunnlag() {
            this.beregningsgrunnlag = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
            ));
            return this;
        }

        public Builder medTilkjentPeriode(LocalDateInterval tilkjentPeriode) {
            this.tilkjentPeriodeOverride = tilkjentPeriode;
            return this;
        }

        public Builder medNySøktPeriodeTrigger() {
            var p = new LocalDateInterval(fom, tom);
            this.behandlingTriggere.add(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(p)));
            return this;
        }

        public AktivitetspengerTestScenario build() {
            var p = new LocalDateInterval(fom, tom);

            if (aldersvilkår == null) {
                aldersvilkår = new LocalDateTimeline<>(p, Utfall.OPPFYLT);
            }

            if (tilkjentYtelsePerioder == null && satsGrunnlagTidslinje != null && beregningsgrunnlag != null) {
                var satserTidslinje = lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag);
                var tilkjentPeriode = tilkjentPeriodeOverride != null ? tilkjentPeriodeOverride : p;
                tilkjentYtelsePerioder = AktivitetspengerBrevScenarioerUtils.tilkjentYtelsePerioder(satserTidslinje, tilkjentPeriode);
            }

            return new AktivitetspengerTestScenario(
                navn,
                List.of(new Periode(fom, tom)),
                satsperioder,
                beregningsgrunnlag,
                tilkjentYtelsePerioder,
                aldersvilkår,
                fødselsdato,
                behandlingTriggere,
                barn,
                dødsdato,
                kontrollerInntektPerioder
            );
        }
    }
}
