package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.FraKalkulusMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningAvklaringsbehovResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.MapEndringsresultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdReferanseDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.GrunnbeløpReguleringStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Vilkårsavslagsårsak;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoListeForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KontrollerGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.Grunnbeløp;
import no.nav.folketrygdloven.kalkulus.response.v1.GrunnbeløpReguleringRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandListeResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringListeRespons;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvklaringsbehovDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvslagsårsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningVenteårsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * KalkulusTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt
 * (https://github.com/navikt/ft-kalkulus/)
 */
@ApplicationScoped
@FagsakYtelseTypeRef
@Default
public class KalkulusTjeneste implements KalkulusApiTjeneste {

    private KalkulusRestKlient restTjeneste;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private KalkulatorInputTjeneste kalkulatorInputTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;
    private boolean togglePsbMigrering;

    public KalkulusTjeneste() {
    }

    @Inject
    public KalkulusTjeneste(KalkulusRestKlient restTjeneste,
                            FagsakRepository fagsakRepository,
                            VilkårResultatRepository vilkårResultatRepository,
                            @FagsakYtelseTypeRef("*") KalkulatorInputTjeneste kalkulatorInputTjeneste,
                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                            @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper,
                            @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering
    ) {
        this.restTjeneste = restTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.kalkulatorInputTjeneste = kalkulatorInputTjeneste;
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
        this.togglePsbMigrering = toggleMigrering;
    }

    @Override
    public SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, List<StartBeregningInput> startBeregningInput) {
        if (startBeregningInput.isEmpty()) {
            return new SamletKalkulusResultat(Collections.emptyMap(), Collections.emptyMap());
        }
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        Set<Inntektsmelding> sakInntektsmeldinger = finnInntektsmeldingerForSak(referanse, startBeregningInput);
        var startBeregningRequest = initStartRequest(
            referanse,
            iayGrunnlag,
            sakInntektsmeldinger,
            startBeregningInput);
        List<TilstandResponse> tilstandResponse = restTjeneste.startBeregning(startBeregningRequest);

        var bgReferanser = startBeregningInput.stream().map(i -> new BgRef(i.getBgReferanse(), i.getSkjæringstidspunkt())).collect(Collectors.toList());
        return mapFraTilstand(tilstandResponse, bgReferanser);
    }

    @Override
    public SamletKalkulusResultat fortsettBeregning(BehandlingReferanse referanse,
                                                    Collection<BgRef> bgReferanser,
                                                    BehandlingStegType stegType) {
        if (bgReferanser.isEmpty()) {
            return new SamletKalkulusResultat(Collections.emptyMap(), Collections.emptyMap());
        }
        var bgRefs = BgRef.getRefs(bgReferanser);
        var ytelseType = YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode());
        var request = new FortsettBeregningListeRequest(
            referanse.getSaksnummer().getVerdi(),
            bgRefs,
            ytelseType,
            new StegType(stegType.getKode()));
        TilstandListeResponse tilstandResponse = restTjeneste.fortsettBeregning(request);
        if (tilstandResponse.trengerNyInput()) {
            tilstandResponse = fortsettMedOppdatertInput(referanse, bgReferanser, stegType, ytelseType);
        }
        return mapFraTilstand(tilstandResponse.getTilstand(), bgReferanser);

    }

    /**
     * Kalkulus lagrer input, men kan kreve oppdatert input i enkelte situasjoner der kontrakten har endret seg.
     * I slike tilfeller må k9-sak kalle kalkulus med oppdatert input pr referanse
     * <p>
     * Metoden bygger kalkulatorinput med den gjeldende kontrakten og kaller fortsett hos kalkulus.
     *
     * @param referanse    Behandlingreferanse
     * @param bgReferanser referanser til bg
     * @param stegType     stegtype
     * @param ytelseType   Ytelsetype
     * @return Respons fra kalkulus
     */
    private TilstandListeResponse fortsettMedOppdatertInput(BehandlingReferanse referanse,
                                                            Collection<BgRef> bgReferanser,
                                                            BehandlingStegType stegType,
                                                            YtelseTyperKalkulusStøtterKontrakt ytelseType) {
        Map<UUID, KalkulatorInputDto> input = lagInputMap(bgReferanser, referanse);
        List<UUID> referanser = BgRef.getRefs(bgReferanser);
        var request = new FortsettBeregningListeRequest(
            referanse.getSaksnummer().getVerdi(),
            referanser,
            input,
            ytelseType,
            new StegType(stegType.getKode()));
        return restTjeneste.fortsettBeregning(request);
    }

    @Override
    public List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(BehandlingReferanse behandlingReferanse,
                                                                           Collection<BgRef> bgReferanser,
                                                                           Map<UUID, HåndterBeregningDto> håndterberegningMap) {
        List<HåndterBeregningRequest> requestListe = håndterberegningMap.entrySet().stream().map(e -> new HåndterBeregningRequest(e.getValue(), e.getKey())).collect(Collectors.toList());
        var oppdateringRespons = restTjeneste.oppdaterBeregningListe(new HåndterBeregningListeRequest(requestListe,
            null, // Sender null fordi inputen ligger lagret i kalkulus, settes ulik null når kalkulus svarer med trengerNyInput = true
            YtelseTyperKalkulusStøtterKontrakt.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
            behandlingReferanse.getSaksnummer().getVerdi(),
            behandlingReferanse.getBehandlingUuid()));
        if (oppdateringRespons.trengerNyInput()) {
            oppdateringRespons = oppdaterMedOppdatertInput(requestListe, bgReferanser, behandlingReferanse);
        }
        return oppdateringRespons.getOppdateringer().stream()
            .map(oppdatering -> MapEndringsresultat.mapFraOppdateringRespons(oppdatering.getOppdatering(), oppdatering.getEksternReferanse()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Kalkulus lagrer input, men kan kreve oppdatert input i enkelte situasjoner der kontrakten har endret seg.
     * I slike tilfeller må k9-sak kalle kalkulus med oppdatert input pr referanse
     * <p>
     * Metoden bygger kalkulatorinput med den gjeldende kontrakten og kaller oppdater hos kalkulus.
     *
     * @param requestListe requester for oppdatering
     * @param bgReferanser referanser til bg
     * @param referanse    Behandlingreferanse
     * @return Respons fra kalkulus
     */
    private OppdateringListeRespons oppdaterMedOppdatertInput(List<HåndterBeregningRequest> requestListe,
                                                              Collection<BgRef> bgReferanser,
                                                              BehandlingReferanse referanse) {
        Map<UUID, KalkulatorInputDto> input = lagInputMap(bgReferanser, referanse);
        var request = new HåndterBeregningListeRequest(requestListe,
            input,
            YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode()),
            referanse.getSaksnummer().getVerdi(),
            referanse.getBehandlingUuid());
        return restTjeneste.oppdaterBeregningListe(request);
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, Collection<BgRef> bgReferanser) {
        List<BeregningsgrunnlagGrunnlag> grunnlag = hentGrunnlag(ref, bgReferanser);
        if (grunnlag.isEmpty()) {
            return Collections.emptyList();
        }
        return grunnlag
            .stream()
            .filter(v -> Objects.equals(BeregningsgrunnlagTilstand.FASTSATT, v.getBeregningsgrunnlagTilstand()))
            .map(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
            .collect(Collectors.toList());
    }

    @Override
    public BeregningsgrunnlagListe hentBeregningsgrunnlagListeDto(BehandlingReferanse referanse, Set<BeregningsgrunnlagReferanse> bgReferanser) {
        HentBeregningsgrunnlagDtoListeForGUIRequest request = lagHentBeregningsgrunnlagListeRequest(referanse, bgReferanser);
        return restTjeneste.hentBeregningsgrunnlagDto(request);
    }

    private HentBeregningsgrunnlagDtoListeForGUIRequest lagHentBeregningsgrunnlagListeRequest(BehandlingReferanse referanse, Set<BeregningsgrunnlagReferanse> bgReferanser) {
        YtelseTyperKalkulusStøtterKontrakt ytelseSomSkalBeregnes = YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode());
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        Set<ArbeidsforholdReferanseDto> referanser = inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon()
            .stream()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .flatMap(Collection::stream)
            .map(ref -> new ArbeidsforholdReferanseDto(TilKalkulusMapper.mapTilAktør(ref.getArbeidsgiver()),
                new InternArbeidsforholdRefDto(ref.getInternReferanse().getReferanse()),
                new EksternArbeidsforholdRef(ref.getEksternReferanse().getReferanse())))
            .collect(Collectors.toSet());

        var vilkår = vilkårResultatRepository.hent(referanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var requestListe = bgReferanser.stream().map(it -> new HentBeregningsgrunnlagDtoForGUIRequest(
                it.getReferanse(),
                ytelseSomSkalBeregnes,
                referanser,
                vilkår.finnPeriodeForSkjæringstidspunkt(it.getSkjæringstidspunkt()).getPeriode().getFomDato()))
            .sorted(Comparator.comparing(HentBeregningsgrunnlagDtoForGUIRequest::getVilkårsperiodeFom))
            .collect(Collectors.toList());

        return new HentBeregningsgrunnlagDtoListeForGUIRequest(requestListe, referanse.getBehandlingUuid());
    }

    @Override
    public List<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, Collection<BgRef> bgReferanser) {
        if (bgReferanser.isEmpty()) {
            return List.of();
        }
        var ytelseSomSkalBeregnes = YtelseTyperKalkulusStøtterKontrakt.fraKode(ref.getFagsakYtelseType().getKode());

        var saksnummer = ref.getSaksnummer().getVerdi();
        List<BeregningsgrunnlagGrunnlag> resultater = new ArrayList<>();
        List<HentBeregningsgrunnlagRequest> requests = bgReferanser.stream()
            .map(bgRef -> new HentBeregningsgrunnlagRequest(bgRef.getRef(), saksnummer, ytelseSomSkalBeregnes, false)).collect(Collectors.toList());

        var dtoer = restTjeneste.hentBeregningsgrunnlagGrunnlag(new HentBeregningsgrunnlagListeRequest(requests, ref.getBehandlingUuid(), saksnummer, false));

        if (dtoer == null || dtoer.isEmpty()) {
            return Collections.emptyList();
        } else {
            dtoer.forEach(dto -> resultater.add(FraKalkulusMapper.mapBeregningsgrunnlagGrunnlag(dto)));
            return Collections.unmodifiableList(resultater);
        }
    }

    @Override
    public void lagreBeregningsgrunnlag(BehandlingReferanse behandlingReferanse, Beregningsgrunnlag beregningsgrunnlag, BeregningsgrunnlagTilstand opprettet) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public void deaktiverBeregningsgrunnlag(FagsakYtelseType fagsakYtelseType, Saksnummer saksnummer, List<UUID> bgReferanse) {
        var bgRequests = bgReferanse.stream().map(BeregningsgrunnlagRequest::new).collect(Collectors.toList());
        var request = new BeregningsgrunnlagListeRequest(saksnummer.getVerdi(), bgRequests);
        restTjeneste.deaktiverBeregningsgrunnlag(request);
    }

    @Override
    public no.nav.folketrygdloven.beregningsgrunnlag.modell.Grunnbeløp hentGrunnbeløp(LocalDate dato) {
        HentGrunnbeløpRequest request = new HentGrunnbeløpRequest(dato);
        Grunnbeløp grunnbeløp = restTjeneste.hentGrunnbeløp(request);
        return new no.nav.folketrygdloven.beregningsgrunnlag.modell.Grunnbeløp(
            grunnbeløp.getVerdi().longValue(),
            DatoIntervallEntitet.fraOgMedTilOgMed(grunnbeløp.getPeriode().getFom(), grunnbeløp.getPeriode().getTom()));
    }

    protected KalkulusRestKlient getKalkulusRestTjeneste() {
        return restTjeneste;
    }

    private Set<Inntektsmelding> finnInntektsmeldingerForSak(BehandlingReferanse referanse, List<StartBeregningInput> startBeregningInput) {
        var overstyrteInntektsmeldinger = finnOverstyrtInntektsmeldinger(startBeregningInput);
        if (!overstyrteInntektsmeldinger.isEmpty()) {
            return overstyrteInntektsmeldinger;
        }
        return new HashSet<>(iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer()));
    }

    private Set<Inntektsmelding> finnOverstyrtInntektsmeldinger(List<StartBeregningInput> startBeregningInput) {
        if (!togglePsbMigrering) {
            return Collections.emptySet();
        }
        return startBeregningInput.stream().flatMap(i -> i.getInputOverstyringPeriode().stream()).flatMap(overstyrtPeriode -> {
            LocalDate stp = overstyrtPeriode.getSkjæringstidspunkt();
            return overstyrtPeriode.getAktivitetOverstyringer().stream()
                .filter(a -> a.getAktivitetStatus().erArbeidstaker())
                .map(a -> InntektsmeldingBuilder.builder()
                    .medInnsendingstidspunkt(stp.atStartOfDay())
                    .medArbeidsgiver(a.getArbeidsgiver())
                    .medStartDatoPermisjon(stp)
                    .medRefusjon(a.getRefusjonPrÅr() == null ? BigDecimal.ZERO :
                        a.getRefusjonPrÅr().getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP))
                    .medBeløp(a.getInntektPrÅr().getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP))
                    .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
                    .medJournalpostId("OVERSTYRT_FOR_INFOTRYGDMIGRERING")
                    .medKanalreferanse("OVERSTYRT_FOR_INFOTRYGDMIGRERING")
                    .build());
        }).collect(Collectors.toSet());
    }

    protected StartBeregningListeRequest initStartRequest(BehandlingReferanse behandlingReferanse,
                                                          InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                          Collection<Inntektsmelding> sakInntektsmeldinger,
                                                          List<StartBeregningInput> startBeregningInput) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(behandlingReferanse.getFagsakId());

        AktørIdPersonident aktør = new AktørIdPersonident(fagsak.getAktørId().getId());

        var referanseRelasjoner = startBeregningInput.stream().collect(Collectors.toMap(StartBeregningInput::getBgReferanse, StartBeregningInput::getOriginalReferanser));

        Map<UUID, KalkulatorInputDto> input = lagInput(
            behandlingReferanse,
            iayGrunnlag,
            sakInntektsmeldinger,
            startBeregningInput);

        return new StartBeregningListeRequest(
            input,
            fagsak.getSaksnummer().getVerdi(),
            aktør,
            YtelseTyperKalkulusStøtterKontrakt.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
            referanseRelasjoner);
    }

    private Map<UUID, KalkulatorInputDto> lagInput(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, Collection<Inntektsmelding> sakInntektsmeldinger, List<StartBeregningInput> startBeregningInput) {
        Map<UUID, LocalDate> stpMap = startBeregningInput.stream()
            .collect(Collectors.toMap(StartBeregningInput::getBgReferanse, StartBeregningInput::getSkjæringstidspunkt));
        return getReferanseTilInputMap(
            behandlingReferanse,
            iayGrunnlag,
            sakInntektsmeldinger,
            stpMap
        );
    }

    private Map<UUID, KalkulatorInputDto> lagInputMap(Collection<BgRef> bgReferanser, BehandlingReferanse referanse) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());

        Map<UUID, LocalDate> stpMap = bgReferanser.stream().collect(Collectors.toMap(BgRef::getRef, BgRef::getStp));
        return getReferanseTilInputMap(referanse, iayGrunnlag, sakInntektsmeldinger, stpMap);
    }

    private Map<UUID, KalkulatorInputDto> getReferanseTilInputMap(BehandlingReferanse behandlingReferanse,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                  Collection<Inntektsmelding> sakInntektsmeldinger,
                                                                  Map<UUID, LocalDate> referanseSkjæringstidspunktMap) {
        Vilkår vilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var opptjeningsvilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        var mapper = getYtelsesspesifikkMapper(behandlingReferanse.getFagsakYtelseType());
        return referanseSkjæringstidspunktMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(entry -> {
                UUID bgReferanse = entry.getKey();
                var vilkårPeriode = vilkår.finnPeriodeForSkjæringstidspunkt(entry.getValue());

                VilkårUtfallMerknad vilkårsMerknad = null;
                if (opptjeningsvilkår.isPresent()) {
                    vilkårsMerknad = opptjeningsvilkår.get().finnPeriodeForSkjæringstidspunkt(vilkårPeriode.getSkjæringstidspunkt()).getMerknad();
                }

                var ytelsesGrunnlag = mapper.lagYtelsespesifiktGrunnlag(behandlingReferanse, vilkårPeriode.getPeriode());


                KalkulatorInputDto kalkulatorInputDto = kalkulatorInputTjeneste.byggDto(
                    behandlingReferanse,
                    bgReferanse,
                    iayGrunnlag,
                    sakInntektsmeldinger,
                    ytelsesGrunnlag,
                    vilkårPeriode.getPeriode(),
                    vilkårsMerknad);
                return new AbstractMap.SimpleEntry<>(bgReferanse,
                    kalkulatorInputDto);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    protected SamletKalkulusResultat mapFraTilstand(Collection<TilstandResponse> response, Collection<BgRef> bgReferanser) {

        Map<UUID, KalkulusResultat> resultater = new LinkedHashMap<>();
        for (var tilstandResponse : response) {
            var avklaringsbehovResultatList = tilstandResponse.getAvklaringsbehovMedTilstandDto().stream()
                .map(dto -> BeregningAvklaringsbehovResultat.opprettMedFristFor(BeregningAvklaringsbehovDefinisjon.fraKode(dto.getBeregningAvklaringsbehovDefinisjon().getKode()),
                    dto.getVenteårsak() != null ? BeregningVenteårsak.fraKode(dto.getVenteårsak().getKode()) : null, dto.getVentefrist()))
                .collect(Collectors.toList());
            KalkulusResultat kalkulusResultat = new KalkulusResultat(avklaringsbehovResultatList);
            if (tilstandResponse.getVilkarOppfylt() != null) {
                if (tilstandResponse.getVilkårsavslagsårsak() != null && !tilstandResponse.getVilkarOppfylt()) {
                    kalkulusResultat = kalkulusResultat.medAvslåttVilkår(mapTilAvslagsårsak(tilstandResponse.getVilkårsavslagsårsak()));
                } else {
                    kalkulusResultat = kalkulusResultat.medVilkårResultat(tilstandResponse.getVilkarOppfylt());
                }
            }
            resultater.put(tilstandResponse.getEksternReferanse(), kalkulusResultat);
        }
        return new SamletKalkulusResultat(resultater, bgReferanser);
    }

    private Avslagsårsak mapTilAvslagsårsak(Vilkårsavslagsårsak vilkårsavslagsårsak) {
        if (vilkårsavslagsårsak.getKode().equals(BeregningAvslagsårsak.SØKT_FL_INGEN_FL_INNTEKT.getKode())) {
            return Avslagsårsak.SØKT_FRILANS_UTEN_FRILANS_INNTEKT;
        } else if (vilkårsavslagsårsak.getKode().equals(BeregningAvslagsårsak.FOR_LAVT_BG.getKode())) {
            return Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG;
        } else if (vilkårsavslagsårsak.getKode().equals(BeregningAvslagsårsak.AVKORTET_GRUNNET_ANNEN_INNTEKT.getKode())) {
            return Avslagsårsak.AVKORTET_GRUNNET_ANNEN_INNTEKT;
        }
        return Avslagsårsak.UDEFINERT;
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        var ytelseTypeKode = ytelseType.getKode();
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
    }

    public Map<UUID, GrunnbeløpReguleringStatus> kontrollerBehovForGregulering(List<UUID> koblingerÅSpørreMot, Saksnummer saksnummer) {
        KontrollerGrunnbeløpRequest request = new KontrollerGrunnbeløpRequest(koblingerÅSpørreMot, saksnummer.getVerdi());
        GrunnbeløpReguleringRespons respons = restTjeneste.kontrollerBehovForGRegulering(request);
        return respons.getResultat();
    }
}
