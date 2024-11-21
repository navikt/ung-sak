package no.nav.ung.sak.domene.abakus;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest.GrunnlagVersjon;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagSakSnapshotDto;
import no.nav.abakus.iaygrunnlag.v1.OverstyrtInntektArbeidYtelseDto;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.abakus.async.AsyncInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.abakus.mapping.IAYFraDtoMapper;
import no.nav.ung.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.*;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Dependent
@Default
public class AbakusInntektArbeidYtelseTjeneste implements InntektArbeidYtelseTjeneste {

    private static final Logger log = LoggerFactory.getLogger(AbakusInntektArbeidYtelseTjeneste.class);
    private AbakusTjeneste abakusTjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
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
                                             MottatteDokumentRepository mottatteDokumentRepository,
                                             FagsakRepository fagsakRepository,
                                             IAYRequestCache requestCache) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.abakusTjeneste = Objects.requireNonNull(abakusTjeneste, "abakusTjeneste");
        this.mottatteDokumentRepository = mottatteDokumentRepository;
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
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(Long behandlingId) {
        var iayGrunnlag = finnGrunnlag(behandlingId);
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(UUID behandlingUuid, UUID angittReferanse, LocalDateTime angittOpprettetTidspunkt) {
        var iayGrunnlag = Optional.ofNullable(hentGrunnlag(behandlingUuid));
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, angittReferanse, angittOpprettetTidspunkt, iayGrunnlag);
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

    // FIXME: bør kalle asyncabakustjeneste internt og overføre abakus i egen task
    @Override
    public void lagreArbeidsforhold(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder informasjonBuilder) {
        Objects.requireNonNull(informasjonBuilder, "informasjonBuilder"); // NOSONAR

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var eksisterendeGrunnlag = Optional.ofNullable(hentGrunnlagHvisEksisterer(behandling));
        konverterOgLagre(behandlingId, eksisterendeGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getSaksbehandletVersjon).orElse(null), informasjonBuilder.build());
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

    private InntektArbeidYtelseGrunnlagRequest initSnapshotRequest(Fagsak fagsak, GrunnlagVersjon siste) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(fagsak.getAktørId().getId()));
        request.medSaksnummer(fagsak.getSaksnummer().getVerdi());
        request.medYtelseType(YtelseType.fraKode(fagsak.getYtelseType().getKode()));
        request.medDataset(Arrays.asList(Dataset.values()));
        request.hentGrunnlagVersjon(siste);
        return request;
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

    private void konverterOgLagre(Long behandlingId, InntektArbeidYtelseAggregat overstyrtArbeid, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        lagreOverstyrt(overstyrtArbeid, arbeidsforholdInformasjon, behandlingId);
    }

    private UUID lagreOverstyrt(InntektArbeidYtelseAggregat overstyrtArbeid, ArbeidsforholdInformasjon arbeidsforholdInformasjon, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagreOverstyrt(konverterTilDto(behandling, overstyrtArbeid, arbeidsforholdInformasjon));
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

    private UUID lagreOverstyrt(OverstyrtInntektArbeidYtelseDto dto) {
        try {
            abakusTjeneste.lagreOverstyrt(dto);
            return dto.getGrunnlagReferanse();
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke lagre overstyrt arbeid i Abakus: " + e.getMessage(), e).toException();
        }
    }
}
