package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseDto;

@Dependent
public class TilkjentYtelseTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    TilkjentYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseTjeneste(BehandlingRepositoryProvider repositoryProvider, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }


    public TilkjentYtelseDto hentilkjentYtelse(Behandling behandling) {
        MapperForTilkjentYtelse mapper = new MapperForTilkjentYtelse(arbeidsgiverTjeneste);
        var tyPerioder = beregningsresultatRepository.hentBeregningsresultat(behandling.getId())
            .map(br -> mapper.mapTilkjentYtelse(br))
            .orElse(List.of());

        return TilkjentYtelseDto.build()
            .medPerioder(tyPerioder)
            .create();
    }

}
