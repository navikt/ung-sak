package no.nav.k9.sak.fagsak.prosessering.avsluttning;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.fagsak.hendelse.OppdaterFagsakStatusFelles;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class FagsakAvsluttningTjeneste {

    private final Logger log = LoggerFactory.getLogger(FagsakAvsluttningTjeneste.class);

    private FagsakRepository fagsakRepository;
    private FagsakAvsluttningRepository fagsakAvsluttningRepository;
    private BehandlingRepository behandlingRepository;
    private OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles;

    FagsakAvsluttningTjeneste() {
        // CDI
    }

    @Inject
    public FagsakAvsluttningTjeneste(FagsakRepository fagsakRepository,
                                     FagsakAvsluttningRepository fagsakAvsluttningRepository,
                                     BehandlingRepository behandlingRepository,
                                     OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles) {
        this.fagsakRepository = fagsakRepository;
        this.fagsakAvsluttningRepository = fagsakAvsluttningRepository;
        this.behandlingRepository = behandlingRepository;
        this.oppdaterFagsakStatusFelles = oppdaterFagsakStatusFelles;
    }

    public Set<Fagsak> finnFagsakerSomSkalAvsluttes() {
        return fagsakAvsluttningRepository.finnKandidaterFagsakerSomKanAvsluttes();
    }

    public void avsluttFagsak(Long fagsakId) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId);

        var kanAvsluttes = valider(fagsak, behandlinger);

        if (kanAvsluttes) {
            log.info("Avslutter fagsak med saksnummer='{}'", fagsak.getSaksnummer());
            oppdaterFagsakStatusFelles.oppdaterFagsakStatus(fagsak, null, FagsakStatus.AVSLUTTET);
        } else {
            log.info("Prøvde å avslutte fagsak med saksnummer='{}', men fagsaken kan ikke avsluttes", fagsak.getSaksnummer());
        }
    }

    private boolean valider(Fagsak fagsak, List<Behandling> behandlinger) {
        return fagsakAvsluttningRepository.fagsaksPeriodeHarUtløptMedToMåneder(fagsak)
            && behandlinger.stream().allMatch(Behandling::erAvsluttet);
    }

    public Fagsak finnFagsakForSaksnummer(Saksnummer it) {
        return fagsakRepository.hentSakGittSaksnummer(it).orElseThrow();
    }
}
