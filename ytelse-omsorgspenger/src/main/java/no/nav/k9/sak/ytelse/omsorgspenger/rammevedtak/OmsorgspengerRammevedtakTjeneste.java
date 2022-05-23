package no.nav.k9.sak.ytelse.omsorgspenger.rammevedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
public class OmsorgspengerRammevedtakTjeneste {
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private BehandlingRepository behandlingRepository;
    private AktørTjeneste aktørTjeneste;

    OmsorgspengerRammevedtakTjeneste() {
        // for CDI
    }

    @Inject
    public OmsorgspengerRammevedtakTjeneste(ÅrskvantumTjeneste årskvantumTjeneste, BehandlingRepository behandlingRepository, AktørTjeneste aktørTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.aktørTjeneste = aktørTjeneste;
    }

    public RammevedtakResponse hentRammevedtak(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        PersonIdent personIdent = aktørTjeneste.hentPersonIdentForAktørId(behandling.getAktørId()).orElseThrow();

        DatoIntervallEntitet fagsakperiode = behandling.getFagsak().getPeriode();
        return årskvantumTjeneste.hentRammevedtak(personIdent, new LukketPeriode(fagsakperiode.getFomDato(), fagsakperiode.getTomDato()));
    }

}
