package no.nav.ung.sak.formidling.vedtak;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;

@Dependent
public class DetaljertResultatUtlederImpl implements DetaljertResultatUtleder {

    private TilkjentYtelseUtleder tilkjentYtelseUtleder;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;

    public DetaljertResultatUtlederImpl() {
    }

    @Inject
    public DetaljertResultatUtlederImpl(
        TilkjentYtelseUtleder tilkjentYtelseUtleder,
        ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder) {

        this.tilkjentYtelseUtleder = tilkjentYtelseUtleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
    }

    @Override
    public LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling) {

        var perioderTilVurdering = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId());
        var tilkjentYtelseTidslinje = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandling.getId());

        var perioderMedTilkjentYtelse = perioderTilVurdering.intersection(tilkjentYtelseTidslinje);

        if (perioderTilVurdering.disjoint(perioderMedTilkjentYtelse).isEmpty()) {
            return perioderTilVurdering.mapValue(it -> new DetaljertResultat(Set.of(DetaljertResultatType.INNVILGET_NY_PERIODE)));
        }

        return LocalDateTimeline.empty();
    }

}
