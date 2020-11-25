package no.nav.k9.sak.mottak;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.revurdering.NyBehandlingTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@Dependent
public class Behandlingsoppretter {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRevurderingRepository revurderingRepository;
    private SøknadRepository søknadRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<NyBehandlingTjeneste> nyBehandlingTjenester;

    @Inject
    public Behandlingsoppretter(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                InntektArbeidYtelseTjeneste iayTjeneste,
                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                @Any Instance<NyBehandlingTjeneste> nyBehandlingTjenester) { // NOSONAR
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingRepository = behandlingRepositoryProvider.getBehandlingRevurderingRepository();
        this.søknadRepository = behandlingRepositoryProvider.getSøknadRepository();
        this.nyBehandlingTjenester = nyBehandlingTjenester;
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

    /**
     * Opprett ny behandling basert på forrige behandling
     * - Dersom forrige behandling er unntaksbehandling: Opprett ny unntaksbehandling
     * - Ellers opprettes revurdering
     */
    public Behandling opprettNyBehandling(Behandling origBehandling, BehandlingÅrsakType nyBehandlingÅrsak) {
        NyBehandlingTjeneste nyBehandlingTjeneste = getNyBehandlingTjeneste(origBehandling);
        Behandling nyBehandling = nyBehandlingTjeneste.opprettAutomatiskNyBehandling(origBehandling, nyBehandlingÅrsak, behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(origBehandling.getFagsak()));
        return nyBehandling;
    }

    public Behandling oppdaterBehandlingViaHenleggelse(Behandling sisteYtelseBehandling, BehandlingÅrsakType revurderingsÅrsak) {
        henleggBehandling(sisteYtelseBehandling);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(sisteYtelseBehandling.getType())) {
            return opprettNyFørstegangsbehandlingMedInntektsmeldingerOgVedleggFraForrige(revurderingsÅrsak, sisteYtelseBehandling.getFagsak());
        } else {
            Behandling revurdering = opprettNyBehandling(sisteYtelseBehandling, revurderingsÅrsak);

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

    public Behandling opprettNyFørstegangsbehandling(Collection<MottattDokument> mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        var nyFørstegangsbehandling = opprettNyFørstegangsbehandlingFraTidligereSøknad(fagsak, BehandlingÅrsakType.UDEFINERT, avsluttetBehandling);
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
        NyBehandlingTjeneste nyBehandlingTjeneste = getNyBehandlingTjeneste(behandlingMedSøknad);
        nyBehandlingTjeneste.kopierAlleGrunnlagFraTidligereBehandling(behandlingMedSøknad, behandling);
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

    private NyBehandlingTjeneste getNyBehandlingTjeneste(Behandling origBehandling) {
        return BehandlingTypeRef.Lookup.find(NyBehandlingTjeneste.class, nyBehandlingTjenester, origBehandling.getFagsakYtelseType(), origBehandling.getType())
            .orElseThrow(() ->  new UnsupportedOperationException("RevurderingTjeneste ikke implementert for ytelse [" + origBehandling.getFagsakYtelseType() + "], behandlingtype [" + origBehandling.getType() + "]"));
    }
}
