package no.nav.k9.sak.mottak;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@Dependent
public class Behandlingsoppretter {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRevurderingRepository revurderingRepository;
    private SøknadRepository søknadRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public Behandlingsoppretter(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                InntektArbeidYtelseTjeneste iayTjeneste,
                                MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingRepository = behandlingRepositoryProvider.getBehandlingRevurderingRepository();
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
            OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
            beh.setBehandlendeEnhet(enhet);
        }); // NOSONAR
    }

    public Behandling opprettNyFørstegangsbehandlingMedInntektsmeldingerOgVedleggFraForrige(BehandlingÅrsakType behandlingÅrsakType, Fagsak fagsak) {
        Behandling forrigeBehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD)
            .orElseThrow(() -> new IllegalStateException("Fant ingen behandling som passet for saksnummer: " + fagsak.getSaksnummer()));
        Behandling nyFørstegangsbehandling = opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(forrigeBehandling));
        opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(forrigeBehandling, nyFørstegangsbehandling);
        return nyFørstegangsbehandling;
    }

    public Behandling opprettRevurdering(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, origBehandling.getFagsakYtelseType()).orElseThrow();
        Behandling revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, revurderingsÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(origBehandling.getFagsak()));
        return revurdering;
    }


    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        henleggBehandling(sisteYtelseBehandling);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(sisteYtelseBehandling.getType())) {
            return opprettNyFørstegangsbehandlingMedInntektsmeldingerOgVedleggFraForrige(revurderingsÅrsak, sisteYtelseBehandling.getFagsak());
        } else {
            Behandling revurdering = opprettRevurdering(sisteYtelseBehandling, revurderingsÅrsak);

            opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(sisteYtelseBehandling, revurdering);

            // Kopier behandlingsårsaker fra forrige behandling
            new BehandlingÅrsak.Builder(sisteYtelseBehandling.getBehandlingÅrsaker().stream()
                .map(BehandlingÅrsak::getBehandlingÅrsakType)
                .collect(toList()))
                    .buildFor(revurdering);

            BehandlingskontrollKontekst nyKontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
            behandlingRepository.lagre(revurdering, nyKontekst.getSkriveLås());

            return revurdering;
        }
    }

    public void henleggBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.MERGET_OG_HENLAGT);
    }

    public void opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(Behandling forrigeBehandling, Behandling nyBehandling) {
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(forrigeBehandling.getId(), nyBehandling.getId());
    }

    public boolean harBehandlingsresultatOpphørt(Behandling behandling) {
        return behandling.getBehandlingResultatType().isBehandlingsresultatOpphørt();
    }

    public boolean erAvslåttBehandling(Behandling behandling) {
        return behandling.getBehandlingResultatType().isBehandlingsresultatAvslått();
    }

    public Behandling opprettNyFørstegangsbehandling(Collection<MottattDokument> mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        var nyFørstegangsbehandling = opprettNyFørstegangsbehandlingFraTidligereSøknad(fagsak, BehandlingÅrsakType.UDEFINERT, avsluttetBehandling);
        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(nyFørstegangsbehandling, mottattDokument);
        return nyFørstegangsbehandling;
    }

    public Behandling opprettNyFørstegangsbehandlingFraTidligereSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling behandlingMedSøknad) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());
        boolean harÅpenBehandling = !sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.TRUE);
        Behandling behandling = harÅpenBehandling ? oppdaterBehandlingViaHenleggelse(sisteYtelsesbehandling.get(), behandlingÅrsakType)
            : opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.of(behandlingMedSøknad));

        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingMedSøknad.getId());
        if (søknad.isPresent()) {
            søknadRepository.lagreOgFlush(behandling, søknad.get());
        }
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        revurderingTjeneste.kopierAlleGrunnlagFraTidligereBehandling(behandlingMedSøknad, behandling);
        return behandling;
    }

    public Optional<Behandling> sisteHenlagteFørstegangsbehandling(Fagsak fagsak) {
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandling.isPresent()
            && behandling.get().erStatusFerdigbehandlet()
            && behandling.get().erHenlagt()) {

            return behandlingRepository
                .hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD)
                .filter(v -> v.getBehandlingResultatType().isBehandlingsresultatHenlagt());
        }

        return Optional.empty();
    }
}
