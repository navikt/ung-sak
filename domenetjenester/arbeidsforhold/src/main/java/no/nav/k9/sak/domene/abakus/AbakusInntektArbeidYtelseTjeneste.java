package no.nav.k9.sak.domene.abakus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest.GrunnlagVersjon;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerMottattRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerRequest;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagSakSnapshotDto;
import no.nav.abakus.iaygrunnlag.v1.OverstyrtInntektArbeidYtelseDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.abakus.async.AsyncInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.abakus.mapping.IAYFraDtoMapper;
import no.nav.k9.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.k9.sak.domene.abakus.mapping.MapInntektsmeldinger;
import no.nav.k9.sak.domene.arbeidsforhold.IAYDiffsjekker;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
@Default
public class AbakusInntektArbeidYtelseTjeneste implements InntektArbeidYtelseTjeneste {

    private static final Logger log = LoggerFactory.getLogger(AbakusInntektArbeidYtelseTjeneste.class);
    private AbakusTjeneste abakusTjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private IAYRequestCache requestCache;
    private AsyncInntektArbeidYtelseTjeneste asyncIayTjeneste;

    /**
     * CDI ctor for proxies.
     */
    AbakusInntektArbeidYtelseTjeneste() {
        // CDI proxy
    }

    /**
     * Standard ctor som injectes av CDI.
     */
    @Inject
    public AbakusInntektArbeidYtelseTjeneste(AbakusTjeneste abakusTjeneste,
                                             AsyncInntektArbeidYtelseTjeneste asyncIayTjeneste,
                                             BehandlingRepository behandlingRepository,
                                             FagsakRepository fagsakRepository,
                                             IAYRequestCache requestCache) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.abakusTjeneste = Objects.requireNonNull(abakusTjeneste, "abakusTjeneste");
        this.requestCache = Objects.requireNonNull(requestCache, "requestCache");
        this.fagsakRepository = Objects.requireNonNull(fagsakRepository, "fagsakRepository");
        this.asyncIayTjeneste = asyncIayTjeneste;
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlagHvisEksisterer(behandling);
        if (grunnlag == null) {
            throw new IllegalStateException("Fant ikke IAY grunnlag som forventet.");
        }
        return grunnlag;
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlagHvisEksisterer(Behandling behandling) {
        var request = initRequest(behandling);
        AktørId aktørId = behandling.getAktørId();
        return hentOgMapGrunnlag(request, aktørId);
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(UUID behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlagHvisEksisterer(behandling);
        if (grunnlag == null) {
            throw new IllegalStateException("Fant ikke IAY grunnlag som forventet.");
        }
        return grunnlag;
    }

    @Override
    public Optional<InntektArbeidYtelseGrunnlag> finnGrunnlag(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return Optional.ofNullable(this.hentGrunnlagHvisEksisterer(behandling));
    }

    /**
     * Anbefalt ikke bruk? Mulig behov for totrinnskontroll som lenker direkte til nøkkel
     */
    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlagForGrunnlagId(Long behandlingId, UUID inntektArbeidYtelseGrunnlagUuid) {
        var dto = requestCache.getGrunnlag(inntektArbeidYtelseGrunnlagUuid);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (dto == null) {
            var request = initRequest(behandling, inntektArbeidYtelseGrunnlagUuid);
            AktørId aktørId = behandling.getAktørId();
            var grunnlaget = hentOgMapGrunnlag(request, aktørId);
            if (grunnlaget == null || grunnlaget.getEksternReferanse() == null || !grunnlaget.getEksternReferanse().equals(inntektArbeidYtelseGrunnlagUuid)) {
                throw new IllegalStateException("Fant ikke grunnlag med referanse=" + inntektArbeidYtelseGrunnlagUuid);
            }
            return grunnlaget;
        }
        return dto;
    }

    private InntektArbeidYtelseGrunnlagRequest initRequest(Behandling behandling, UUID inntektArbeidYtelseGrunnlagUuid) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(behandling.getAktørId().getId()));
        request.medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        request.medYtelseType(YtelseType.fraKode(behandling.getFagsakYtelseType().getKode()));
        request.forKobling(behandling.getUuid());
        request.forGrunnlag(inntektArbeidYtelseGrunnlagUuid);
        request.medDataset(Arrays.asList(Dataset.values()));
        return request;
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(Long behandlingId) {
        var iayGrunnlag = finnGrunnlag(behandlingId);
        return opprettBuilderFor(VersjonType.REGISTER, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(UUID behandlingUuid, UUID angittReferanse, LocalDateTime angittOpprettetTidspunkt) {
        var iayGrunnlag = Optional.ofNullable(hentGrunnlag(behandlingUuid));
        return opprettBuilderFor(VersjonType.REGISTER, angittReferanse, angittOpprettetTidspunkt, iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(Long behandlingId) {
        var iayGrunnlag = finnGrunnlag(behandlingId);
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(UUID behandlingUuid, UUID angittReferanse, LocalDateTime angittOpprettetTidspunkt) {
        var iayGrunnlag = Optional.ofNullable(hentGrunnlag(behandlingUuid));
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, angittReferanse, angittOpprettetTidspunkt, iayGrunnlag);
    }

    @Override
    public Set<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer, AktørId aktørId, FagsakYtelseType ytelseType) {
        Optional<Fagsak> fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        if (fagsakOpt.isPresent()) {
            Fagsak fagsak = fagsakOpt.get();
            // Hent grunnlag fra abakus
            return hentOgMapAlleInntektsmeldinger(aktørId, fagsak.getSaksnummer(), ytelseType);

        }
        return Set.of();
    }

    @Override
    public Set<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer) {
        Optional<Fagsak> fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        if (fagsakOpt.isPresent()) {
            Fagsak fagsak = fagsakOpt.get();
            // Hent grunnlag fra abakus
            return hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), fagsak.getYtelseType());

        }
        return Set.of();
    }

    private SakInntektsmeldinger hentInntektsmeldinger(Saksnummer saksnummer) {
        SakInntektsmeldinger sakInntektsmeldinger = new SakInntektsmeldinger();
        Optional<Fagsak> fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        if (fagsakOpt.isPresent()) {
            Fagsak fagsak = fagsakOpt.get();

            // Hent grunnlag fra abakus
            List<InntektArbeidYtelseGrunnlag> fagsakIayGrunnlag = hentOgMapAlleGrunnlag(fagsak);

            fagsakIayGrunnlag.forEach(iayg -> {
                iayg.getInntektsmeldinger()
                    .stream()
                    .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
                    .flatMap(Collection::stream)
                    .forEach(im -> {
                        behandlingRepository.hentBehandlingHvisFinnes(iayg.getKoblingReferanse().orElseThrow())
                            .ifPresent(behandling -> {
                                sakInntektsmeldinger.leggTil(behandling.getId(), iayg.getEksternReferanse(), iayg.getOpprettetTidspunkt(), im);
                                sakInntektsmeldinger.leggTil(behandling.getId(), iayg.getEksternReferanse(), iayg.getOpprettetTidspunkt(), iayg);
                            });
                    });
            });
        }
        return sakInntektsmeldinger;
    }

    @Override
    public Set<Inntektsmelding> hentInntektsmeldingerSidenRef(Saksnummer saksnummer, Long behandlingId, UUID eksternReferanse) {
        var sakInntektsmeldinger = hentInntektsmeldinger(saksnummer); // TODO: Fjern denne, hent direkte fra abakus?
        return sakInntektsmeldinger.hentInntektsmeldingerSidenRef(behandlingId, eksternReferanse);
    }

    // FIXME: : bør håndteres i egen task (kall asyncabakustjeneste internt).
    @Override
    public void lagreIayAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
        if (!Objects.equals(builder.getVersjon(), VersjonType.SAKSBEHANDLET)) {
            throw new UnsupportedOperationException("Støtter kun lagre ned saksbehandlet versjon, fikk her :" + builder.getVersjon());
        }
        InntektArbeidYtelseGrunnlag dummy = getGrunnlagBuilder(behandlingId, builder).build();
        var saksbehandlet = dummy.getSaksbehandletVersjon().orElse(null);
        var arbeidsforholdInformasjon = dummy.getArbeidsforholdInformasjon().orElse(null);
        konverterOgLagre(behandlingId, saksbehandlet, arbeidsforholdInformasjon);
    }

    @Override
    /** @deprecated (brukes kun i test) Bruk AsyncAbakusLagreOpptjeningTask i modul mottak i stedet */
    public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        throw new UnsupportedOperationException("Ikke lenger i bruk, bruk heller AsyncAbakusLagreOpptjeningTask");
    }

    @Override
    public void lagreOverstyrtOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        if (oppgittOpptjeningBuilder == null) {
            return;
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var ytelseType = YtelseType.fraKode(behandling.getFagsakYtelseType().getKode());
        var oppgittOpptjening = new IAYTilDtoMapper(behandling.getAktørId(), null, behandling.getUuid()).mapTilDto(oppgittOpptjeningBuilder);
        var request = new OppgittOpptjeningMottattRequest(saksnummer.getVerdi(), behandling.getUuid(), aktør, ytelseType, oppgittOpptjening);

        try {
            abakusTjeneste.lagreOverstyrtOppgittOpptjening(request);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Lagre oppgitt opptjening i abakus: " + e.getMessage(), e).toException();
        }
    }

    // FIXME: bør kalle asyncabakustjeneste internt og overføre abakus i egen task
    @Override
    public void lagreArbeidsforhold(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder informasjonBuilder) {
        Objects.requireNonNull(informasjonBuilder, "informasjonBuilder"); // NOSONAR

        InntektArbeidYtelseGrunnlagBuilder builder = opprettGrunnlagBuilderFor(behandlingId);
        builder.medInformasjon(informasjonBuilder.build());
        konverterOgLagre(behandlingId, builder.build());
    }

    @Override
    public Optional<OppgittOpptjening> hentKunOverstyrtOppgittOpptjening(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var request = initRequest(behandling);
        AktørId aktørId = behandling.getAktørId();
        return Optional.ofNullable(hentOgMapGrunnlag(request, aktørId)).flatMap(InntektArbeidYtelseGrunnlag::getOverstyrtOppgittOpptjening);
    }

    // FIXME: I dag kalles denne fra LagreMottattInntektsmeldingerTask. Burde hatt task logikk i implementasjonen her i stedet for symmetri
    @Override
    public void lagreInntektsmeldinger(Saksnummer saksnummer, Long behandlingId, Collection<InntektsmeldingBuilder> builders) {
        Objects.requireNonNull(builders, "inntektsmelding");
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var inntektsmeldingerDto = new IAYTilDtoMapper(behandling.getAktørId(), null, behandling.getUuid()).mapTilDto(builders);

        if (inntektsmeldingerDto == null) {
            return;
        }
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());
        var ytelseType = YtelseType.fraKode(behandling.getFagsakYtelseType().getKode());
        var inntektsmeldingerMottattRequest = new InntektsmeldingerMottattRequest(saksnummer.getVerdi(), behandling.getUuid(), aktør, ytelseType, inntektsmeldingerDto);
        try {
            abakusTjeneste.lagreInntektsmeldinger(inntektsmeldingerMottattRequest);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Lagre mottatte inntektsmeldinger i abakus: " + e.getMessage(), e).toException();
        }
    }

    @Override
    public void fjernSaksbehandletVersjon(Long behandlingId) {
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = finnGrunnlag(behandlingId);
        if (iayGrunnlagOpt.isPresent()) {
            InntektArbeidYtelseGrunnlag iayGrunnlag = iayGrunnlagOpt.get();
            if (iayGrunnlag.getSaksbehandletVersjon().isPresent()) {
                konverterOgLagre(behandlingId, null, iayGrunnlag.getArbeidsforholdInformasjon().orElse(null));
            }
        }
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId, Set<Dataset> dataset) {
        asyncIayTjeneste.kopierIayGrunnlag(fraBehandlingId, tilBehandlingId, dataset);
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId) {
        kopierGrunnlagFraEksisterendeBehandling(fraBehandlingId, tilBehandlingId, null);
    }

    private InntektArbeidYtelseGrunnlag hentOgMapGrunnlag(InntektArbeidYtelseGrunnlagRequest request, AktørId aktørId) {
        var dto = hentGrunnlag(request);
        UUID forespurtGrunnlagsRef = request.getGrunnlagReferanse() != null ? request.getGrunnlagReferanse() : request.getSisteKjenteGrunnlagReferanse();
        var sisteGrunnlag = requestCache.getGrunnlag(forespurtGrunnlagsRef);
        if (dto == null && sisteGrunnlag == null) {
            return null;
        } else if (dto == null && sisteGrunnlag != null) {
            return sisteGrunnlag;
        }
        return mapOgCacheGrunnlag(dto, aktørId, request.getGrunnlagVersjon() == InntektArbeidYtelseGrunnlagRequest.GrunnlagVersjon.SISTE);
    }

    private InntektArbeidYtelseGrunnlag mapOgCacheGrunnlag(InntektArbeidYtelseGrunnlagDto grunnlagDto, AktørId aktørId, boolean isAktiv) {
        var grunnlag = mapResult(aktørId, grunnlagDto, isAktiv);
        requestCache.leggTil(grunnlag);
        return grunnlag;
    }

    private InntektsmeldingerDto hentUnikeInntektsmeldinger(InntektsmeldingerRequest request) {
        try {
            return abakusTjeneste.hentUnikeUnntektsmeldinger(request);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente inntektsmeldinger fra Abakus: " + e.getMessage(), e).toException();
        }
    }

    private InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) {
        try {
            return abakusTjeneste.hentGrunnlag(request);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente grunnlag fra Abakus: " + e.getMessage(), e).toException();
        }
    }

    private InntektArbeidYtelseGrunnlagSakSnapshotDto hentGrunnlagSnapshot(InntektArbeidYtelseGrunnlagRequest request) {
        try {
            return abakusTjeneste.hentGrunnlagSnapshot(request);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente grunnlag snapshot fra Abakus: " + e.getMessage(), e).toException();
        }
    }

    private InntektsmeldingAggregat mapResult(InntektsmeldingerDto dto) {
        var mapInntektsmeldinger = new MapInntektsmeldinger.MapFraDto();
        ArbeidsforholdInformasjonBuilder dummyBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        return mapInntektsmeldinger.map(dummyBuilder, dto);
    }

    private InntektArbeidYtelseGrunnlag mapResult(AktørId aktørId, InntektArbeidYtelseGrunnlagDto dto, boolean isAktiv) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = new IAYFraDtoMapper(aktørId).mapTilGrunnlagInklusivRegisterdata(dto, isAktiv);
        return new AbakusInntektArbeidYtelseGrunnlag(inntektArbeidYtelseGrunnlag, dto.getKoblingReferanse());
    }

    private InntektArbeidYtelseGrunnlagRequest initRequest(Behandling behandling) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(behandling.getAktørId().getId()));
        request.medSisteKjenteGrunnlagReferanse(requestCache.getSisteAktiveGrunnlagReferanse(behandling.getUuid()));
        request.medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        request.medYtelseType(YtelseType.fraKode(behandling.getFagsakYtelseType().getKode()));
        request.forKobling(behandling.getUuid());
        request.medDataset(Arrays.asList(Dataset.values()));
        return request;
    }

    private InntektsmeldingerRequest initInntektsmeldingerRequest(AktørId aktørId, Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        var request = new InntektsmeldingerRequest(new AktørIdPersonident(aktørId.getId()));
        request.setSaksnummer(saksnummer.getVerdi());
        request.setYtelseType(YtelseType.fraKode(ytelseType.getKode()));
        return request;
    }

    private InntektArbeidYtelseGrunnlagRequest initSnapshotRequest(Fagsak fagsak, GrunnlagVersjon siste) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(fagsak.getAktørId().getId()));
        request.medSaksnummer(fagsak.getSaksnummer().getVerdi());
        request.medYtelseType(YtelseType.fraKode(fagsak.getYtelseType().getKode()));
        request.medDataset(Arrays.asList(Dataset.values()));
        request.hentGrunnlagVersjon(siste);
        return request;
    }

    private LinkedHashSet<Inntektsmelding> hentOgMapAlleInntektsmeldinger(AktørId aktørId, Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        var request = initInntektsmeldingerRequest(aktørId, saksnummer, ytelseType);
        var dto = hentUnikeInntektsmeldinger(request);
        var inntektsmeldinger = mapResult(dto).getAlleInntektsmeldinger();

        return inntektsmeldinger.stream()
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<InntektArbeidYtelseGrunnlag> hentOgMapAlleGrunnlag(Fagsak fagsak) {
        var request = initSnapshotRequest(fagsak, GrunnlagVersjon.ALLE);
        var dto = hentGrunnlagSnapshot(request);
        return dto.getGrunnlag().stream().map(konvolutt -> mapOgCacheGrunnlag(konvolutt.getData(), fagsak.getAktørId(), false)).collect(Collectors.toList());
    }

    private InntektArbeidYtelseAggregatBuilder opprettBuilderFor(VersjonType versjonType, UUID angittReferanse, LocalDateTime opprettetTidspunkt,
                                                                 Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        InntektArbeidYtelseGrunnlagBuilder grunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(grunnlag);
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder");
        Optional<InntektArbeidYtelseGrunnlag> aggregat = Optional.ofNullable(grunnlagBuilder.getKladd());
        Objects.requireNonNull(aggregat, "aggregat");
        if (aggregat.isPresent()) {
            final InntektArbeidYtelseGrunnlag aggregat1 = aggregat.get();
            return InntektArbeidYtelseAggregatBuilder.builderFor(hentRiktigVersjon(versjonType, aggregat1), angittReferanse, opprettetTidspunkt, versjonType);
        }
        throw new IllegalArgumentException("aggregat kan ikke være null: " + angittReferanse);
    }

    private Optional<InntektArbeidYtelseAggregat> hentRiktigVersjon(VersjonType versjonType, InntektArbeidYtelseGrunnlag ytelseGrunnlag) {
        if (versjonType == VersjonType.REGISTER) {
            return ytelseGrunnlag.getRegisterVersjon();
        } else if (versjonType == VersjonType.SAKSBEHANDLET) {
            return ytelseGrunnlag.getSaksbehandletVersjon();
        }
        throw new IllegalStateException("Kunne ikke finne riktig versjon av InntektArbeidYtelseAggregat");
    }

    private InntektArbeidYtelseGrunnlagBuilder getGrunnlagBuilder(Long behandlingId, InntektArbeidYtelseAggregatBuilder iayAggregetBuilder) {
        Objects.requireNonNull(iayAggregetBuilder, "iayAggregetBuilder"); // NOSONAR
        InntektArbeidYtelseGrunnlagBuilder grunnlagBuilder = opprettGrunnlagBuilderFor(behandlingId);
        grunnlagBuilder.medData(iayAggregetBuilder);
        return grunnlagBuilder;
    }

    private InntektArbeidYtelseGrunnlagBuilder opprettGrunnlagBuilderFor(Long behandlingId) {
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidGrunnlag = finnGrunnlag(behandlingId);
        return InntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidGrunnlag);
    }

    private void konverterOgLagre(Long behandlingId, InntektArbeidYtelseGrunnlag nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        if (nyttGrunnlag == null) {
            return;
        }

        Optional<InntektArbeidYtelseGrunnlag> tidligereAggregat = finnGrunnlag(behandlingId);
        if (tidligereAggregat.isPresent()) {
            InntektArbeidYtelseGrunnlag tidligereGrunnlag = tidligereAggregat.get();
            if (differ().diff(tidligereGrunnlag, nyttGrunnlag).isEmpty()) {
                return;
            }
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        } else {
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        }
    }

    private void konverterOgLagre(Long behandlingId, InntektArbeidYtelseAggregat overstyrtArbeid, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        lagreOverstyrt(overstyrtArbeid, arbeidsforholdInformasjon, behandlingId);
    }

    private DiffEntity differ() {
        return new IAYDiffsjekker(false).getDiffEntity();
    }

    private UUID lagreOverstyrt(InntektArbeidYtelseAggregat overstyrtArbeid, ArbeidsforholdInformasjon arbeidsforholdInformasjon, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagreOverstyrt(konverterTilDto(behandling, overstyrtArbeid, arbeidsforholdInformasjon));
    }

    private void lagreGrunnlag(InntektArbeidYtelseGrunnlag nyttGrunnlag, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        lagreGrunnlag(konverterTilDto(behandling, nyttGrunnlag));
    }

    private InntektArbeidYtelseGrunnlagDto konverterTilDto(Behandling behandling, InntektArbeidYtelseGrunnlag gr) {
        InntektArbeidYtelseGrunnlagDto dto;
        try {
            var tilDto = new IAYTilDtoMapper(behandling.getAktørId(), gr.getEksternReferanse(), behandling.getUuid());
            dto = tilDto.mapTilDto(behandling.getFagsakYtelseType(), gr);
        } catch (RuntimeException t) {
            log.warn("Kunne ikke transformere til Dto: grunnlag=" + gr.getEksternReferanse() + ", behandling=" + behandling.getId(), t);
            throw t;
        }
        return dto;
    }

    private OverstyrtInntektArbeidYtelseDto konverterTilDto(Behandling behandling, InntektArbeidYtelseAggregat overstyrt, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        OverstyrtInntektArbeidYtelseDto dto;
        UUID nyGrunnlagReferanse = UUID.randomUUID();
        try {
            var tilDto = new IAYTilDtoMapper(behandling.getAktørId(), nyGrunnlagReferanse, behandling.getUuid());
            dto = tilDto.mapTilDto(behandling.getFagsakYtelseType(), overstyrt, arbeidsforholdInformasjon);
        } catch (RuntimeException t) {
            log.warn("Kunne ikke transformere til Dto: grunnlag=" + nyGrunnlagReferanse + ", behandling=" + behandling.getId(), t);
            throw t;
        }
        return dto;
    }

    private void lagreGrunnlag(InntektArbeidYtelseGrunnlagDto grunnlagDto) {
        try {
            abakusTjeneste.lagreGrunnlag(grunnlagDto);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke lagre grunnlag i Abakus: " + e.getMessage(), e).toException();
        }
    }

    private UUID lagreOverstyrt(OverstyrtInntektArbeidYtelseDto dto) {
        try {
            abakusTjeneste.lagreOverstyrt(dto);
            return dto.getGrunnlagReferanse();
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke lagre overstyrt arbeid i Abakus: " + e.getMessage(), e).toException();
        }
    }
}
