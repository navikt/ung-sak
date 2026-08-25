package no.nav.ung.ytelse.aktivitetspenger.testdata;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    LocalDateTimeline<KontrollertInntektPeriode> kontrollerInntektPerioder,
    Map<VilkårType, LocalDateTimeline<VilkårUtfall>> vilkår,
    InngangsvilkårVurderingTestData inngangsvilkårVurderinger) {

    public AktivitetspengerTestScenario(
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
        this(navn, søknadsperioder, satsperioder, beregningsgrunnlag, tilkjentYtelsePerioder, aldersvilkår, fødselsdato,
            behandlingTriggere, barn, dødsdato, kontrollerInntektPerioder, Map.of(), InngangsvilkårVurderingTestData.tom());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String navn;
        private List<Periode> søknadsperioder = List.of();
        private LocalDateTimeline<AktivitetspengerSatsPeriode> satsperioder;
        private LocalDateTimeline<Beregningsgrunnlag> beregningsgrunnlag;
        private LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder;
        private LocalDateTimeline<Utfall> aldersvilkår;
        private LocalDate fødselsdato;
        private Set<Trigger> behandlingTriggere = Set.of();
        private List<PersonInformasjon> barn = List.of();
        private LocalDate dødsdato;
        private LocalDateTimeline<KontrollertInntektPeriode> kontrollerInntektPerioder;
        private final Map<VilkårType, LocalDateTimeline<VilkårUtfall>> vilkår = new LinkedHashMap<>();
        private InngangsvilkårVurderingTestData inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.tom();

        public Builder medNavn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder medSøknadsperioder(List<Periode> søknadsperioder) {
            this.søknadsperioder = søknadsperioder;
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

        public Builder medTilkjentYtelse(LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder) {
            this.tilkjentYtelsePerioder = tilkjentYtelsePerioder;
            return this;
        }

        public Builder medAldersvilkår(LocalDateTimeline<Utfall> aldersvilkår) {
            this.aldersvilkår = aldersvilkår;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medTriggere(Set<Trigger> behandlingTriggere) {
            this.behandlingTriggere = behandlingTriggere;
            return this;
        }

        public Builder medBarn(List<PersonInformasjon> barn) {
            this.barn = barn;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public Builder medKontrollerteInntektperioder(LocalDateTimeline<KontrollertInntektPeriode> kontrollerInntektPerioder) {
            this.kontrollerInntektPerioder = kontrollerInntektPerioder;
            return this;
        }

        public Builder medVilkår(VilkårType vilkårType, LocalDateTimeline<VilkårUtfall> tidslinje) {
            if (vilkårType == VilkårType.ALDERSVILKÅR) {
                throw new IllegalArgumentException("Aldersvilkåret settes med medAldersvilkår");
            }
            if (vilkår.put(vilkårType, tidslinje) != null) {
                throw new IllegalArgumentException("Vilkår er allerede satt: " + vilkårType);
            }
            return this;
        }

        public Builder medInngangsvilkårVurderinger(InngangsvilkårVurderingTestData inngangsvilkårVurderinger) {
            this.inngangsvilkårVurderinger = inngangsvilkårVurderinger;
            return this;
        }

        public AktivitetspengerTestScenario build() {
            return new AktivitetspengerTestScenario(navn, søknadsperioder, satsperioder, beregningsgrunnlag, tilkjentYtelsePerioder,
                aldersvilkår, fødselsdato, behandlingTriggere, barn, dødsdato, kontrollerInntektPerioder, Map.copyOf(vilkår), inngangsvilkårVurderinger);
        }
    }
}
