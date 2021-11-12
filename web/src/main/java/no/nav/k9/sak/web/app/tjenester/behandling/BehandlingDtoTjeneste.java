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
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingOperasjonerDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingsresultatDto;
import no.nav.k9.sak.kontrakt.behandling.ByttBehandlendeEnhetDto;
import no.nav.k9.sak.kontrakt.behandling.GjenopptaBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.HenleggBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.ReåpneBehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.behandling.SettBehandlingPaVentDto;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingEndringDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingOpprettelseDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårResultatDto;
import no.nav.k9.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold.ArbeidsgiverRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.død.RettVedDødRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.kompletthet.KompletthetForBeregningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorg.OmsorgenForRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger.RammevedtakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger.ÅrskvantumRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PleietrengendeRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomVurderingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument.SykdomDokumentRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.tilsyn.VurderTilsynRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.PleiepengerUttakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.DokumenterMedUstrukturerteDataRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sikkerhet.context.SubjectHandler;

/**
 * Bygger et sammensatt resultat av BehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */
@Dependent
public class BehandlingDtoTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SøknadRepository søknadRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttakRepository uttakRepository;
    private TotrinnTjeneste totrinnTjeneste;

    /**
     * denne kan overstyres for testing lokalt
     */
    private String k9OppdragProxyUrl;
    private Instance<InformasjonselementerUtleder> informasjonselementer;

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
                                 VilkårResultatRepository vilkårResultatRepository,
                                 TotrinnTjeneste totrinnTjeneste,
                                 @Any Instance<InformasjonselementerUtleder> informasjonselementer,
                                 @KonfigVerdi(value = "k9.oppdrag.proxy.url") String k9OppdragProxyUrl) {

        this.fagsakRepository = fagsakRepository;
        this.uttakRepository = uttakRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadRepository = søknadRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.totrinnTjeneste = totrinnTjeneste;
        this.informasjonselementer = informasjonselementer;
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

        leggTilRettigheterLinks(dto);
        leggTilGrunnlagResourceLinks(behandling, dto);
        leggTilStatusResultaterLinks(behandling, dto);
        leggTilHandlingerResourceLinks(behandling, dto);
    }

    private void leggTilRettigheterLinks(BehandlingDto dto) {
        var uuidQueryParams = Map.of(BehandlingUuidDto.NAME, dto.getUuid().toString());
        dto.leggTil(getFraMap(BehandlingRestTjeneste.RETTIGHETER_PATH, "behandling-rettigheter", uuidQueryParams));
    }

    private void leggTilStatusResultaterLinks(Behandling behandling, BehandlingDto dto) {
        var idQueryParams = Map.of(BehandlingIdDto.NAME, behandling.getUuid().toString()); // legacy param name
        var uuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(getFraMap(KontrollRestTjeneste.KONTROLLRESULTAT_V2_PATH, "kontrollresultat", idQueryParams));
        } else if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.HAR_SAMME_RESULTAT_PATH, "har-samme-resultat", uuidQueryParams));
        }

    }

    private void leggTilHandlingerResourceLinks(Behandling behandling, BehandlingDto dto) {

        if (behandling.getStatus().erFerdigbehandletStatus()) {
            // Tillatt å bestille brev på lukket behandling, skip resten for ferdigbehandlet behandling
            dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill", new BestillBrevDto()));
            return;
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
        dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_PATH, "totrinnskontroll-arsaker", uuidQueryParams));
        dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_READ_ONLY_PATH, "totrinnskontroll-arsaker-readOnly", uuidQueryParams));
        if (BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
            dto.leggTil(post(TotrinnskontrollRestTjeneste.BEKREFT_AKSJONSPUNKT_PATH, "bekreft-totrinnsaksjonspunkt", new BehandlingIdDto(behandling.getUuid())));
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
        var ref = BehandlingReferanse.fra(behandling);
        var behandlingsresultat = lagBehandlingsresultat(ref);
        return behandlingsresultat;
    }

    private BehandlingsresultatDto lagBehandlingsresultat(BehandlingReferanse ref) {
        var dto = new BehandlingsresultatDto();

        Long behandlingId = ref.getBehandlingId();

        dto.setResultatType(ref.getBehandlingResultat());

        var vilkårResultater = vilkårResultatRepository.hentVilkårResultater(behandlingId);
        var vilkårResultaterMap = vilkårResultater.stream()
            .map(vp -> new AbstractMap.SimpleEntry<>(vp.getVilkårType(), new VilkårResultatDto(vp.getPeriode(), vp.getAvslagsårsak(), vp.getUtfall())))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
        dto.setVilkårResultat(vilkårResultaterMap);

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

    public BehandlingDto lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(Behandling revurdering) {
        var dto = new BehandlingDto();

        Optional<Long> originalBehandling = revurdering.getOriginalBehandlingId();

        var erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(revurdering, originalBehandling);
        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(revurdering.getId()).orElse(null);
        setStandardfelter(revurdering, dto, behandlingVedtak, erBehandlingMedGjeldendeVedtak);

        var behandlingsresultatDto = lagBehandlingsresultat(revurdering);
        initBehandlingResourceLinks(revurdering, behandlingsresultatDto, dto);

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
        lagOriginalBehandlingLink(behandling).ifPresent(dto::leggTil);

        leggTilYtelsespesifikkResourceLinks(behandling, dto);

        dto.leggTil(get(FagsakRestTjeneste.PATH, "fagsak", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));
        dto.leggTil(getFraMap(FagsakRestTjeneste.RELATERTE_SAKER_PATH, "fagsak-relaterte-saker", uuidQueryParams));

        dto.leggTil(get(HistorikkRestTjeneste.PATH, "historikk", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));

        dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_V2_PATH, "aksjonspunkter", uuidQueryParams));
        dto.leggTil(getFraMap(VilkårRestTjeneste.V3_PATH, "vilkar-v3", uuidQueryParams));

        dto.leggTil(getFraMap(SøknadRestTjeneste.SOKNAD_PATH, "soknad", uuidQueryParams));

        dto.leggTil(getFraMap(SøknadsfristRestTjeneste.SØKNADSFRIST_STATUS_PATH, "soknadsfrist-status", uuidQueryParams));

        dto.leggTil(getFraMap(PersonRestTjeneste.PERSONOPPLYSNINGER_PATH, "soeker-personopplysninger", uuidQueryParams));

        leggTilBeregnetYtelseBaserteLinks(behandling, dto, uuidQueryParams);

        dto.leggTil(getFraMap(BrevRestTjeneste.HENT_VEDTAKVARSEL_PATH, "vedtak-varsel", uuidQueryParams));
        lagFormidlingLink(behandling).forEach(dto::leggTil);
    }

    private void leggTilBeregnetYtelseBaserteLinks(Behandling behandling, BehandlingDto dto, Map<String, String> uuidQueryParams) {
        boolean ytelseMedBeregning = InformasjonselementerUtleder.finnTjeneste(informasjonselementer, behandling.getFagsakYtelseType(), behandling.getType()).harBeregnetYtelse(behandling.getType());

        if (ytelseMedBeregning) {
            // FIXME: fjern denne, skal bare ha "arbeidsforhold" som rel
            dto.leggTil(getFraMap(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_ARBEIDSFORHOLD_PATH, "arbeidsforhold-v1", uuidQueryParams));
            dto.leggTil(getFraMap(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_ARBEIDSFORHOLD_PATH, "arbeidsforhold", uuidQueryParams));

            dto.leggTil(getFraMap(ArbeidsgiverRestTjeneste.ARBEIDSGIVER_PATH, "arbeidsgivere", uuidQueryParams));


            dto.leggTil(getFraMap(OpptjeningRestTjeneste.PATH_V2, "opptjening-v2", uuidQueryParams));
            dto.leggTil(getFraMap(OpptjeningRestTjeneste.INNTEKT_PATH, "inntekt", uuidQueryParams));

            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.BEREGNINGSRESULTAT_PATH, "beregningsresultat", uuidQueryParams));
            dto.leggTil(getFraMap(KompletthetForBeregningRestTjeneste.KOMPLETTHET_FOR_BEREGNING_PATH, "kompletthet-beregning", uuidQueryParams));
            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.BEREGNINGSRESULTAT_UTBETALT_PATH, "beregningsresultat-utbetalt", uuidQueryParams));
            lagBeregningsgrunnlagLink(behandling).ifPresent(dto::leggTil);
            lagBeregningsgrunnlagAlleLink(behandling).ifPresent(dto::leggTil);
            lagSimuleringResultatLink(behandling).ifPresent(dto::leggTil);
            lagTilbakekrevingValgLink(behandling).forEach(dto::leggTil);
        }
    }

    private void leggTilYtelsespesifikkResourceLinks(Behandling behandling, BehandlingDto dto) {
        var uuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        var saksnummerAndUuidQueryParam = Map.of("saksnummer", behandling.getFagsak().getSaksnummer().toString(),
            BehandlingUuidDto.NAME, behandling.getUuid().toString());

        var ytelseType = behandling.getFagsakYtelseType();
        switch (ytelseType) {
            case FRISINN:
                dto.leggTil(getFraMap(UttakRestTjeneste.UTTAK_OPPGITT, "uttak-oppgitt", uuidQueryParams));
                dto.leggTil(getFraMap(UttakRestTjeneste.UTTAK_FASTSATT, "uttak-fastsatt", uuidQueryParams));
                dto.leggTil(getFraMap(InntektArbeidYtelseRestTjeneste.OPPGITT_OPPTJENING_PATH_V2, "oppgitt-opptjening-v2", uuidQueryParams));
                dto.leggTil(getFraMap(BeregningsgrunnlagRestTjeneste.PATH_KOBLINGER, "beregning-koblinger", uuidQueryParams));
                break;
            case OMSORGSPENGER:
                dto.leggTil(getFraMap(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "soeker-medlemskap-v2", uuidQueryParams));
                dto.leggTil(getFraMap(OmsorgenForRestTjeneste.OMSORGEN_FOR_OPPLYSNINGER_PATH, "omsorgen-for", uuidQueryParams));
                dto.leggTil(getFraMap(ÅrskvantumRestTjeneste.FORBRUKTEDAGER, "forbrukte-dager", uuidQueryParams));
                dto.leggTil(getFraMap(ÅrskvantumRestTjeneste.FULL_UTTAKSPLAN, "full-uttaksplan", saksnummerAndUuidQueryParam));
                dto.leggTil(getFraMap(BeregningsgrunnlagRestTjeneste.PATH_KOBLINGER, "beregning-koblinger", uuidQueryParams));
                break;
            case OMSORGSPENGER_KS:
            case OMSORGSPENGER_MA:
            case OMSORGSPENGER_AO:
                dto.leggTil(getFraMap(OmsorgenForRestTjeneste.OMSORGEN_FOR_OPPLYSNINGER_PATH, "omsorgen-for", uuidQueryParams));
                dto.leggTil(getFraMap(RammevedtakRestTjeneste.RAMMEVEDTAK, "rammevedtak", uuidQueryParams));
                break;
            case PLEIEPENGER_NÆRSTÅENDE:
            case OPPLÆRINGSPENGER:
            case PLEIEPENGER_SYKT_BARN:
                dto.leggTil(getFraMap(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "soeker-medlemskap-v2", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomRestTjeneste.SYKDOM_AKSJONSPUNKT_PATH, "sykdom-aksjonspunkt", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomVurderingRestTjeneste.VURDERING_OVERSIKT_KTP_PATH, "sykdom-vurdering-oversikt-ktp", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomVurderingRestTjeneste.VURDERING_OVERSIKT_TOO_PATH, "sykdom-vurdering-oversikt-too", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomVurderingRestTjeneste.VURDERING_PATH, "sykdom-vurdering-direkte", uuidQueryParams));
                dto.leggTil(post(SykdomVurderingRestTjeneste.VURDERING_PATH, "sykdom-vurdering-opprettelse", new SykdomVurderingOpprettelseDto(behandling.getUuid().toString())));
                dto.leggTil(post(SykdomVurderingRestTjeneste.VURDERING_PATH, "sykdom-vurdering-endring", new SykdomVurderingEndringDto(behandling.getUuid().toString())));
                dto.leggTil(getFraMap(SykdomDokumentRestTjeneste.DOKUMENT_OVERSIKT_PATH, "sykdom-dokument-oversikt", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomDokumentRestTjeneste.DOKUMENT_LISTE_PATH, "sykdom-dokument-liste", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomDokumentRestTjeneste.DOKUMENTER_SOM_IKKE_HAR_OPPDATERT_EKSISTERENDE_VURDERINGER_PATH, "sykdom-dokument-eksisterendevurderinger", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomDokumentRestTjeneste.SYKDOM_INNLEGGELSE_PATH, "sykdom-innleggelse", uuidQueryParams));
                dto.leggTil(getFraMap(SykdomDokumentRestTjeneste.SYKDOM_DIAGNOSEKODER_PATH, "sykdom-diagnosekoder", uuidQueryParams));
                dto.leggTil(getFraMap(PleiepengerUttakRestTjeneste.GET_UTTAKSPLAN_PATH, "pleiepenger-sykt-barn-uttaksplan", uuidQueryParams));
                dto.leggTil(getFraMap(PleiepengerUttakRestTjeneste.GET_SKULLE_SØKT_OM_PATH, "psb-manglende-arbeidstid", uuidQueryParams));
                dto.leggTil(getFraMap(OmsorgenForRestTjeneste.OMSORGEN_FOR_OPPLYSNINGER_PATH, "omsorgen-for", uuidQueryParams));
                dto.leggTil(getFraMap(BeregningsgrunnlagRestTjeneste.PATH_KOBLINGER, "beregning-koblinger", uuidQueryParams));
                dto.leggTil(getFraMap(VurderTilsynRestTjeneste.BASEPATH, "pleiepenger-sykt-barn-tilsyn", uuidQueryParams));
                dto.leggTil(getFraMap(RettVedDødRestTjeneste.BASEPATH, "rett-ved-dod", uuidQueryParams));
                dto.leggTil(getFraMap(PleietrengendeRestTjeneste.BASE_PATH, "om-pleietrengende", uuidQueryParams));
                dto.leggTil(getFraMap(DokumenterMedUstrukturerteDataRestTjeneste.FRITEKSTDOKUMENTER_PATH, "pleiepenger-fritekstdokumenter", uuidQueryParams));
                leggTilUttakEndepunkt(behandling, dto);
                break;
            default:
                throw new UnsupportedOperationException("Støtter ikke ytelse " + ytelseType);
        }

    }

    private void leggTilUttakEndepunkt(Behandling behandling, BehandlingDto dto) {
        final var behandlingUuidQueryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        dto.leggTil(getFraMap(UttakRestTjeneste.UTTAK_FASTSATT, "uttak-fastsatt", behandlingUuidQueryParams));
        dto.leggTil(getFraMap(UttakRestTjeneste.UTTAK_OPPGITT, "uttak-oppgitt", behandlingUuidQueryParams));
    }

    private Optional<ResourceLink> lagOriginalBehandlingLink(Behandling behandling) {
        if (behandling.getOriginalBehandlingId().isPresent()) {
            var originalBehandling = behandlingRepository.hentBehandling(behandling.getOriginalBehandlingId().get());
            var originalQueryParams = Map.of(BehandlingUuidDto.NAME, originalBehandling.getUuid().toString());
            return Optional.of(getFraMap(BehandlingRestTjeneste.REVURDERING_ORGINAL_PATH, "original-behandling", originalQueryParams));
        } else {
            return Optional.empty();
        }
    }

    private Optional<ResourceLink> lagBeregningsgrunnlagLink(Behandling behandling) {
        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        return Optional.of(getFraMap(BeregningsgrunnlagRestTjeneste.PATH, "beregningsgrunnlag", queryParams));
    }

    private Optional<ResourceLink> lagBeregningsgrunnlagAlleLink(Behandling behandling) {
        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        return Optional.of(getFraMap(BeregningsgrunnlagRestTjeneste.PATH_ALLE, "beregningsgrunnlag-alle", queryParams));
    }

    private boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatIkkeEndret();
    }

    private Optional<ResourceLink> lagSimuleringResultatLink(Behandling behandling) {
        return Optional.of(ResourceLink.post(k9OppdragProxyUrl + "/simulering/detaljert-resultat", "simuleringResultat", behandling.getUuid()));
    }

    private List<ResourceLink> lagFormidlingLink(Behandling behandling) {
        final var FORMIDLING_PATH = "/k9/formidling/api";
        final var FORMIDLING_DOKUMENTDATA_PATH = "/k9/formidling/dokumentdata/api";

        final var behandlingUuid = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        final var behandlingUuidOgYtelse = Map.of(
            BehandlingUuidDto.NAME, behandling.getUuid().toString(),
            "sakstype", behandling.getFagsakYtelseType().getKode());

        List<ResourceLink> links = new ArrayList<>();
        links.add(ResourceLink.get(FORMIDLING_PATH + "/brev/maler", "brev-maler", behandlingUuidOgYtelse));
        links.add(ResourceLink.get(FORMIDLING_PATH + "/brev/tilgjengeligevedtaksbrev", "tilgjengelige-vedtaksbrev", behandlingUuidOgYtelse));
        links.add(ResourceLink.get(FORMIDLING_PATH + "/brev/informasjonsbehov", "informasjonsbehov-vedtaksbrev", behandlingUuidOgYtelse));
        links.add(ResourceLink.get(FORMIDLING_DOKUMENTDATA_PATH, "dokumentdata-hente", behandlingUuid));
        links.add(ResourceLink.post(FORMIDLING_DOKUMENTDATA_PATH + "/" + behandling.getUuid(), "dokumentdata-lagre", null));
        return links;
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

    public BehandlingOperasjonerDto lovligeOperasjoner(Behandling b) {
        if (b.erSaksbehandlingAvsluttet()) {
            return BehandlingOperasjonerDto.builder(b.getUuid()).build(); // Skal ikke foreta menyvalg lenger
        } else if (BehandlingStatus.FATTER_VEDTAK.equals(b.getStatus())) {
            boolean tilgokjenning = b.getAnsvarligSaksbehandler() != null && !b.getAnsvarligSaksbehandler().equalsIgnoreCase(SubjectHandler.getSubjectHandler().getUid());
            return BehandlingOperasjonerDto.builder(b.getUuid()).medTilGodkjenning(tilgokjenning).build();
        } else {
            boolean kanÅpnesForEndring = b.erRevurdering() && !b.isBehandlingPåVent();
            boolean totrinnRetur = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(b).stream()
                .anyMatch(tt -> !tt.isGodkjent());
            return BehandlingOperasjonerDto.builder(b.getUuid())
                .medTilGodkjenning(false)
                .medFraBeslutter(!b.isBehandlingPåVent() && totrinnRetur)
                .medKanBytteEnhet(true)
                .medKanHenlegges(true)
                .medKanSettesPaVent(!b.isBehandlingPåVent())
                .medKanGjenopptas(b.isBehandlingPåVent())
                .medKanOpnesForEndringer(kanÅpnesForEndring)
                .medKanSendeMelding(!b.isBehandlingPåVent())
                .build();
        }
    }

}
