package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingDtoUtil.get;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingDtoUtil.getFraMap;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingDtoUtil.post;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingDtoUtil.setStandardfelter;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AbstractVedtaksbrevOverstyringshåndterer.FPSAK_LAGRE_FRITEKST_INN_FORMIDLING;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingDataTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.klient.TekstFraSaksbehandler;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.sykdom.SykdomRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingsresultatDto;
import no.nav.k9.sak.kontrakt.behandling.ByttBehandlendeEnhetDto;
import no.nav.k9.sak.kontrakt.behandling.GjenopptaBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.HenleggBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.ReåpneBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.behandling.SettBehandlingPaVentDto;
import no.nav.k9.sak.kontrakt.behandling.SkjæringstidspunktDto;
import no.nav.k9.sak.kontrakt.behandling.UtvidetBehandlingDto;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.vedtak.konfig.PropertyUtil;

/**
 * Bygger et sammensatt resultat av BehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */

// FIXME (TEAM SVP) denne burde splittes til å støtte en enkelt ytelse
@ApplicationScoped
public class BehandlingDtoTjeneste {

    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private Unleash unleash;
    private BehandlingRepository behandlingRepository;
    private FormidlingDataTjeneste formidlingDataTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    BehandlingDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                 TilbakekrevingRepository tilbakekrevingRepository,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 FormidlingDataTjeneste formidlingDataTjeneste,
                                 Unleash unleash) {

        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.unleash = unleash;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.formidlingDataTjeneste = formidlingDataTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    private static BehandlingDto lagBehandlingDto(Behandling behandling,
                                                  Optional<BehandlingsresultatDto> behandlingsresultatDto,
                                                  boolean erBehandlingMedGjeldendeVedtak,
                                                  SøknadRepository søknadRepository) {
        var dto = new BehandlingDto();
        setStandardfelter(behandling, dto, erBehandlingMedGjeldendeVedtak);
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository));
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));

        Map<String, String> behandlingUuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        Map<String, String> behandlingIdQueryParams = Map.of(BehandlingIdDto.NAME, behandling.getUuid().toString()); // legacy param name
        
        // Behandlingsmeny-operasjoner
        dto.leggTil(getFraMap(BehandlingRestTjeneste.HANDLING_RETTIGHETER, "handling-rettigheter", behandlingUuidQueryParams));
        dto.leggTil(post(BehandlingRestTjeneste.BYTT_ENHET_PATH, "bytt-behandlende-enhet", new ByttBehandlendeEnhetDto()));
        dto.leggTil(post(BehandlingRestTjeneste.OPNE_FOR_ENDRINGER_PATH, "opne-for-endringer", new ReåpneBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.HENLEGG_PATH, "henlegg-behandling", new HenleggBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.GJENOPPTA_PATH, "gjenoppta-behandling", new GjenopptaBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.SETT_PA_VENT_PATH, "sett-behandling-pa-vent", new SettBehandlingPaVentDto()));

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(getFraMap(KontrollRestTjeneste.KONTROLLRESULTAT_V2_PATH, "kontrollresultat", behandlingIdQueryParams));
            dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_RISIKO_PATH, "risikoklassifisering-aksjonspunkt", behandlingUuidQueryParams));
            dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "lagre-risikoklassifisering-aksjonspunkt", new BekreftedeAksjonspunkterDto()));
        }

        // Totrinnsbehandlin
        if (BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
            dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_PATH, "totrinnskontroll-arsaker", behandlingUuidQueryParams));
            dto.leggTil(post(TotrinnskontrollRestTjeneste.BEKREFT_AKSJONSPUNKT_PATH, "bekreft-totrinnsaksjonspunkt", new BehandlingIdDto(behandling.getUuid())));
        } else if (BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
            dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_READ_ONLY_PATH, "totrinnskontroll-arsaker-readOnly", behandlingUuidQueryParams));
        }

        if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH, "har-apent-kontroller-revurdering-aksjonspunkt", behandlingUuidQueryParams));
            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.HAR_SAMME_RESULTAT_PATH, "har-samme-resultat", behandlingUuidQueryParams));
        }

        // Brev
        dto.leggTil(getFraMap(BrevRestTjeneste.MALER_PATH, "brev-maler", behandlingUuidQueryParams));
        dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill", new BestillBrevDto()));

        return dto;
    }

    private static Språkkode getSpråkkode(Behandling behandling, SøknadRepository søknadRepository) {
        Optional<SøknadEntitet> søknadOpt = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            return søknadOpt.get().getSpråkkode();
        } else {
            return behandling.getFagsak().getNavBruker().getSpråkkode();
        }
    }

    public List<BehandlingDto> lagBehandlingDtoer(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return Collections.emptyList();
        }
        Optional<BehandlingVedtak> gjeldendeVedtak = behandlingVedtakRepository.hentGjeldendeVedtak(behandlinger.get(0).getFagsak());
        Optional<Behandling> behandlingMedGjeldendeVedtak = gjeldendeVedtak.map(bv -> bv.getBehandlingsresultat().getBehandling());
        return behandlinger.stream().map(behandling -> {
            boolean erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(behandling, behandlingMedGjeldendeVedtak);
            var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
            return lagBehandlingDto(behandling, behandlingsresultatDto, erBehandlingMedGjeldendeVedtak, søknadRepository);
        }).collect(Collectors.toList());
    }

    private boolean erBehandlingMedGjeldendeVedtak(Behandling behandling, Optional<Behandling> behandlingMedGjeldendeVedtak) {
        if (behandlingMedGjeldendeVedtak.isEmpty()) {
            return false;
        }
        return Objects.equals(behandlingMedGjeldendeVedtak.get().getId(), behandling.getId());
    }

    public UtvidetBehandlingDto lagUtvidetBehandlingDto(Behandling behandling, AsyncPollingStatus asyncStatus) {
        Optional<Behandling> sisteAvsluttedeIkkeHenlagteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());
        UtvidetBehandlingDto dto = mapFra(behandling, erBehandlingMedGjeldendeVedtak(behandling, sisteAvsluttedeIkkeHenlagteBehandling));
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }
        return dto;
    }

    public UtvidetBehandlingDto lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(Behandling originalBehandling) {
        var dto = new UtvidetBehandlingDto();

        Optional<Behandling> sisteAvsluttedeIkkeHenlagteBehandling = behandlingRepository
            .finnSisteAvsluttedeIkkeHenlagteBehandling(originalBehandling.getFagsakId());
        var erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(originalBehandling, sisteAvsluttedeIkkeHenlagteBehandling);
        setStandardfelter(originalBehandling, dto, erBehandlingMedGjeldendeVedtak);
        var behandlingsresultatDto = lagBehandlingsresultatDto(originalBehandling);
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));

        var queryParams = Map.of(BehandlingUuidDto.NAME, originalBehandling.getUuid().toString());
        dto.leggTil(getFraMap(SøknadRestTjeneste.SOKNAD_PATH, "soknad", queryParams));

        // FIXME K9 urler og uttak
        dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.BEREGNINGSRESULTAT_PATH, "beregningsresultat", queryParams));

        return dto;
    }

    private void settStandardfelterUtvidet(Behandling behandling, UtvidetBehandlingDto dto, boolean erBehandlingMedGjeldendeVedtak) {
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, dto, erBehandlingMedGjeldendeVedtak);
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository));
        var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));
    }

    private UtvidetBehandlingDto mapFra(Behandling behandling, boolean erBehandlingMedGjeldendeVedtak) {
        UtvidetBehandlingDto dto = new UtvidetBehandlingDto();
        settStandardfelterUtvidet(behandling, dto, erBehandlingMedGjeldendeVedtak);

        dto.leggTil(get(FagsakRestTjeneste.PATH, "fagsak", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));

        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_V2_PATH, "aksjonspunkter", queryParams));
        dto.leggTil(getFraMap(VilkårRestTjeneste.V2_PATH, "vilkar", queryParams));

        return utvideBehandlingDto(behandling, dto);
    }

    private UtvidetBehandlingDto utvideBehandlingDto(Behandling behandling, UtvidetBehandlingDto dto) {
        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        // mapping ved hjelp av tjenester
        dto.leggTil(getFraMap(SøknadRestTjeneste.SOKNAD_PATH, "soknad", queryParams));
        dto.leggTil(getFraMap(DokumentRestTjeneste.MOTTATT_DOKUMENTER_PATH, "mottattdokument", queryParams));

        dto.leggTil(getFraMap(PersonRestTjeneste.PERSONOPPLYSNINGER_PATH, "soeker-personopplysninger", queryParams));

        dto.leggTil(getFraMap(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "soeker-medlemskap-v2", queryParams));
        // TODO (TOR) Legg til else her når frontend ikkje lenger feilaktig brukar både ny og gammal versjon
        dto.leggTil(getFraMap(PersonRestTjeneste.MEDLEMSKAP_PATH, "soeker-medlemskap", queryParams));

        if (BehandlingType.REVURDERING.equals(behandling.getType()) && BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
            dto.leggTil(getFraMap(BrevRestTjeneste.VARSEL_REVURDERING_PATH, "sendt-varsel-om-revurdering", queryParams));
        }

        dto.leggTil(getFraMap(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_PATH, "inntekt-arbeid-ytelse", queryParams));
        dto.leggTil(getFraMap(SykdomRestTjeneste.SYKDOMS_OPPLYSNINGER_PATH, "sykdom", queryParams));

        dto.leggTil(getFraMap(OpptjeningRestTjeneste.PATH, "opptjening", queryParams));

        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (beregningsgrunnlag.isPresent()) {
            dto.leggTil(getFraMap(BeregningsgrunnlagRestTjeneste.PATH, "beregningsgrunnlag", queryParams));
        }

        lagTilbakekrevingValgLink(behandling).ifPresent(dto::leggTil);
        lagSimuleringResultatLink(behandling).ifPresent(dto::leggTil);

        behandling.getOriginalBehandling().ifPresent(originalBehandling -> {
            var originalQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
            dto.leggTil(getFraMap(BehandlingRestTjeneste.REVURDERING_ORGINAL_PATH, "original-behandling", originalQueryParams));
        });

        return dto;
    }

    private Optional<BehandlingsresultatDto> lagBehandlingsresultatDto(Behandling behandling) {
        var behandlingsresultat = behandling.getBehandlingsresultat();
        if (behandlingsresultat == null) {
            return Optional.empty();
        }
        BehandlingsresultatDto dto = new BehandlingsresultatDto();
        Optional<TekstFraSaksbehandler> tekstFraSaksbehandlerOptional = Optional.empty();
        dto.setId(behandlingsresultat.getId());
        dto.setType(behandlingsresultat.getBehandlingResultatType());
        dto.setAvslagsarsak(behandlingsresultat.getAvslagsårsak());
        dto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
        dto.setSkjæringstidspunkt(finnSkjæringstidspunktForBehandling(behandling).orElse(null));
        dto.setErRevurderingMedUendretUtfall(erRevurderingMedUendretUtfall(behandling));
        if (unleash.isEnabled(FPSAK_LAGRE_FRITEKST_INN_FORMIDLING)) {
            tekstFraSaksbehandlerOptional = formidlingDataTjeneste.hentSaksbehandlerTekst(behandling.getUuid());
        }
        // For å støtte eksiterende data inn i FPSAK
        if (tekstFraSaksbehandlerOptional.isPresent()) {
            final TekstFraSaksbehandler tekstFraSaksbehandlerDto = tekstFraSaksbehandlerOptional.get();
            dto.setAvslagsarsakFritekst(tekstFraSaksbehandlerDto.getAvslagarsakFritekst());
            dto.setVedtaksbrev(tekstFraSaksbehandlerDto.getVedtaksbrev());
            dto.setOverskrift(tekstFraSaksbehandlerDto.getOverskrift());
            dto.setFritekstbrev(tekstFraSaksbehandlerDto.getFritekstbrev());
        } else {
            dto.setAvslagsarsakFritekst(behandlingsresultat.getAvslagarsakFritekst());
            dto.setVedtaksbrev(behandlingsresultat.getVedtaksbrev());
            dto.setOverskrift(behandlingsresultat.getOverskrift());
            dto.setFritekstbrev(behandlingsresultat.getFritekstbrev());
        }
        return Optional.of(dto);
    }

    private boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow()
            .erRevurderingMedUendretUtfall(behandling);
    }

    private Optional<SkjæringstidspunktDto> finnSkjæringstidspunktForBehandling(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return Optional.empty();
        }
        Optional<LocalDate> skjæringstidspunktHvisUtledet = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId())
            .getSkjæringstidspunktHvisUtledet();
        if (skjæringstidspunktHvisUtledet.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SkjæringstidspunktDto(skjæringstidspunktHvisUtledet.get()));
    }

    private Optional<ResourceLink> lagSimuleringResultatLink(Behandling behandling) {
        String fpoppdragOverrideUrl = PropertyUtil.getProperty("fpoppdrag.override.proxy.url");
        String baseUurl = fpoppdragOverrideUrl != null ? fpoppdragOverrideUrl : "/k9/oppdrag/api";
        return Optional.of(ResourceLink.post(baseUurl + "/simulering/resultat-uten-inntrekk", "simuleringResultat", behandling.getUuid()));
    }

    private Optional<ResourceLink> lagTilbakekrevingValgLink(Behandling behandling) {
        Map<String, String> queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        return tilbakekrevingRepository.hent(behandling.getId()).isPresent()
            ? Optional.of(getFraMap(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekrevingvalg", queryParams))
            : Optional.empty();
    }

    public Boolean finnBehandlingOperasjonRettigheter(Behandling behandling) {
        return this.søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).isPresent();
    }
}
