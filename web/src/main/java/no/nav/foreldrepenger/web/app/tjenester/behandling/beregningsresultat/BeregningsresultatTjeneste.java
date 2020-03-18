package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplan;

@ApplicationScoped
public class BeregningsresultatTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private UttakTjeneste uttakTjeneste;
    private BeregningsresultatMapper beregningsresultatMapper;

    public BeregningsresultatTjeneste() {
        // For CDI
    }

    @Inject
    public BeregningsresultatTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                      UttakTjeneste uttakTjeneste,
                                      BeregningsresultatMapper beregningsresultatMapper) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.beregningsresultatMapper = beregningsresultatMapper;
    }

    public Optional<BeregningsresultatDto> lagBeregningsresultatMedUttaksplan(Behandling behandling) {
        Optional<Uttaksplan> uttakResultat = uttakTjeneste.hentUttaksplan(behandling.getUuid());
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(behandling.getId());
        return beregningsresultatAggregatEntitet
            .map(bresAggregat -> beregningsresultatMapper.lagBeregningsresultatMedUttaksplan(behandling, bresAggregat, uttakResultat));
    }

}
