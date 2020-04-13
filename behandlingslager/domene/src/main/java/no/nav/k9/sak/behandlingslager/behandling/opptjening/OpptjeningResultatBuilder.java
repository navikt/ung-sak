package no.nav.k9.sak.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class OpptjeningResultatBuilder {

    private static final Logger log = LoggerFactory.getLogger(OpptjeningResultatBuilder.class);
    private final OpptjeningResultat kladd;
    private boolean built = false;

    OpptjeningResultatBuilder(OpptjeningResultat kladd) {
        this.kladd = (kladd != null) ? new OpptjeningResultat(kladd) : new OpptjeningResultat();
    }

    OpptjeningResultatBuilder leggTil(Opptjening opptjening) {
        validerState();
        Objects.requireNonNull(opptjening);
        kladd.deaktiver(opptjening.getOpptjeningPeriode());
        kladd.leggTil(opptjening);
        return this;
    }

    OpptjeningResultatBuilder deaktiver(LocalDate skjæringstidspunkt) {
        validerState();
        Objects.requireNonNull(skjæringstidspunkt);
        kladd.deaktiver(skjæringstidspunkt);
        return this;
    }

    Optional<Opptjening> hentTidligereOpptjening(DatoIntervallEntitet periode) {
        validerState();
        return kladd.finnOpptjening(periode);
    }

    Optional<Opptjening> hentTidligereOpptjening(LocalDate skjæringstidspunkt) {
        validerState();
        return kladd.finnOpptjening(skjæringstidspunkt);
    }

    OpptjeningResultatBuilder validerMotVilkår(Vilkår vilkår) {
        Objects.requireNonNull(vilkår);
        if (!VilkårType.OPPTJENINGSVILKÅRET.equals(vilkår.getVilkårType())) {
            throw new IllegalArgumentException("[Utviklerfeil] krever Opptjeningsvilkår");
        }

        var vilkårsPerioder = vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).collect(Collectors.toSet());

        var opptjeningUtenKnytningTilVilkårsPerioder = kladd.getOpptjeningPerioder()
            .stream()
            .map(Opptjening::getOpptjeningPeriode)
            .filter(it -> vilkårsPerioder.stream()
                .noneMatch(it::grenserTil))
            .collect(Collectors.toList());

        if (!opptjeningUtenKnytningTilVilkårsPerioder.isEmpty()) {
            log.warn("OpptjeningResultat inneholder opptjening for perioder='{}' som ikke stemmer med vilkårsperioder={}",
                opptjeningUtenKnytningTilVilkårsPerioder,
                vilkårsPerioder);
            throw new IllegalStateException("Opptjeningsresultat inneholder opptjening for perioder som ikke er knyttet til vilkårsperioder");
        }

        return this;
    }

    OpptjeningResultat build() {
        validerState();
        this.built = true;

        return kladd;
    }

    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }
}
