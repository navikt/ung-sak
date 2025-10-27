package no.nav.ung.sak.behandling.revurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

import java.util.Optional;
import java.util.Set;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class RevurderingTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private Instance<GrunnlagKopierer> grunnlagKopierere;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private ProsessTriggereRepository prosessTriggereRepository;

    public RevurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                               RevurderingTjenesteFelles revurderingTjenesteFelles,
                               @Any Instance<GrunnlagKopierer> grunnlagKopierere,
                               HistorikkinnslagRepository historikkinnslagRepository, ProsessTriggereRepository prosessTriggereRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.grunnlagKopierere = grunnlagKopierere;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    public Behandling opprettManuellRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet, Optional<DatoIntervallEntitet> periode) {
        validerTilstand(origBehandling);
        Behandling revurdering = opprettRevurdering(origBehandling, revurderingsÅrsak, true, enhet);

        DatoIntervallEntitet revurderingPeriode = periode.orElse(revurdering.getFagsak().getPeriode());
        prosessTriggereRepository.leggTil(revurdering.getId(), Set.of(new Trigger(revurderingsÅrsak, revurderingPeriode)));


        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedManuellOpprettelse(origBehandling, revurdering);

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
        HistorikkAktør historikkAktør = manueltOpprettet ? HistorikkAktør.SAKSBEHANDLER : HistorikkAktør.VEDTAKSLØSNINGEN;

        var historikkBuilder = new Historikkinnslag.Builder();
        historikkBuilder.medTittel("Revurdering opprettet")
            .medBehandlingId(behandling.getId())
            .addLinje(revurderingÅrsak.getNavn())
            .medFagsakId(behandling.getFagsakId())
            .medAktør(historikkAktør);

        historikkinnslagRepository.lagre(historikkBuilder.build());
    }

}
