package no.nav.k9.sak.behandling;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class FagsakTjeneste {

    private FagsakRepository fagsakRepository;
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;

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

    public Optional<Fagsak> finnesEnFagsakSomOverlapper(FagsakYtelseType ytelseType, AktørId bruker, AktørId pleietrengende, AktørId relatertPersonAktørId, LocalDate fom, LocalDate tom) {
        var potensielleFagsaker = fagsakRepository.finnFagsakRelatertTil(ytelseType, bruker, pleietrengende, relatertPersonAktørId, fom, tom);
        if (potensielleFagsaker.isEmpty()) {
            return Optional.empty();
        }
        return potensielleFagsaker.stream().max(Comparator.comparing(Fagsak::getPeriode));
    }

    public boolean finnesEnFagsakForMinstEnAvAktørene(FagsakYtelseType ytelseType, AktørId bruker, AktørId pleietrengende, AktørId relatertPersonAktørId, LocalDate fom, LocalDate tom) {
        if (bruker != null) {
            final var fagSakerPåBruker = fagsakRepository.finnFagsakRelatertTil(ytelseType, bruker, null, null, fom, tom);
            if (!fagSakerPåBruker.isEmpty()) {
                return true;
            }
        }
        if (pleietrengende != null) {
            final var fagSakerPåPleietrengende = fagsakRepository.finnFagsakRelatertTil(ytelseType, pleietrengende, null, fom, tom);
            if (!fagSakerPåPleietrengende.isEmpty()) {
                return true;
            }
        }
        if (relatertPersonAktørId != null) {
            final var fagSakerPåRelatertPerson = fagsakRepository.finnFagsakRelatertTil(ytelseType, null, relatertPersonAktørId, fom, tom);
            if (!fagSakerPåRelatertPerson.isEmpty()) {
                return true;
            }
        }

        return false;
    }

}
