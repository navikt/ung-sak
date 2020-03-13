package no.nav.foreldrepenger.mottak;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.DokumentPersistererTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class Behandlingsoppretter {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentPersistererTjeneste dokumentPersistererTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRevurderingRepository revurderingRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private VedtakVarselRepository vedtakVarselRepository;
    private SøknadRepository søknadRepository;

    public Behandlingsoppretter() {
        // For CDI
    }

    @Inject
    public Behandlingsoppretter(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                VedtakVarselRepository vedtakVarselRepository,
                                BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                DokumentPersistererTjeneste dokumentPersistererTjeneste,
                                MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                HistorikkinnslagTjeneste historikkinnslagTjeneste) { // NOSONAR
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentPersistererTjeneste = dokumentPersistererTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingRepository = behandlingRepositoryProvider.getBehandlingRevurderingRepository();
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.søknadRepository = behandlingRepositoryProvider.getSøknadRepository();
    }

    public boolean erKompletthetssjekkPassert(Behandling behandling) {
        return behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.VURDER_KOMPLETTHET);
    }

    /**
     * Opprett og Oppdater under vil opprette behandling og kopiere grunnlag, men ikke opprette start/fortsett tasks.
     */
    public Behandling opprettFørstegangsbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> tidligereBehandling) {
        BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
        if (!tidligereBehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(true)) {
            throw new IllegalStateException("Utviklerfeil: Prøver opprette ny behandling når det finnes åpen av samme type: " + fagsak.getId());
        }
        return behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingType, (beh) -> {
            if (!BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
                BehandlingÅrsak.builder(behandlingÅrsakType).buildFor(beh);
            }
            beh.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()));
            OrganisasjonsEnhet enhet = tidligereBehandling.map(b -> utledEnhetFraTidligereBehandling(b).orElse(b.getBehandlendeOrganisasjonsEnhet()))
                .orElse(finnBehandlendeEnhet(beh));
            beh.setBehandlendeEnhet(enhet);
        }); // NOSONAR
    }

    public Behandling opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(BehandlingÅrsakType behandlingÅrsakType, Fagsak fagsak) {
        Behandling forrigeBehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD)
            .orElseThrow(() -> new IllegalStateException("Fant ingen behandling som passet for saksnummer: " + fagsak.getSaksnummer()));
        Behandling nyFørstegangsbehandling = opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(forrigeBehandling));
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(fagsak.getSaksnummer(), nyFørstegangsbehandling);
        return nyFørstegangsbehandling;
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        Behandling revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.sjekkEnhetVedNyAvledetBehandling(fagsak));
        return revurdering;
    }

    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        Behandling revurdering = revurderingTjeneste.opprettManuellRevurdering(fagsak, revurderingsÅrsak, behandlendeEnhetTjeneste.sjekkEnhetVedNyAvledetBehandling(fagsak));
        return revurdering;
    }

    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        henleggBehandling(sisteYtelseBehandling);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(sisteYtelseBehandling.getType())) {
            return opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(revurderingsÅrsak, sisteYtelseBehandling.getFagsak());
        }
        Behandling revurdering = opprettRevurdering(sisteYtelseBehandling.getFagsak(), revurderingsÅrsak);

        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(sisteYtelseBehandling.getFagsak().getSaksnummer(), revurdering);

        // Kopier behandlingsårsaker fra forrige behandling
        new BehandlingÅrsak.Builder(sisteYtelseBehandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .collect(toList()))
                .buildFor(revurdering);

        BehandlingskontrollKontekst nyKontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingRepository.lagre(revurdering, nyKontekst.getSkriveLås());

        return revurdering;
    }

    public void henleggBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.MERGET_OG_HENLAGT);
    }

    public void opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(@SuppressWarnings("unused") Saksnummer saksnummer, Behandling nyBehandling) {
        hentAlleInntektsmeldingdokumenter(nyBehandling.getFagsakId()).stream()
            .sorted(MottattDokumentSorterer.sorterMottattDokument())
            .forEach(mottattDokument -> dokumentPersistererTjeneste.persisterDokumentinnhold(mottattDokument, nyBehandling));

    }

    private List<MottattDokument> hentAlleInntektsmeldingdokumenter(Long fagsakId) {
        return mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsakId).stream().collect(toList());
    }

    private OrganisasjonsEnhet finnBehandlendeEnhet(Behandling behandling) {
        return behandlendeEnhetTjeneste.finnBehandlendeEnhetFraSøker(behandling);
    }

    private Optional<OrganisasjonsEnhet> utledEnhetFraTidligereBehandling(Behandling tidligereBehandling) {
        // Utleder basert på regler rundt sakskompleks og diskresjonskoder. Vil bruke forrige enhet med mindre noen tilsier Kode6 eller enhet
        // opphørt
        return behandlendeEnhetTjeneste.sjekkEnhetVedNyAvledetBehandling(tidligereBehandling);
    }

    public boolean harBehandlingsresultatOpphørt(Behandling behandling) {
        return behandling.getBehandlingResultatType().isBehandlingsresultatOpphørt();
    }

    public boolean erAvslåttBehandling(Behandling behandling) {
        return behandling.getBehandlingResultatType().isBehandlingsresultatAvslått();
    }

    public Behandling opprettNyFørstegangsbehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, DokumentTypeId dokumentTypeId) {
        Behandling behandling;
        // Ny førstegangssøknad
        if (dokumentTypeId == null) {
            behandling = opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.of(avsluttetBehandling));
            historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, mottattDokument.getJournalpostId());
        } else {
            behandling = opprettNyFørstegangsbehandlingFraTidligereSøknad(fagsak, BehandlingÅrsakType.UDEFINERT, avsluttetBehandling);
            historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), dokumentTypeId);
        }
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        return behandling;
    }

    public Behandling opprettNyFørstegangsbehandlingFraTidligereSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling behandlingMedSøknad) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());
        boolean harÅpenBehandling = !sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        Behandling behandling = harÅpenBehandling ? oppdaterBehandlingViaHenleggelse(sisteYtelsesbehandling.get(), behandlingÅrsakType)
            : opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(behandlingMedSøknad));

        SøknadEntitet søknad = søknadRepository.hentSøknad(behandlingMedSøknad);
        if (søknad != null) {
            søknadRepository.lagreOgFlush(behandling, søknad);
        }
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        revurderingTjeneste.kopierAlleGrunnlagFraTidligereBehandling(behandlingMedSøknad, behandling);
        return behandling;
    }

    public boolean erBehandlingOgFørstegangsbehandlingHenlagt(Fagsak fagsak) {
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        Optional<VedtakVarsel> behandlingsresultat = behandling.flatMap(b -> vedtakVarselRepository.hentHvisEksisterer(b.getId()));
        if (behandlingsresultat.isPresent() && behandling.get().getBehandlingResultatType().isBehandlingsresultatHenlagt()) {
            Optional<Behandling> førstegangsbehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD);
            Optional<VedtakVarsel> førstegangsbehandlingBehandlingsresultat = førstegangsbehandling.flatMap(b -> vedtakVarselRepository.hentHvisEksisterer(b.getId()));
            return førstegangsbehandlingBehandlingsresultat.isPresent() && førstegangsbehandling.get().getBehandlingResultatType().isBehandlingsresultatHenlagt();
        }
        return false;
    }
}
