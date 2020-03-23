package no.nav.k9.sak.behandling.revurdering;

import java.time.LocalDate;
import java.util.Optional;

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
    private RevurderingHistorikk revurderingHistorikk;
    private VilkårResultatRepository vilkårResultatRepository;

    public RevurderingTjenesteFelles() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteFelles(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRevurdering = new FagsakRevurdering(repositoryProvider.getBehandlingRepository());
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.revurderingHistorikk = new RevurderingHistorikk(repositoryProvider.getHistorikkRepository());
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    public Behandling opprettRevurderingsbehandling(BehandlingÅrsakType revurderingÅrsakType, Behandling opprinneligBehandling, boolean manueltOpprettet,
                                                    Optional<OrganisasjonsEnhet> enhet) {
        BehandlingType behandlingType = BehandlingType.REVURDERING;
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(revurderingÅrsakType)
            .medOriginalBehandling(opprinneligBehandling)
            .medManueltOpprettet(manueltOpprettet);
        Behandling revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()))
            .medBehandlingÅrsak(revurderingÅrsak).build();
        enhet.ifPresent(revurdering::setBehandlendeEnhet);
        revurderingHistorikk.opprettHistorikkinnslagOmRevurdering(revurdering, revurderingÅrsakType, manueltOpprettet);
        return revurdering;
    }

    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return fagsakRevurdering.kanRevurderingOpprettes(fagsak);
    }

    public void kopierVilkårsresultat(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        vilkårResultatRepository.kopier(origBehandling.getId(), revurdering.getId());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());
        
        // Kan være at førstegangsbehandling ble avslått før den har kommet til opptjening.
        if (opptjeningRepository.finnOpptjening(origBehandling.getId()).isPresent()) {
            opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, revurdering);
        }
    }
}
