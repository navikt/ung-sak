package no.nav.k9.sak.ytelse.beregning.grunnlag;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;

class BeregningsgrunnlagPerioderGrunnlagBuilder {

    private static final Logger log = LoggerFactory.getLogger(BeregningsgrunnlagPerioderGrunnlagBuilder.class);
    private final BeregningsgrunnlagPerioderGrunnlag kladd;
    private boolean built = false;

    BeregningsgrunnlagPerioderGrunnlagBuilder(BeregningsgrunnlagPerioderGrunnlag kladd) {
        this.kladd = (kladd != null) ? new BeregningsgrunnlagPerioderGrunnlag(kladd) : new BeregningsgrunnlagPerioderGrunnlag();
    }

    BeregningsgrunnlagPerioderGrunnlagBuilder leggTil(BeregningsgrunnlagPeriode periode) {
        validerState();
        Objects.requireNonNull(periode);
        kladd.deaktiver(periode.getSkjæringstidspunkt());
        kladd.leggTil(periode);
        return this;
    }

    BeregningsgrunnlagPerioderGrunnlagBuilder deaktiver(LocalDate skjæringstidspunkt) {
        validerState();
        Objects.requireNonNull(skjæringstidspunkt);
        kladd.deaktiver(skjæringstidspunkt);
        return this;
    }

    Optional<BeregningsgrunnlagPeriode> hentTidligere(LocalDate skjæringstidspunkt) {
        validerState();
        return kladd.finnFor(skjæringstidspunkt);
    }

    BeregningsgrunnlagPerioderGrunnlagBuilder validerMotVilkår(Vilkår vilkår) {
        Objects.requireNonNull(vilkår);
        if (!VilkårType.BEREGNINGSGRUNNLAGVILKÅR.equals(vilkår.getVilkårType())) {
            throw new IllegalArgumentException("[Utviklerfeil] krever BEREGNINGSGRUNNLAGVILKÅR");
        }

        var vilkårsSkjæringspunkter = vilkår.getPerioder().stream().map(VilkårPeriode::getSkjæringstidspunkt).collect(Collectors.toSet());

        var perioderUtenKnytningTilVilkårsPerioder = kladd.getGrunnlagPerioder()
            .stream()
            .map(BeregningsgrunnlagPeriode::getSkjæringstidspunkt)
            .filter(it -> vilkårsSkjæringspunkter.stream()
                .noneMatch(it::equals))
            .collect(Collectors.toList());

        if (!perioderUtenKnytningTilVilkårsPerioder.isEmpty()) {
            log.warn("BeregningsgrunnlagPerioderGrunnlag inneholder grunnlag for skjæringstidspunkter='{}' som ikke stemmer med vilkårsperioder med skjæringstidspunkter={}",
                perioderUtenKnytningTilVilkårsPerioder,
                vilkårsSkjæringspunkter);
            throw new IllegalStateException("BeregningsgrunnlagPerioderGrunnlag inneholder grunnlag for perioder som ikke er knyttet til vilkårsperioder");
        }

        return this;
    }

    BeregningsgrunnlagPerioderGrunnlagBuilder ryddMotVilkår(Vilkår vilkår) {
        Objects.requireNonNull(vilkår);
        if (!VilkårType.BEREGNINGSGRUNNLAGVILKÅR.equals(vilkår.getVilkårType())) {
            throw new IllegalArgumentException("[Utviklerfeil] krever BEREGNINGSGRUNNLAGVILKÅR");
        }

        var vilkårsSkjæringspunkter = vilkår.getPerioder().stream().map(VilkårPeriode::getSkjæringstidspunkt).collect(Collectors.toSet());

        var perioderUtenKnytningTilVilkårsPerioder = kladd.getGrunnlagPerioder()
            .stream()
            .map(BeregningsgrunnlagPeriode::getSkjæringstidspunkt)
            .filter(it -> vilkårsSkjæringspunkter.stream()
                .noneMatch(it::equals))
            .collect(Collectors.toList());

        if (!perioderUtenKnytningTilVilkårsPerioder.isEmpty()) {
            perioderUtenKnytningTilVilkårsPerioder.forEach(this::deaktiver);
        }

        return this;
    }

    BeregningsgrunnlagPerioderGrunnlag build() {
        validerState();
        this.built = true;

        return kladd;
    }

    boolean erForskjellig(BeregningsgrunnlagPerioderGrunnlag grunnlag, DiffEntity differ) {
        return differ.areDifferent(grunnlag, kladd);
    }

    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }
}
