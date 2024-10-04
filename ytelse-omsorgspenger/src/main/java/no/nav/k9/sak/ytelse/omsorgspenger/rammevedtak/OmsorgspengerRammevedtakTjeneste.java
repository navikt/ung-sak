package no.nav.k9.sak.ytelse.omsorgspenger.rammevedtak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
public class OmsorgspengerRammevedtakTjeneste {
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private BehandlingRepository behandlingRepository;

    OmsorgspengerRammevedtakTjeneste() {
        // for CDI
    }

    @Inject
    public OmsorgspengerRammevedtakTjeneste(ÅrskvantumTjeneste årskvantumTjeneste, BehandlingRepository behandlingRepository) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public RammevedtakResponse hentRammevedtak(BehandlingUuidDto behandlingUuid, List<Personinfo> barn) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());


        DatoIntervallEntitet fagsakperiode = behandling.getFagsak().getPeriode();
        return årskvantumTjeneste.hentRammevedtak(behandling.getAktørId(), new LukketPeriode(fagsakperiode.getFomDato(), fagsakperiode.getTomDato()), barn);
    }

    public RammevedtakResponse hentRammevedtak(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        DatoIntervallEntitet fagsakperiode = behandling.getFagsak().getPeriode();
        return årskvantumTjeneste.hentRammevedtak(new LukketPeriode(fagsakperiode.getFomDato(), fagsakperiode.getTomDato()), behandling);
    }

    public RammevedtakResponse hentRammevedtakV1(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        DatoIntervallEntitet fagsakperiode = behandling.getFagsak().getPeriode();
        return årskvantumTjeneste.hentRammevedtakV1(new LukketPeriode(fagsakperiode.getFomDato(), fagsakperiode.getTomDato()), behandling);
    }

}
