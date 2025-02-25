package no.nav.ung.sak.formidling.vedtak;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;

@Dependent
public class DetaljertResultatUtlederImpl implements DetaljertResultatUtleder {

    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private VilkårResultatRepository vilkårResultatRepository;
    private UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste;
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    public DetaljertResultatUtlederImpl() {
    }

    @Inject
    public DetaljertResultatUtlederImpl(
        ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
        VilkårResultatRepository vilkårResultatRepository,
        UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste,
        TilkjentYtelseRepository tilkjentYtelseRepository) {

        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.ungdomsytelseSøknadsperiodeTjeneste = ungdomsytelseSøknadsperiodeTjeneste;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    @Override
    public LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling) {

        var perioderTilVurdering = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId());

        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        LocalDateTimeline<DetaljertResultat> combine = perioderTilVurdering.combine(tilkjentYtelseTidslinje,
            (p, lhs, rhs) -> {
                var årsaker = lhs != null ? lhs.getValue() : Collections.emptySet();
                var tilkjentYtelse = rhs != null ? rhs.getValue() : null;
                var resultater = new HashSet<DetaljertResultatType>();

                if (tilkjentYtelse != null) {
                    bestemResultatMedTilkjentYtelse(årsaker, tilkjentYtelse, resultater);
                } else {
                    //TODO må spisse avslag mer
                    resultater.add(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR);
                }

                return new LocalDateSegment<>(p, new DetaljertResultat(resultater));

            },
            JoinStyle.LEFT_JOIN
        );

        return combine;

    }

    private static void bestemResultatMedTilkjentYtelse(Set<?> årsak, TilkjentYtelseVerdi tilkjentYtelse, HashSet<DetaljertResultatType> resultater) {
        if (årsak.equals(Collections.singleton(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT))) {
            if (tilkjentYtelse.utbetalingsgrad() > 0) {
                resultater.add(DetaljertResultatType.ENDRING_RAPPORTERT_INNTEKT);
            } else {
                resultater.add(DetaljertResultatType.AVSLAG_RAPPORTERT_INNTEKT);
            }
        } else if (årsak.equals(Collections.singleton(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE))) {
            resultater.add(DetaljertResultatType.INNVILGET_NY_PERIODE);
        } else {
            // Innvilgelse men uten søknad/endring fra bruker - spisse dette mer
            resultater.add(DetaljertResultatType.INNVILGET_NY_PERIODE);
        }
    }

}
