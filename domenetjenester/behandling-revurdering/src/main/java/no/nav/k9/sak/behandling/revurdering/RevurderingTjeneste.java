package no.nav.k9.sak.behandling.revurdering;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-004")
@ApplicationScoped
public class RevurderingTjeneste implements NyBehandlingTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private Instance<GrunnlagKopierer> grunnlagKopierere;

    public RevurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                               RevurderingTjenesteFelles revurderingTjenesteFelles,
                               @Any Instance<GrunnlagKopierer> grunnlagKopierere) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.grunnlagKopierere = grunnlagKopierere;
    }


    @Override
    public Behandling opprettManueltNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        validerTilstand(origBehandling);
        Behandling revurdering = opprettRevurdering(origBehandling, revurderingsÅrsak, true, enhet);

        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedManuellOpprettelse(origBehandling, revurdering);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, grunnlagKopierer.getApForManuellRevurdering());
        return revurdering;
    }

    @Override
    public Behandling opprettAutomatiskNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        validerTilstand(origBehandling);
        var revurdering = opprettRevurdering(origBehandling, revurderingsÅrsak, false, enhet);

        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedAutomatiskOpprettelse(origBehandling, revurdering);

        return revurdering;
    }

    private Behandling opprettRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet, OrganisasjonsEnhet enhet) {
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);

        // Opprett revurderingsbehandling
        Behandling revurdering = revurderingTjenesteFelles.opprettNyBehandling(BehandlingType.REVURDERING, revurderingsÅrsak, origBehandling, manueltOpprettet, enhet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurdering);

        // Kopier vilkår (samme vilkår vurderes i Revurdering)
        revurderingTjenesteFelles.kopierVilkårsresultat(origBehandling, revurdering, kontekst);

        return revurdering;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        getGrunnlagKopierer(original.getFagsakYtelseType()).kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

    @Override
    public Boolean kanNyBehandlingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    private void validerTilstand(Behandling origBehandling) {
        if (!kanNyBehandlingOpprettes(origBehandling.getFagsak())) {
            throw new IllegalStateException("Kan ikke opprette revurdering på fagsak");
        }
    }

    private GrunnlagKopierer getGrunnlagKopierer(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(grunnlagKopierere, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Kopiering av grunnlag for revurdering ikke støttet for " + ytelseType.getKode()));
    }

}
