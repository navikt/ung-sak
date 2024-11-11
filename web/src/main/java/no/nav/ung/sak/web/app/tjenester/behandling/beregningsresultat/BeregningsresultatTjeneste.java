package no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.ytelse.beregning.BeregningsresultatMapper;

@ApplicationScoped
public class BeregningsresultatTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<BeregningsresultatMapper> mappere;

    public BeregningsresultatTjeneste() {
        // For CDI
    }

    @Inject
    public BeregningsresultatTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                      @Any Instance<BeregningsresultatMapper> mappere) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.mappere = mappere;
    }

    public Optional<BeregningsresultatDto> lagBeregningsresultatMedUttaksplan(Behandling behandling) {
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(behandling.getId());
        return beregningsresultatAggregatEntitet
            .map(bresAggregat -> getMapper(behandling).map(behandling, bresAggregat));
    }

    private BeregningsresultatMapper getMapper(Behandling behandling) {
        // Workaround for at BehandlingTypeRef skal finne mapper for BehandlingType.UNNTAK før de ytelsesspefikke mappere
        return BehandlingTypeRef.Lookup.find(BeregningsresultatMapper.class, mappere, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseGet(() -> FagsakYtelseTypeRef.Lookup.find(mappere, behandling.getFagsakYtelseType())
                .orElseThrow());
    }

    public Optional<BeregningsresultatMedUtbetaltePeriodeDto> lagBeregningsresultatMedUtbetaltePerioder(Behandling behandling) {
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(behandling.getId());
        return beregningsresultatAggregatEntitet
            .map(bresAggregat -> getMapper(behandling).mapMedUtbetaltePerioder(behandling, bresAggregat));
    }
}
