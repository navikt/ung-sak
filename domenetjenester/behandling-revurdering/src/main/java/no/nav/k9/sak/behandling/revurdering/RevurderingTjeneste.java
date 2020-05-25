package no.nav.k9.sak.behandling.revurdering;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@FagsakYtelseTypeRef
@ApplicationScoped
public class RevurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private Instance<GrunnlagKopierer> grunnlagKopierere;

    public RevurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                               BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                               RevurderingTjenesteFelles revurderingTjenesteFelles,
                               @Any Instance<GrunnlagKopierer> grunnlagKopierere) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.grunnlagKopierere = grunnlagKopierere;
    }

    public Behandling opprettManuellRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        Behandling behandling = opprettRevurdering(origBehandling, revurderingsÅrsak, true, enhet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING));
        return behandling;
    }

    public Behandling opprettAutomatiskRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        return opprettRevurdering(origBehandling, revurderingsÅrsak, false, enhet);
    }

    private Behandling opprettRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet, OrganisasjonsEnhet enhet) {
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);

        // Opprett revurderingsbehandling
        Behandling revurdering = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingsÅrsak, origBehandling, manueltOpprettet, enhet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurdering);

        // Kopier vilkår (samme vilkår vurderes i Revurdering)
        revurderingTjenesteFelles.kopierVilkårsresultat(origBehandling, revurdering, kontekst);

        // Kopier grunnlagsdata
         var grunnlagKopierer = FagsakYtelseTypeRef.Lookup.find(grunnlagKopierere, origBehandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Kopiering av grunnlag for revurdering ikke støttet for " + origBehandling.getFagsakYtelseType().getKode()));
        grunnlagKopierer.kopierAlleGrunnlagFraTidligereBehandling(origBehandling, revurdering);

        // Aksjonspunkt for skjema dersom manuelt opprettet
        if (manueltOpprettet) {
            grunnlagKopierer.opprettAksjonspunktForSaksbehandlerOverstyring(revurdering);
        }

        return revurdering;
    }

    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        var grunnlagKopierer = FagsakYtelseTypeRef.Lookup.find(grunnlagKopierere, original.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Kopiering av grunnlag for revurdering ikke støttet for " + original.getFagsakYtelseType().getKode()));
        grunnlagKopierer.kopierAlleGrunnlagFraTidligereBehandling(original, ny);
    }

    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

}
