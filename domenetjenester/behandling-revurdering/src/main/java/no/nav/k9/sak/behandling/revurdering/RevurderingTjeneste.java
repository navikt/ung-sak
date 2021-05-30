package no.nav.k9.sak.behandling.revurdering;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class RevurderingTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private Instance<GrunnlagKopierer> grunnlagKopierere;
    private HistorikkRepository historikkRepository;

    public RevurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                               RevurderingTjenesteFelles revurderingTjenesteFelles,
                               @Any Instance<GrunnlagKopierer> grunnlagKopierere,
                               HistorikkRepository historikkRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.grunnlagKopierere = grunnlagKopierere;
        this.historikkRepository = historikkRepository;
    }

    public Behandling opprettManuellRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        validerTilstand(origBehandling);
        Behandling revurdering = opprettRevurdering(origBehandling, revurderingsÅrsak, true, enhet);

        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedManuellOpprettelse(origBehandling, revurdering);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, grunnlagKopierer.getApForManuellRevurdering());
        return revurdering;
    }

    public Behandling opprettAutomatiskRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
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

        opprettHistorikkinnslag(revurdering, revurderingsÅrsak, manueltOpprettet);

        return revurdering;
    }

    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        getGrunnlagKopierer(original.getFagsakYtelseType()).kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    private GrunnlagKopierer getGrunnlagKopierer(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(grunnlagKopierere, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Kopiering av grunnlag for revurdering ikke støttet for " + ytelseType.getKode()));
    }

    private void validerTilstand(Behandling origBehandling) {
        if (!revurderingTjenesteFelles.kanRevurderingOpprettes(origBehandling.getFagsak())) {
            throw new IllegalStateException("Kan ikke opprette revurdering på fagsak");
        }
    }

    public void opprettHistorikkinnslag(Behandling behandling, BehandlingÅrsakType revurderingÅrsak, boolean manueltOpprettet) {
        if (BehandlingÅrsakType.BERØRT_BEHANDLING.equals(revurderingÅrsak)) {
            return;
        }

        HistorikkAktør historikkAktør = manueltOpprettet ? HistorikkAktør.SAKSBEHANDLER : HistorikkAktør.VEDTAKSLØSNINGEN;

        Historikkinnslag revurderingsInnslag = new Historikkinnslag();
        revurderingsInnslag.setBehandling(behandling);
        revurderingsInnslag.setType(HistorikkinnslagType.REVURD_OPPR);
        revurderingsInnslag.setAktør(historikkAktør);
        HistorikkInnslagTekstBuilder historiebygger = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.REVURD_OPPR)
            .medBegrunnelse(revurderingÅrsak);
        historiebygger.build(revurderingsInnslag);

        historikkRepository.lagre(revurderingsInnslag);
    }

}
