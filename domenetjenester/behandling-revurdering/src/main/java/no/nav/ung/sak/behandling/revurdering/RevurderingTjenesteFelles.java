package no.nav.ung.sak.behandling.revurdering;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
public class RevurderingTjenesteFelles {

    private BehandlingRepository behandlingRepository;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private FagsakRevurdering fagsakRevurdering;
    private VilkårResultatRepository vilkårResultatRepository;

    public RevurderingTjenesteFelles() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteFelles(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingAnsvarligRepository = repositoryProvider.getBehandlingAnsvarligRepository();
        this.fagsakRevurdering = new FagsakRevurdering(repositoryProvider.getBehandlingRepository());
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    public Behandling opprettNyBehandling(BehandlingType behandlingType, BehandlingÅrsakType nyBehandlingÅrsakType,
                                          Behandling opprinneligBehandling,
                                          boolean manueltOpprettet) {
        BehandlingÅrsak.Builder nyBehandlingÅrsak = BehandlingÅrsak.builder(nyBehandlingÅrsakType)
            .medManueltOpprettet(manueltOpprettet);
        Behandling behandling = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()))
            .medBehandlingÅrsak(nyBehandlingÅrsak).build();

        return behandling;
    }

    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return fagsakRevurdering.kanRevurderingOpprettes(fagsak);
    }

    public void kopierVilkårsresultat(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        Long originalBehandlingId = origBehandling.getId();
        vilkårResultatRepository.kopier(originalBehandlingId, revurdering.getId());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());

    }
}
