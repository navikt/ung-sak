package no.nav.k9.sak.behandling;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Journalpost;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class FagsakTjeneste {

    private FagsakRepository fagsakRepository;
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;

    FagsakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FagsakTjeneste(BehandlingRepositoryProvider repositoryProvider,
                          FagsakStatusEventPubliserer fagsakStatusEventPubliserer) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
    }

    public void opprettFagsak(Fagsak nyFagsak) {
        validerNyFagsak(nyFagsak);
        fagsakRepository.opprettNy(nyFagsak);
        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(nyFagsak, nyFagsak.getStatus());
        }
    }

    private void validerNyFagsak(Fagsak fagsak) {
        if (fagsak.getId() != null || !Objects.equals(fagsak.getStatus(), FagsakStatus.OPPRETTET)) {
            throw new IllegalArgumentException("Kan ikke kalle opprett fagsak med eksisterende: " + fagsak); //$NON-NLS-1$
        }
    }

    public Optional<Fagsak> finnFagsakGittSaksnummer(Saksnummer saksnummer, boolean taSkriveLås) {
        return fagsakRepository.hentSakGittSaksnummer(saksnummer, taSkriveLås);
    }

    public List<Fagsak> finnFagsakerForAktør(AktørId aktørId) {
        return fagsakRepository.hentForBruker(aktørId);
    }

    public Fagsak finnEksaktFagsak(long fagsakId) {
        return fagsakRepository.finnEksaktFagsak(fagsakId);
    }

    public void oppdaterFagsakMedGsakSaksnummer(Long fagsakId, Saksnummer saksnummer) {
        fagsakRepository.oppdaterSaksnummer(fagsakId, saksnummer);
    }

    public void lagreJournalPost(Journalpost journalpost) {
        fagsakRepository.lagre(journalpost);
    }

    public Optional<Journalpost> hentJournalpost(JournalpostId journalpostId) {
        return fagsakRepository.hentJournalpost(journalpostId);
    }

}
