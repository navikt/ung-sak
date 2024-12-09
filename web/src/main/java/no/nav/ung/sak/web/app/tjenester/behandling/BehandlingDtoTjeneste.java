package no.nav.ung.sak.web.app.tjenester.behandling;

import static no.nav.ung.sak.web.app.tjenester.behandling.BehandlingDtoUtil.get;
import static no.nav.ung.sak.web.app.tjenester.behandling.BehandlingDtoUtil.getFraMap;
import static no.nav.ung.sak.web.app.tjenester.behandling.BehandlingDtoUtil.post;
import static no.nav.ung.sak.web.app.tjenester.behandling.BehandlingDtoUtil.setStandardfelter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.ung.sak.kontrakt.AsyncPollingStatus;
import no.nav.ung.sak.kontrakt.ResourceLink;
import no.nav.ung.sak.kontrakt.behandling.BehandlingDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingOperasjonerDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingsresultatDto;
import no.nav.ung.sak.kontrakt.behandling.ByttBehandlendeEnhetDto;
import no.nav.ung.sak.kontrakt.behandling.GjenopptaBehandlingDto;
import no.nav.ung.sak.kontrakt.behandling.HenleggBehandlingDto;
import no.nav.ung.sak.kontrakt.behandling.ReåpneBehandlingDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.behandling.SettBehandlingPaVentDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårResultatDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.web.app.proxy.oppdrag.OppdragProxyRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.arbeidsforhold.ArbeidsgiverRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.kravperioder.PerioderTilBehandlingMedKildeRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.los.LosRestTjeneste;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sikkerhet.context.SubjectHandler;

/**
 * Bygger et sammensatt barnetilleggTidslinje av BehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */
@Dependent
public class BehandlingDtoTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final BehandlingVedtakRepository behandlingVedtakRepository;
    private final SøknadRepository søknadRepository;
    private final TilbakekrevingRepository tilbakekrevingRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final TotrinnTjeneste totrinnTjeneste;

    private final Instance<InformasjonselementerUtleder> informasjonselementer;

    @Inject
    public BehandlingDtoTjeneste(BehandlingRepository behandlingRepository,
                                 BehandlingVedtakRepository behandlingVedtakRepository,
                                 SøknadRepository søknadRepository,
                                 TilbakekrevingRepository tilbakekrevingRepository,
                                 VilkårResultatRepository vilkårResultatRepository,
                                 TotrinnTjeneste totrinnTjeneste,
                                 @Any Instance<InformasjonselementerUtleder> informasjonselementer) {

        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadRepository = søknadRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.totrinnTjeneste = totrinnTjeneste;
        this.informasjonselementer = informasjonselementer;
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
            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.HAR_SAMME_RESULTAT_PATH, "har-samme-barnetilleggTidslinje", uuidQueryParams));
        }
        dto.leggTil(getFraMap(PerioderTilBehandlingMedKildeRestTjeneste.BEHANDLING_PERIODER, "behandling-perioder-årsak", uuidQueryParams));
        dto.leggTil(getFraMap(PerioderTilBehandlingMedKildeRestTjeneste.BEHANDLING_PERIODER_MED_VILKÅR, "behandling-perioder-årsak-med-vilkår", uuidQueryParams));
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
            // ingenting spesielt p.t.
        } else if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH, "har-apent-kontroller-revurdering-aksjonspunkt", uuidQueryParams));
        }

        // Totrinnsbehandlin
        dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_PATH, "totrinnskontroll-arsaker", uuidQueryParams));
        dto.leggTil(getFraMap(TotrinnskontrollRestTjeneste.ARSAKER_READ_ONLY_PATH, "totrinnskontroll-arsaker-readOnly", uuidQueryParams));
        if (BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
            dto.leggTil(post(TotrinnskontrollRestTjeneste.BEKREFT_AKSJONSPUNKT_PATH, "bekreft-totrinnsaksjonspunkt", new BehandlingIdDto(behandling.getUuid())));
        }

    }


    public List<BehandlingDto> lagBehandlingDtoer(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return Collections.emptyList();
        }
        Optional<BehandlingVedtak> gjeldendeVedtak = behandlingVedtakRepository.hentGjeldendeVedtak(behandlinger.get(0).getFagsak());
        Optional<Long> behandlingMedGjeldendeVedtak = gjeldendeVedtak.map(BehandlingVedtak::getBehandlingId);
        return behandlinger.stream().map(behandling -> {
            boolean erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(behandling, behandlingMedGjeldendeVedtak);
            var behandlingsresultat = lagBehandlingsresultat(behandling);
            return lagBehandlingDto(behandling, behandlingsresultat, erBehandlingMedGjeldendeVedtak);
        }).collect(Collectors.toList());
    }

    BehandlingsresultatDto lagBehandlingsresultat(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        return lagBehandlingsresultat(ref);
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

        dto.leggTil(get(FagsakRestTjeneste.PATH, "fagsak", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));
        dto.leggTil(getFraMap(FagsakRestTjeneste.RELATERTE_SAKER_PATH, "fagsak-relaterte-saker", uuidQueryParams));

        dto.leggTil(get(HistorikkRestTjeneste.PATH, "historikk", new SaksnummerDto(behandling.getFagsak().getSaksnummer())));

        dto.leggTil(getFraMap(AksjonspunktRestTjeneste.AKSJONSPUNKT_V2_PATH, "aksjonspunkter", uuidQueryParams));
        dto.leggTil(getFraMap(VilkårRestTjeneste.V3_PATH, "vilkar-v3", uuidQueryParams));

        dto.leggTil(getFraMap(SøknadRestTjeneste.SOKNAD_PATH, "soknad", uuidQueryParams));

        dto.leggTil(getFraMap(SøknadsfristRestTjeneste.SØKNADSFRIST_STATUS_PATH, "soknadsfrist-status", uuidQueryParams));

        dto.leggTil(getFraMap(PersonRestTjeneste.PERSONOPPLYSNINGER_PATH, "soeker-personopplysninger", uuidQueryParams));

        leggTilBeregnetYtelseBaserteLinks(behandling, dto, uuidQueryParams);

        lagLosLink(behandling).forEach(dto::leggTil);
    }

    private void leggTilBeregnetYtelseBaserteLinks(Behandling behandling, BehandlingDto dto, Map<String, String> uuidQueryParams) {
        boolean ytelseMedBeregning = InformasjonselementerUtleder.finnTjeneste(informasjonselementer, behandling.getFagsakYtelseType(), behandling.getType()).harBeregnetYtelse(behandling.getType());

        if (ytelseMedBeregning) {
            // FIXME: fjern denne, skal bare ha "arbeidsforhold" som rel
            dto.leggTil(getFraMap(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_ARBEIDSFORHOLD_PATH, "arbeidsforhold-v1", uuidQueryParams));
            dto.leggTil(getFraMap(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_ARBEIDSFORHOLD_PATH, "arbeidsforhold", uuidQueryParams));

            dto.leggTil(getFraMap(ArbeidsgiverRestTjeneste.ARBEIDSGIVER_PATH, "arbeidsgivere", uuidQueryParams));

            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.BEREGNINGSRESULTAT_PATH, "beregningsresultat", uuidQueryParams));
            dto.leggTil(getFraMap(BeregningsresultatRestTjeneste.BEREGNINGSRESULTAT_UTBETALT_PATH, "beregningsresultat-utbetalt", uuidQueryParams));
            lagSimuleringResultatLink(behandling).ifPresent(dto::leggTil);
            lagTilbakekrevingValgLink(behandling).forEach(dto::leggTil);
        }
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

    private boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatIkkeEndret();
    }

    private Optional<ResourceLink> lagSimuleringResultatLink(Behandling behandling) {
        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());
        return Optional.of(getFraMap(OppdragProxyRestTjeneste.SIMULERING_RESULTAT_URL, "simuleringResultat", queryParams));
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

    private List<ResourceLink> lagLosLink(Behandling behandling) {
        var queryParams = Map.of(BehandlingUuidDto.NAME, behandling.getUuid().toString());

        List<ResourceLink> links = new ArrayList<>();
        links.add(getFraMap(LosRestTjeneste.MERKNAD_PATH, "los-hente-merknad", queryParams));
        links.add(post(LosRestTjeneste.MERKNAD_PATH, "los-lagre-merknad", new BehandlingUuidDto(behandling.getUuid())));
        return links;
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
