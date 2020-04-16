package no.nav.k9.sak.web.app.tjenester.behandling;

import static no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil.get;
import static no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil.getFraMap;
import static no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil.post;
import static no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil.setStandardfelter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.ResourceLink.HttpMethod;
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
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårResultatDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorg.OmsorgenForRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

/**
 * Bygger et sammensatt resultat av BehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */
@Dependent
public class BehandlingDtoTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttakRepository uttakRepository;

    /**
     * denne kan overstyres for testing lokalt
     */
    private String k9OppdragProxyUrl;

    BehandlingDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingDtoTjeneste(FagsakRepository fagsakRepository,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingVedtakRepository behandlingVedtakRepository,
                                 SøknadRepository søknadRepository,
                                 UttakRepository uttakRepository,
                                 TilbakekrevingRepository tilbakekrevingRepository,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 VilkårResultatRepository vilkårResultatRepository,
                                 @KonfigVerdi(value = "k9.oppdrag.proxy.url") String k9OppdragProxyUrl) {

        this.fagsakRepository = fagsakRepository;
        this.uttakRepository = uttakRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadRepository = søknadRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.k9OppdragProxyUrl = k9OppdragProxyUrl;
    }

    private static Språkkode getSpråkkode(Behandling behandling, SøknadRepository søknadRepository) {
        Optional<SøknadEntitet> søknadOpt = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            return søknadOpt.get().getSpråkkode();
        } else {
            return Språkkode.nb; // Defaulter
        }
    }

    private BehandlingDto lagBehandlingDto(Behandling behandling,
                                           BehandlingsresultatDto behandlingsresultatDto,
                                           boolean erBehandlingMedGjeldendeVedtak) {
        var dto = new BehandlingDto();
        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElse(null);
        BehandlingDtoUtil.setStandardfelter(behandling, dto, behandlingVedtak, erBehandlingMedGjeldendeVedtak);
        initBehandlingResourceLinks(behandling, behandlingsresultatDto, dto);

        return dto;
    }

    private void initBehandlingResourceLinks(Behandling behandling, BehandlingsresultatDto behandlingsresultatDto, BehandlingDto dto) {
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository));
        dto.setBehandlingsresultat(behandlingsresultatDto);

        leggTilGrunnlagResourceLinks(behandling, dto);
        leggTilStatusResultaterLinks(behandling, dto);
        leggTilHandlingerResourceLinks(behandling, dto);
    }

    private void leggTilStatusResultaterLinks(Behandling behandling, BehandlingDto dto) {
        var idQueryParams = Map.of(BehandlingIdDto.NAME, behandling.getUuid().toString()); // legacy param name
        var uuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());

        dto.leggTil(getFraMap(BehandlingRestTjeneste.HANDLING_RETTIGHETER, "handling-rettigheter", uuidQueryParams));

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(getFraMap(KontrollRestTjeneste.KONTROLLRESULTAT_V2_PATH, "kontrollresultat", idQueryParams));
        } else if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.HAR_SAMME_RESULTAT_PATH, "har-samme-resultat", uuidQueryParams));
        }

    }

    private void leggTilHandlingerResourceLinks(Behandling behandling, BehandlingDto dto) {

        if (behandling.getStatus().erFerdigbehandletStatus()) {
            return; // skip resten hvis ferdig
        }

        // Behandlingsmeny-operasjoner
        dto.leggTil(post(BehandlingRestTjeneste.BYTT_ENHET_PATH, "bytt-behandlende-enhet", new ByttBehandlendeEnhetDto()));
        dto.leggTil(post(BehandlingRestTjeneste.OPNE_FOR_ENDRINGER_PATH, "opne-for-endringer", new ReåpneBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.HENLEGG_PATH, "henlegg-behandling", new HenleggBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.GJENOPPTA_PATH, "gjenoppta-behandling", new GjenopptaBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.SETT_PA_VENT_PATH, "sett-behandling-pa-vent", new SettBehandlingPaVentDto()));

        var uuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_RISIKO_PATH, "risikoklassifisering-aksjonspunkt", uuidQueryParams));
            dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "lagre-risikoklassifisering-aksjonspunkt", new BekreftedeAksjonspunkterDto()));
        } else if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH, "har-apent-kontroller-revurdering-aksjonspunkt", uuidQueryParams));
        }

        // Totrinnsbehandlin
        if (BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
            dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_PATH, "totrinnskontroll-arsaker", uuidQueryParams));
            dto.leggTil(
                post(TotrinnskontrollRestTjeneste.BEKREFT_AKSJONSPUNKT_PATH, "bekreft-totrinnsaksjonspunkt", new BehandlingIdDto(behandling.getUuid())));
        } else if (BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
            dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_READ_ONLY_PATH, "totrinnskontroll-arsaker-readOnly", uuidQueryParams));
        }

        // Brev
        dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill", new BestillBrevDto()));
    }

    public List<BehandlingDto> lagBehandlingDtoer(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return Collections.emptyList();
        }
        Optional<BehandlingVedtak> gjeldendeVedtak = behandlingVedtakRepository.hentGjeldendeVedtak(behandlinger.get(0).getFagsak());
        Optional<Long> behandlingMedGjeldendeVedtak = gjeldendeVedtak.map(bv -> bv.getBehandlingId());
        return behandlinger.stream().map(behandling -> {
            boolean erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(behandling, behandlingMedGjeldendeVedtak);
            var behandlingsresultat = lagBehandlingsresultat(behandling);
            return lagBehandlingDto(behandling, behandlingsresultat, erBehandlingMedGjeldendeVedtak);
        }).collect(Collectors.toList());
    }

    BehandlingsresultatDto lagBehandlingsresultat(Behandling behandling) {
        var skjæringstidspunkt = finnSkjæringstidspunkt(behandling);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var behandlingsresultat = lagBehandlingsresultat(ref);
        return behandlingsresultat;
    }

    BehandlingsresultatDto lagBehandlingsresultat(BehandlingReferanse ref) {
        var dto = new BehandlingsresultatDto();

        Long behandlingId = ref.getBehandlingId();

        dto.setResultatType(ref.getBehandlingResultat());

        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (vilkårene.isPresent()) {
            var vilkårResultater = vilkårene.get().getVilkårene().stream()
                .flatMap(vt -> vt.getPerioder().stream())
                .map(vp -> new AbstractMap.SimpleEntry<>(vp.getVilkårType(),
                    new VilkårResultatDto(new Periode(vp.getFom(), vp.getTom()), vp.getAvslagsårsak(), vp.getUtfall())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
            dto.setVilkårResultat(vilkårResultater);
        }

        dto.setSkjæringstidspunkt(finnSkjæringstidspunktForBehandling(ref).orElse(null));
        dto.setErRevurderingMedUendretUtfall(erRevurderingMedUendretUtfall(ref));

        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakFor(ref.getBehandlingUuid());
        behandlingVedtak.ifPresent(bv -> dto.setVedtaksdato(bv.getVedtaksdato()));

        return dto;
    }

    boolean erBehandlingMedGjeldendeVedtak(Behandling behandling, Optional<Long> behandlingMedGjeldendeVedtak) {
        if (behandlingMedGjeldendeVedtak.isEmpty()) {
            return false;
        }
        return Objects.equals(behandlingMedGjeldendeVedtak.get(), behandling.getId());
    }

    public BehandlingDto lagUtvidetBehandlingDto(Behandling behandling, AsyncPollingStatus asyncStatus) {
        Optional<Behandling> sisteAvsluttedeIkkeHenlagteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());
        BehandlingDto dto = mapFra(behandling, erBehandlingMedGjeldendeVedtak(behandling, sisteAvsluttedeIkkeHenlagteBehandling.map(Behandling::getId)));
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }
        return dto;
    }

    public BehandlingDto lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(Behandling originalBehandling) {
        var dto = new BehandlingDto();

        Optional<Behandling> sisteAvsluttedeIkkeHenlagteBehandling = behandlingRepository
            .finnSisteAvsluttedeIkkeHenlagteBehandling(originalBehandling.getFagsakId());

        var erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(originalBehandling, sisteAvsluttedeIkkeHenlagteBehandling.map(Behandling::getId));
        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(originalBehandling.getId()).orElse(null);
        setStandardfelter(originalBehandling, dto, behandlingVedtak, erBehandlingMedGjeldendeVedtak);

        var behandlingsresultatDto = lagBehandlingsresultat(originalBehandling);
        initBehandlingResourceLinks(originalBehandling, behandlingsresultatDto, dto);

        return dto;
    }

    private void settStandardfelterUtvidet(Behandling behandling, BehandlingDto dto, boolean erBehandlingMedGjeldendeVedtak) {
        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElse(null);

        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, dto, behandlingVedtak, erBehandlingMedGjeldendeVedtak);
    }

    BehandlingDto mapFra(Behandling behandling, boolean erBehandlingMedGjeldendeVedtak) {
        BehandlingDto dto = new BehandlingDto();
        settStandardfelterUtvidet(behandling, dto, erBehandlingMedGjeldendeVedtak);
        var behandlingsresultat = lagBehandlingsresultat(behandling);
        initBehandlingResourceLinks(behandling, behandlingsresultat, dto);

        return dto;
    }

    private void leggTilGrunnlagResourceLinks(Behandling behandling, BehandlingDto dto) {
        var uuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());

        leggTilYtelsespesifikkResourceLinks(behandling, dto);

        dto.leggTil(get(FagsakRestTjeneste.PATH, "fagsak", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));
        dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_V2_PATH, "aksjonspunkter", uuidQueryParams));
        dto.leggTil(getFraMap(VilkårRestTjeneste.V2_PATH, "vilkar", uuidQueryParams));
        dto.leggTil(getFraMap(VilkårRestTjeneste.V3_PATH, "vilkar-v3", uuidQueryParams));

        dto.leggTil(getFraMap(SøknadRestTjeneste.SOKNAD_PATH, "soknad", uuidQueryParams));

        dto.leggTil(getFraMap(PersonRestTjeneste.PERSONOPPLYSNINGER_PATH, "soeker-personopplysninger", uuidQueryParams));

        dto.leggTil(getFraMap(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "soeker-medlemskap-v2", uuidQueryParams));

        dto.leggTil(getFraMap(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_PATH, "inntekt-arbeid-ytelse", uuidQueryParams));
        dto.leggTil(getFraMap(SykdomRestTjeneste.SYKDOMS_OPPLYSNINGER_PATH, "sykdom", uuidQueryParams));
        dto.leggTil(getFraMap(OmsorgenForRestTjeneste.OMSORGEN_FOR_OPPLYSNINGER_PATH, "omsorgen-for", uuidQueryParams));

        dto.leggTil(getFraMap(OpptjeningRestTjeneste.PATH, "opptjening", uuidQueryParams));
        dto.leggTil(getFraMap(OpptjeningRestTjeneste.PATH_V2, "opptjening-v2", uuidQueryParams));

        dto.leggTil(getFraMap(BrevRestTjeneste.HENT_VEDTAKVARSEL_PATH, "vedtak-varsel", uuidQueryParams));

        dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.BEREGNINGSRESULTAT_PATH, "beregningsresultat", uuidQueryParams));

        lagVarselOmRevurderingLink(behandling).ifPresent(dto::leggTil);
        lagBeregningsgrunnlagLink(behandling).ifPresent(dto::leggTil);
        lagSimuleringResultatLink(behandling).ifPresent(dto::leggTil);
        lagOriginalBehandlingLink(behandling).ifPresent(dto::leggTil);

        lagTilbakekrevingValgLink(behandling).forEach(dto::leggTil);
    }

    private void leggTilYtelsespesifikkResourceLinks(Behandling behandling, BehandlingDto dto) {
        leggTilUttakEndepunkt(behandling, dto);
    }

    private void leggTilUttakEndepunkt(Behandling behandling, BehandlingDto dto) {

        UUID behandlingUuid = behandling.getUuid();
        var behandlingUuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandlingUuid.toString());

        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingUuid).map(Søknadsperioder::getMaksPeriode);

        Fagsak fagsak = behandling.getFagsak();
        var fom = søknadsperioder.map(DatoIntervallEntitet::getFomDato).orElse(fagsak.getPeriode().getFomDato());
        var tom = søknadsperioder.map(DatoIntervallEntitet::getTomDato).orElse(fagsak.getPeriode().getTomDato());

        var andreSaker = fagsakRepository.finnFagsakRelatertTil(fagsak.getYtelseType(), fagsak.getPleietrengendeAktørId(), fom, tom)
                .stream().map(Fagsak::getSaksnummer)
                .collect(Collectors.toList());

        // uttaksplaner link inkl
        var link = BehandlingDtoUtil.buildLink(UttakRestTjeneste.UTTAKSPLANER, "uttak-uttaksplaner", HttpMethod.GET, ub -> {
            ub.addParameter(BehandlingUuidDto.NAME, behandlingUuid.toString());
            for (var s : andreSaker) {
                ub.addParameter("saksnummer", s.getVerdi());
            }
        });

        dto.leggTil(link);

        dto.leggTil(getFraMap(UttakRestTjeneste.UTTAK_FASTSATT, "uttak-fastsatt", behandlingUuidQueryParams));
        dto.leggTil(getFraMap(UttakRestTjeneste.UTTAK_OPPGITT, "uttak-oppgitt", behandlingUuidQueryParams));
    }

    private Optional<ResourceLink> lagOriginalBehandlingLink(Behandling behandling) {
        if (behandling.getOriginalBehandling().isPresent()) {
            var originalBehandling = behandling.getOriginalBehandling().get();
            var originalQueryParams = Map.of(BehandlingUuidDto.NAME, originalBehandling.getUuid().toString());
            return Optional.of(getFraMap(BehandlingRestTjeneste.REVURDERING_ORGINAL_PATH, "original-behandling", originalQueryParams));
        } else {
            return Optional.empty();
        }
    }

    private Optional<ResourceLink> lagVarselOmRevurderingLink(Behandling behandling) {
        if (BehandlingType.REVURDERING.equals(behandling.getType()) && BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
            var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
            return Optional.of(getFraMap(BrevRestTjeneste.VARSEL_REVURDERING_PATH, "sendt-varsel-om-revurdering", queryParams));
        } else {
            return Optional.empty();
        }
    }

    private Optional<ResourceLink> lagBeregningsgrunnlagLink(Behandling behandling) {
        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        return Optional.of(getFraMap(BeregningsgrunnlagRestTjeneste.PATH, "beregningsgrunnlag", queryParams));
    }

    private boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatIkkeEndret();
    }

    private Optional<SkjæringstidspunktDto> finnSkjæringstidspunktForBehandling(BehandlingReferanse ref) {
        if (!ref.getBehandlingType().erYtelseBehandlingType()) {
            return Optional.empty();
        }
        var skjæringstidspunktHvisUtledet = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet();
        if (skjæringstidspunktHvisUtledet.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SkjæringstidspunktDto(skjæringstidspunktHvisUtledet.get()));
    }

    private Skjæringstidspunkt finnSkjæringstidspunkt(Behandling behandling) {
        return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
    }

    private Optional<ResourceLink> lagSimuleringResultatLink(Behandling behandling) {
        return Optional.of(ResourceLink.post(k9OppdragProxyUrl + "/simulering/detaljert-resultat", "simuleringResultat", behandling.getUuid()));
    }

    private List<ResourceLink> lagTilbakekrevingValgLink(Behandling behandling) {
        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());

        List<ResourceLink> links = new ArrayList<>();
        if (tilbakekrevingRepository.hent(behandling.getId()).isPresent()) {
            links.add(getFraMap(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekrevingvalg", queryParams));
            links.add(getFraMap(TilbakekrevingRestTjeneste.VARSELTEKST_PATH, "tilbakekrevingsvarsel-fritekst", queryParams));
        }

        return links;
    }

    public Boolean finnBehandlingOperasjonRettigheter(Behandling behandling) {
        return this.søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).isPresent();
    }
}
