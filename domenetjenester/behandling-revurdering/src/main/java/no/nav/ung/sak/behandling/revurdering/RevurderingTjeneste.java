package no.nav.ung.sak.behandling.revurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
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
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private FagsakLåsRepository fagsakLåsRepository;
    private KontrollbehandlingBryter kontrollbehandlingBryter;

    public RevurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                               RevurderingTjenesteFelles revurderingTjenesteFelles,
                               @Any Instance<GrunnlagKopierer> grunnlagKopierere,
                               HistorikkinnslagRepository historikkinnslagRepository,
                               ProsessTriggereRepository prosessTriggereRepository,
                               BehandlingAnsvarligRepository behandlingAnsvarligRepository,
                               BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                               FagsakLåsRepository fagsakLåsRepository,
                               KontrollbehandlingBryter kontrollbehandlingBryter) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.grunnlagKopierere = grunnlagKopierere;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.fagsakLåsRepository = fagsakLåsRepository;
        this.kontrollbehandlingBryter = kontrollbehandlingBryter;
    }

    public Behandling opprettManuellRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, Optional<DatoIntervallEntitet> periode) {
        validerTilstand(origBehandling);
        Behandling revurdering = opprettRevurdering(origBehandling, revurderingsÅrsak, true);

        DatoIntervallEntitet revurderingPeriode = periode.orElse(revurdering.getFagsak().getPeriode());
        prosessTriggereRepository.leggTil(revurdering.getId(), Set.of(new Trigger(revurderingsÅrsak, revurderingPeriode)));

        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedManuellOpprettelse(origBehandling, revurdering);

        return revurdering;
    }

    public Behandling opprettAutomatiskRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        validerTilstand(origBehandling);
        var revurdering = opprettRevurdering(origBehandling, revurderingsÅrsak, false);

        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedAutomatiskOpprettelse(origBehandling, revurdering);

        return revurdering;
    }

    /**
     * Oppretter kontrollbehandling fra siste vedtak på fagsaken. Kontrollbehandlingen får kopi av vilkårsresultat og
     * grunnlag fra vedtaket, og kan behandles parallelt med en åpen ordinær behandling.
     * <p>
     * Originalbehandlingen låses før fagsaken, i samme rekkefølge som når en revurdering opprettes. Under fagsaklåsen
     * sjekkes det at originalbehandlingen er siste vedtak, og at fagsaken ikke har en kontrollbehandling som ikke er
     * avsluttet. Den unike indeksen for åpen kontrollbehandling stopper resten.
     */
    public Behandling opprettKontrollbehandling(Behandling origBehandling, BehandlingÅrsakType årsak, boolean manueltOpprettet) {
        var ytelseType = origBehandling.getFagsakYtelseType();
        if (!kontrollbehandlingBryter.erPå(ytelseType)) {
            throw new IllegalStateException("Kontrollbehandling er ikke slått på for " + ytelseType.getKode());
        }
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);
        fagsakLåsRepository.taLås(origBehandling.getFagsakId());
        if (!revurderingTjenesteFelles.erSisteVedtak(origBehandling)) {
            throw new IllegalStateException("Kontrollbehandling må opprettes fra siste vedtak på fagsaken. Behandling "
                + origBehandling.getId() + " er ikke siste vedtak på fagsak " + origBehandling.getFagsakId());
        }
        if (!revurderingTjenesteFelles.kanKontrollbehandlingOpprettes(origBehandling.getFagsak())) {
            throw new IllegalStateException("Kan ikke opprette kontrollbehandling på fagsak " + origBehandling.getFagsakId()
                + ". Fagsaken har allerede en kontrollbehandling som ikke er avsluttet.");
        }
        var kontrollbehandling = opprettNyBehandlingFraVedtak(origBehandling, BehandlingType.KONTROLL, årsak, manueltOpprettet);

        getGrunnlagKopierer(ytelseType).kopierGrunnlagVedAutomatiskOpprettelse(origBehandling, kontrollbehandling);

        return kontrollbehandling;
    }

    /**
     * Oppretter kontrollbehandling for kontroll av inntekt i én periode, for eksempel når saksbehandler ber om kontroll.
     * Er behandlingen manuelt opprettet, får saksbehandler alltid aksjonspunkt for kontroll av inntekt, som i en manuelt
     * opprettet revurdering for kontroll i dag.
     */
    public Behandling opprettKontrollbehandling(Behandling origBehandling, DatoIntervallEntitet periode, boolean manueltOpprettet) {
        var årsak = BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT;
        var kontrollbehandling = opprettKontrollbehandling(origBehandling, årsak, manueltOpprettet);
        prosessTriggereRepository.leggTil(kontrollbehandling.getId(), Set.of(new Trigger(årsak, periode)));
        return kontrollbehandling;
    }

    public boolean kanKontrollbehandlingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanKontrollbehandlingOpprettes(fagsak);
    }

    private Behandling opprettRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet) {
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);
        return opprettNyBehandlingFraVedtak(origBehandling, BehandlingType.REVURDERING, revurderingsÅrsak, manueltOpprettet);
    }

    private Behandling opprettNyBehandlingFraVedtak(Behandling origBehandling, BehandlingType behandlingType, BehandlingÅrsakType årsak, boolean manueltOpprettet) {
        // Hentes før behandlingen opprettes, ellers finnes ingen forrige behandling å gjenbruke enhet fra
        var behandlendeEnheter = behandlendeEnhetTjeneste.finnBehandlendeEnheterFor(origBehandling.getFagsak());

        Behandling nyBehandling = revurderingTjenesteFelles.opprettNyBehandling(behandlingType, årsak, origBehandling, manueltOpprettet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(nyBehandling);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, nyBehandling);

        behandlingAnsvarligRepository.setBehandlendeEnheter(nyBehandling.getId(), behandlendeEnheter);

        // Kopier vilkår (samme vilkår vurderes i Revurdering)
        revurderingTjenesteFelles.kopierVilkårsresultat(origBehandling, nyBehandling, kontekst);

        opprettHistorikkinnslag(nyBehandling, årsak, manueltOpprettet);

        return nyBehandling;
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
        historikkBuilder.medTittel(behandling.erKontrollbehandling() ? "Kontrollbehandling opprettet" : "Revurdering opprettet")
            .medBehandlingId(behandling.getId())
            .addLinje(revurderingÅrsak.getNavn())
            .medFagsakId(behandling.getFagsakId())
            .medAktør(historikkAktør);

        historikkinnslagRepository.lagre(historikkBuilder.build());
    }

}
