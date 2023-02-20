package no.nav.k9.sak.behandlingslager.behandling;

import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Enkel builder for å lage en enkel behandling for internt bruk her.
 */
public class BasicBehandlingBuilder {

    private final BehandlingRepository behandlingRepository;
    private final AktørId aktørId = AktørId.dummy();
    private EntityManager em;
    private Fagsak fagsak;
    private VilkårResultatRepository vilkårResultatRepository;

    public BasicBehandlingBuilder(EntityManager em) {
        this.em = em;
        behandlingRepository = new BehandlingRepository(em);
        vilkårResultatRepository = new VilkårResultatRepository(em);
    }

    public Behandling opprettOgLagreFørstegangssøknad(Fagsak fagsak, BehandlingStatus startStatus) {
        var builder = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(startStatus);
        Behandling behandling = builder.build();

        lagreBehandling(behandling);

        em.flush();
        return behandling;
    }

    public Behandling opprettNyBehandling(Fagsak fagsak, BehandlingType behandlingType, BehandlingStatus startStatus) {
        var builder = Behandling.nyBehandlingFor(fagsak, behandlingType).medBehandlingStatus(startStatus);
        Behandling behandling = builder.build();

        lagreBehandling(behandling);

        em.flush();
        return behandling;
    }

    public Behandling opprettOgLagreFørstegangssøknad(FagsakYtelseType ytelse, BehandlingStatus startStatus) {
        Fagsak fagsak = opprettFagsak(ytelse);

        var builder = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(startStatus);
        Behandling behandling = builder.build();

        lagreBehandling(behandling);

        em.flush();
        return behandling;
    }

    public Behandling opprettOgLagreFørstegangssøknad(FagsakYtelseType ytelse) {
        Fagsak fagsak = opprettFagsak(ytelse);
        return opprettOgLagreFørstegangssøknad(fagsak);
    }

    public Behandling opprettOgLagreFørstegangssøknad(Fagsak fagsak) {
        var builder = Behandling.forFørstegangssøknad(fagsak);
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

    public Fagsak opprettFagsak(FagsakYtelseType ytelse) {
        if (fagsak != null) {
            assert fagsak.getYtelseType().equals(ytelse) : "Feil ytelsetype - kan ikke gjenbruke fagsak: " + fagsak;
            return fagsak;
        }
        return opprettFagsak(ytelse, aktørId);
    }

    public Fagsak opprettFagsak(FagsakYtelseType ytelse, AktørId aktørId) {

        // Opprett fagsak
        String randomSaksnummer = System.nanoTime() + "";
        this.fagsak = Fagsak.opprettNy(ytelse, aktørId, new Saksnummer(randomSaksnummer));
        em.persist(fagsak);
        em.flush();
        return fagsak;
    }

    public void lagreVilkårResultat(Long behandlingId, Vilkårene vilkårene) {
        vilkårResultatRepository.lagre(behandlingId, vilkårene);
    }
}
