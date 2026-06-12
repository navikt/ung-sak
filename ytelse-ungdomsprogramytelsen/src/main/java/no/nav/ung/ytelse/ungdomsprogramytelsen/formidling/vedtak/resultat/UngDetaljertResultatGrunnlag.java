package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.resultat;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramMaksPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * Intermidiate object for vilkårresultat og behandlingsårsaker primært brukt for tidslinje.
 * Ungdomsprogramytelse-spesifikk variant som i tillegg bærer {@link UngdomsprogramMaksPeriode}
 * og tom-dato for ungdomsprogramperioden.
 *
 */
public record UngDetaljertResultatGrunnlag(List<DetaljertVilkårResultat> vilkårsresultater,
                                           Set<BehandlingÅrsakType> behandlingÅrsaker,
                                           boolean manuellOpprettetBehandling,
                                           UngdomsprogramMaksPeriode ungdomsprogramMaksPeriode,
                                           DatoIntervallEntitet ungdomsprogramPeriode) {

    public Set<Utfall> utfall() {
        return vilkårsresultater.stream().map(DetaljertVilkårResultat::utfall).collect(Collectors.toSet());
    }

    public Set<DetaljertVilkårResultat> avslåtteVilkår() {
        return vilkårsresultater.stream()
            .filter(it -> it.utfall() == Utfall.IKKE_OPPFYLT)
            .collect(Collectors.toSet());
    }

    public Set<DetaljertVilkårResultat> ikkeVurderteVilkår() {
        return vilkårsresultater.stream().filter(it -> it.utfall() == Utfall.IKKE_VURDERT).collect(Collectors.toSet());
    }

    public Optional<UngdomsprogramMaksPeriode> ungdomsprogramMaksPeriodeOpt() {
        return Optional.ofNullable(ungdomsprogramMaksPeriode);
    }

}
