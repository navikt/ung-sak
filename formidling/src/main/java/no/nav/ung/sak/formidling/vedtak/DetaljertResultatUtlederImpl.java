package no.nav.ung.sak.formidling.vedtak;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
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

        var perioderTilVurdering = bestemPeriodeTilVurdering(behandling);

        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        LocalDateTimeline<DetaljertResultat> combine = perioderTilVurdering.combine(tilkjentYtelseTidslinje,
            (p, lhs, rhs) -> {
                var årsaker = lhs != null ? lhs.getValue() : Collections.emptySet();
                var tilkjentYtelse = rhs != null ? rhs.getValue() : null;
                var resultater = new HashSet<DetaljertResultatType>();

                if (tilkjentYtelse != null && tilkjentYtelse.dagsats().compareTo(BigDecimal.ZERO) > 0) {
                    bestemResultatInnvilgelse(årsaker, tilkjentYtelse, resultater);

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

    private static void  bestemResultatInnvilgelse(Set<?> årsak, TilkjentYtelseVerdi tilkjentYtelse, HashSet<DetaljertResultatType> resultater) {
        if (tilkjentYtelse.utbetalingsgrad() < 100) {
            resultater.add(DetaljertResultatType.INNVILGET_NY_PERIODE);
        } else if (årsak.equals(Collections.singleton(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))) {
            resultater.add(DetaljertResultatType.INNVILGET_NY_PERIODE);
        } else {
            // Innvilgelse men uten søknad/endring fra bruker - spisse dette mer
            resultater.add(DetaljertResultatType.INNVILGET_NY_PERIODE);
        }
    }

    private LocalDateTimeline<Set<BehandlingÅrsakType>> bestemPeriodeTilVurdering(Behandling behandling) {
        var triggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId());

        if (behandling.getType() == BehandlingType.FØRSTEGANGSSØKNAD) {
            var programTidslinje = new LocalDateTimeline<>(vilkårResultatRepository
                .hentHvisEksisterer(behandling.getId())
                .flatMap(it -> it.getVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET))
                .orElseThrow(() -> new IllegalStateException("Mangler ungdomsprogram vilkåret"))
                .getPerioder().stream().map(vilkårPeriode -> new LocalDateSegment<>(vilkårPeriode.getPeriode().toLocalDateInterval(), true))
                .toList());

            var søknadsperiodeTidslinje = ungdomsytelseSøknadsperiodeTjeneste.utledTidslinje(behandling.getId());

            return søknadsperiodeTidslinje
                // Henter årsakene fra trigger da trigger kan gå til uendelig, men periodene fra hentes fra søknad
                .combine(triggerTidslinje, StandardCombinators::rightOnly, JoinStyle.LEFT_JOIN)
                // Joiner med ungdomsprogramvilkåret slik det er gjort i VilkårsperioderTilVurderningTjeneste
                .crossJoin(programTidslinje, StandardCombinators::leftOnly);

        }

        return triggerTidslinje;
    }

}
