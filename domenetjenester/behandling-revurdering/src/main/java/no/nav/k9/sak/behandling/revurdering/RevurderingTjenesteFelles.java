package no.nav.k9.sak.behandling.revurdering;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
public class RevurderingTjenesteFelles {

    private BehandlingRepository behandlingRepository;
    private FagsakRevurdering fagsakRevurdering;
    private OpptjeningRepository opptjeningRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    public RevurderingTjenesteFelles() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteFelles(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRevurdering = new FagsakRevurdering(repositoryProvider.getBehandlingRepository());
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    public Behandling opprettNyBehandling(BehandlingType behandlingType, BehandlingÅrsakType nyBehandlingÅrsakType,
                                          Behandling opprinneligBehandling,
                                          boolean manueltOpprettet,
                                          OrganisasjonsEnhet enhet) {
        BehandlingÅrsak.Builder nyBehandlingÅrsak = BehandlingÅrsak.builder(nyBehandlingÅrsakType)
            .medManueltOpprettet(manueltOpprettet);
        return Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlendeEnhet(enhet)
            .medBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()))
            .medBehandlingÅrsak(nyBehandlingÅrsak).build();
    }

    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return fagsakRevurdering.kanRevurderingOpprettes(fagsak);
    }

    public void kopierVilkårsresultat(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        Long originalBehandlingId = origBehandling.getId();
        vilkårResultatRepository.kopier(originalBehandlingId, revurdering.getId());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());

        // Kan være at førstegangsbehandling ble avslått før den har kommet til opptjening.
        if (opptjeningRepository.finnOpptjening(originalBehandlingId).isPresent()) {
            opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, revurdering);
        }
    }
}
