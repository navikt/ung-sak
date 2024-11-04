package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.time.LocalDate;
import java.util.Optional;
import java.util.TreeSet;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class UtledVirkningsdatoNyeUttaksregler {

    private final Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private final boolean datoNyeReglerPreutfyllingEnabled;

    @Inject
    public UtledVirkningsdatoNyeUttaksregler(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                             @KonfigVerdi(value = "DATO_NYE_REGLER_PREUTFYLLING", defaultVerdi = "false") boolean datoNyeReglerPreutfyllingEnabled) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.datoNyeReglerPreutfyllingEnabled = datoNyeReglerPreutfyllingEnabled;
    }

    public Optional<LocalDate> utledDato(BehandlingReferanse ref) {
        if (!datoNyeReglerPreutfyllingEnabled) {
            return Optional.empty();
        }
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        var allePerioderGjeldendeBehandling = perioderTilVurderingTjeneste.utledFullstendigePerioder(ref.getBehandlingId());
        var allePerioderForrigeBehandling = ref.getOriginalBehandlingId().map(perioderTilVurderingTjeneste::utledFullstendigePerioder).orElse(new TreeSet<>());
        var gjeldendeTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(allePerioderGjeldendeBehandling);
        var forrigeTidslinje = TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(allePerioderForrigeBehandling);
        var nyePerioder = gjeldendeTidslinje.disjoint(forrigeTidslinje);
        return nyePerioder.isEmpty() ? Optional.empty() : Optional.of(nyePerioder.getMinLocalDate());
    }


}
