package no.nav.ung.sak.formidling.vedtak;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.perioder.UngdomsytelseVilkårsperioderTilVurderingTjeneste;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;

@Dependent
public class DetaljertResultatUtlederImpl implements DetaljertResultatUtleder {

    private TilkjentYtelseUtleder tilkjentYtelseUtleder;
    private UngdomsytelseVilkårsperioderTilVurderingTjeneste vilkårsperioderTilVurderingTjeneste;

    public DetaljertResultatUtlederImpl() {
    }

    @Inject
    public DetaljertResultatUtlederImpl(
        TilkjentYtelseUtleder tilkjentYtelseUtleder,
        @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE) UngdomsytelseVilkårsperioderTilVurderingTjeneste vilkårsperioderTilVurderingTjeneste) {

        this.tilkjentYtelseUtleder = tilkjentYtelseUtleder;
        this.vilkårsperioderTilVurderingTjeneste = vilkårsperioderTilVurderingTjeneste;
    }

    @Override
    public LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling) {

        var perioderTilVurdering = TidslinjeUtil.tilTidslinje(
            vilkårsperioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandling.getId()));
        var tilkjentYtelseTidslinje = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandling.getId());

        LocalDateTimeline<Boolean> perioderMedTilkjentYtelse = perioderTilVurdering.intersection(tilkjentYtelseTidslinje);

        if (perioderTilVurdering.disjoint(perioderMedTilkjentYtelse).isEmpty()) {
            return perioderTilVurdering.mapValue(it -> new DetaljertResultat(Set.of(DetaljertResultatType.INNVILGET_NY_PERIODE)));
        }

        return LocalDateTimeline.empty();
    }

}
