package no.nav.k9.sak.ytelse.unntaksbehandling.repo;

import javax.persistence.EntityManager;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
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
    private final EntityManager em;
    private Fagsak fagsak;
    private final VilkårResultatRepository vilkårResultatRepository;

    public BasicBehandlingBuilder(EntityManager em) {
        this.em = em;
        behandlingRepository = new BehandlingRepository(em);
        vilkårResultatRepository = new VilkårResultatRepository(em);
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
