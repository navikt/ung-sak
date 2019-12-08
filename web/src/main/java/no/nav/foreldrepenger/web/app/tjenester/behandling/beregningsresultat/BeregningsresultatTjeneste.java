package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;

@ApplicationScoped
public class BeregningsresultatTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private UttakRepository uttakRepository;
    private BeregningsresultatMapper beregningsresultatMapper;

    public BeregningsresultatTjeneste() {
        // For CDI
    }

    @Inject
    public BeregningsresultatTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                      UttakRepository uttakRepository,
                                      BeregningsresultatMapper beregningsresultatMapper) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakRepository = uttakRepository;
        this.beregningsresultatMapper = beregningsresultatMapper;
    }

    public Optional<BeregningsresultatDto> lagBeregningsresultatMedUttaksplan(Behandling behandling) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(behandling.getId());
        return beregningsresultatAggregatEntitet
            .map(bresAggregat -> beregningsresultatMapper.lagBeregningsresultatMedUttaksplan(behandling, bresAggregat, uttakResultat));
    }

}
