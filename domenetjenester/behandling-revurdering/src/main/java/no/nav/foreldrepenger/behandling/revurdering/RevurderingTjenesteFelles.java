package no.nav.foreldrepenger.behandling.revurdering;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
public class RevurderingTjenesteFelles {

    private BehandlingRepository behandlingRepository;
    private FagsakRevurdering fagsakRevurdering;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private OpptjeningRepository opptjeningRepository;
    private RevurderingHistorikk revurderingHistorikk;

    public RevurderingTjenesteFelles() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteFelles(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRevurdering = new FagsakRevurdering(repositoryProvider.getBehandlingRepository());
        this.medlemskapVilkårPeriodeRepository = repositoryProvider.getMedlemskapVilkårPeriodeRepository();
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.revurderingHistorikk = new RevurderingHistorikk(repositoryProvider.getHistorikkRepository());
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
        VilkårResultat origVilkårResultat = origBehandling.getBehandlingsresultat().getVilkårResultat();
        Objects.requireNonNull(origVilkårResultat, "Vilkårsresultat må være satt på revurderingens originale behandling");

        final var behandlingsresultat = Behandlingsresultat.builderFraEksisterende(origBehandling.getBehandlingsresultat()).buildFor(revurdering);
        VilkårResultatBuilder vilkårBuilder = VilkårResultatBuilder.kopi(origVilkårResultat);

        VilkårResultat vilkårResultat = vilkårBuilder.build();
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandlingRepository.lagre(vilkårResultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());

        // MedlemskapsvilkårPerioder er tilknyttet vilkårresultat, ikke behandling
        medlemskapVilkårPeriodeRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, revurdering);

        // Kan være at førstegangsbehandling ble avslått før den har kommet til opptjening.
        if (opptjeningRepository.finnOpptjening(origBehandling.getId()).isPresent()) {
            opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, revurdering);
        }
    }
}
