package no.nav.ung.sak.økonomi.tilbakekreving.modell;

import jakarta.persistence.EntityManager;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

/** Enkel builder for å lage en enkel behandling for internt bruk her. */
class BasicBehandlingBuilder {

    private EntityManager em;
    private final BehandlingRepository behandlingRepository;

    private Fagsak fagsak;

    private final AktørId aktørId = AktørId.dummy();

    BasicBehandlingBuilder(EntityManager em) {
        this.em = em;
        behandlingRepository = new BehandlingRepository(em);
    }

    Behandling opprettOgLagreFørstegangssøknad(FagsakYtelseType ytelse) {
        Fagsak fagsak = opprettFagsak(ytelse);
        return opprettOgLagreFørstegangssøknad(fagsak);
    }

    private Behandling opprettOgLagreFørstegangssøknad(Fagsak fagsak) {
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();

        lagreBehandling(behandling);

        em.flush();
        return behandling;
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    private BehandlingLås taSkriveLås(Behandling behandling) {
        return behandlingRepository.taSkriveLås(behandling);
    }

    private Fagsak opprettFagsak(FagsakYtelseType ytelse) {
        if (fagsak != null) {
            assert fagsak.getYtelseType().equals(ytelse) : "Feil ytelsetype - kan ikke gjenbruke fagsak: " + fagsak;
            return fagsak;
        }
        return opprettFagsak(ytelse, aktørId);
    }

    private Fagsak opprettFagsak(FagsakYtelseType ytelse, AktørId aktørId) {
        // Opprett fagsak
        String randomSaksnummer = System.nanoTime() + "";
        this.fagsak = Fagsak.opprettNy(ytelse, aktørId, new Saksnummer(randomSaksnummer));
        em.persist(fagsak);
        em.flush();
        return fagsak;
    }

}
