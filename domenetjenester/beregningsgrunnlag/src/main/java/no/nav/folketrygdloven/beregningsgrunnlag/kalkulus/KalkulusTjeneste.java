package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import static no.nav.k9.StringTrimmer.trim;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.FraKalkulusMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.MapEndringsresultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdReferanseDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsgiverOpplysningerDto;
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
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.Grunnbeløp;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandListeResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringListeRespons;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAksjonspunktDefinisjon;
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
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
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
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;


    public KalkulusTjeneste() {
    }

    @Inject
    public KalkulusTjeneste(KalkulusRestKlient restTjeneste,
                            FagsakRepository fagsakRepository,
                            VilkårResultatRepository vilkårResultatRepository,
                            @FagsakYtelseTypeRef("*") KalkulatorInputTjeneste kalkulatorInputTjeneste,
                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                            ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                            @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper
    ) {
        this.restTjeneste = restTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.kalkulatorInputTjeneste = kalkulatorInputTjeneste;
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
    }

    public static List<ArbeidsgiverOpplysningerDto> mapArbeidsforholdOpplysninger(Map<Arbeidsgiver, ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger, List<ArbeidsforholdOverstyring> overstyringer) {
        List<ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysningerDtos = new ArrayList<>();
        arbeidsgiverOpplysninger.forEach((key, value) -> arbeidsgiverOpplysningerDtos.add(mapOpplysning(key, value)));
        overstyringer
            .stream()
            .filter(overstyring -> overstyring.getArbeidsgiverNavn() != null) // Vi er kun interessert i overstyringer der SBH har endret navn på arbeidsgiver
            .findFirst()
            .ifPresent(
                arbeidsforhold -> arbeidsgiverOpplysningerDtos.add(new ArbeidsgiverOpplysningerDto(mapArbeidsgiver(arbeidsforhold.getArbeidsgiver()), trim(arbeidsforhold.getArbeidsgiverNavn()))));
        return arbeidsgiverOpplysningerDtos;

    }

    public static ArbeidsgiverOpplysningerDto mapOpplysning(Arbeidsgiver key, ArbeidsgiverOpplysninger arbeidsgiverOpplysninger) {
        return new ArbeidsgiverOpplysningerDto(mapArbeidsgiver(key), trim(arbeidsgiverOpplysninger.getNavn()), arbeidsgiverOpplysninger.getFødselsdato());
    }

    private static Aktør mapArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.getErVirksomhet()) {
            return new Organisasjon(arbeidsgiver.getIdentifikator());
        }
        return new AktørIdPersonident(arbeidsgiver.getIdentifikator());
    }

    @Override
    public SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, List<StartBeregningInput> startBeregningInput) {
        if (startBeregningInput.isEmpty()) {
            return new SamletKalkulusResultat(Collections.emptyMap(), Collections.emptyMap());
        }
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
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
        var request = new FortsettBeregningListeRequest(referanse.getSaksnummer().getVerdi(), bgRefs, ytelseType, new StegType(stegType.getKode()));
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
        var request = new FortsettBeregningListeRequest(referanse.getSaksnummer().getVerdi(), referanser, input, ytelseType, new StegType(stegType.getKode()));
        return restTjeneste.fortsettBeregning(request);
    }

    @Override
    public List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(BehandlingReferanse behandlingReferanse,
                                                                           Collection<BgRef> bgReferanser,
                                                                           Map<UUID, HåndterBeregningDto> håndterberegningMap) {
        List<HåndterBeregningRequest> requestListe = håndterberegningMap.entrySet().stream().map(e -> new HåndterBeregningRequest(e.getValue(), e.getKey())).collect(Collectors.toList());
        var oppdateringRespons = restTjeneste.oppdaterBeregningListe(new HåndterBeregningListeRequest(requestListe, behandlingReferanse.getBehandlingUuid()));
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
        var request = new HåndterBeregningListeRequest(requestListe, input, referanse.getSaksnummer().getVerdi(), referanse.getBehandlingUuid());
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
        List<ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysningerListe = lagArbeidsgiverOpplysningListe(referanse, inntektArbeidYtelseGrunnlag);
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
            arbeidsgiverOpplysningerListe,
            referanser, vilkår.finnPeriodeForSkjæringstidspunkt(it.getSkjæringstidspunkt()).getPeriode().getFomDato()))
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

    protected StartBeregningListeRequest initStartRequest(BehandlingReferanse behandlingReferanse,
                                                          InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                          Collection<Inntektsmelding> sakInntektsmeldinger,
                                                          List<StartBeregningInput> startBeregningInput) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(behandlingReferanse.getFagsakId());

        AktørIdPersonident aktør = new AktørIdPersonident(fagsak.getAktørId().getId());

        Map<UUID, LocalDate> stpMap = startBeregningInput.stream().collect(Collectors.toMap(StartBeregningInput::getBgReferanse, StartBeregningInput::getSkjæringstidspunkt));
        Map<UUID, KalkulatorInputDto> input = getReferanseTilInputMap(behandlingReferanse,
            iayGrunnlag,
            sakInntektsmeldinger, stpMap);

        return new StartBeregningListeRequest(
            input,
            fagsak.getSaksnummer().getVerdi(),
            aktør,
            YtelseTyperKalkulusStøtterKontrakt.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()));
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
                return new AbstractMap.SimpleEntry<>(bgReferanse,
                    kalkulatorInputTjeneste.byggDto(
                        behandlingReferanse,
                        bgReferanse,
                        iayGrunnlag,
                        sakInntektsmeldinger,
                        ytelsesGrunnlag,
                        vilkårPeriode.getPeriode(),
                        vilkårsMerknad));
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    protected SamletKalkulusResultat mapFraTilstand(Collection<TilstandResponse> response, Collection<BgRef> bgReferanser) {

        Map<UUID, KalkulusResultat> resultater = new LinkedHashMap<>();
        for (var tilstandResponse : response) {
            List<BeregningAksjonspunktResultat> aksjonspunktResultatList = tilstandResponse.getAksjonspunktMedTilstandDto().stream()
                .map(dto -> BeregningAksjonspunktResultat.opprettMedFristFor(BeregningAksjonspunktDefinisjon.fraKode(dto.getBeregningAksjonspunktDefinisjon().getKode()),
                    dto.getVenteårsak() != null ? BeregningVenteårsak.fraKode(dto.getVenteårsak().getKode()) : null, dto.getVentefrist()))
                .collect(Collectors.toList());
            KalkulusResultat kalkulusResultat = new KalkulusResultat(aksjonspunktResultatList);
            if (tilstandResponse.getVilkarOppfylt() != null) {
                if (tilstandResponse.getVilkårsavslagsårsak() != null && !tilstandResponse.getVilkarOppfylt()) {
                    kalkulusResultat = kalkulusResultat.medAvslåttVilkår(mapTilAvslagsårsak(tilstandResponse.getVilkårsavslagsårsak()));
                } else {
                    kalkulusResultat = kalkulusResultat.medVilkårResulatat(tilstandResponse.getVilkarOppfylt());
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

    private List<ArbeidsgiverOpplysningerDto> lagArbeidsgiverOpplysningListe(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag iayGrunnlag) {

        Map<Arbeidsgiver, ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger = iayGrunnlag.getAktørArbeidFraRegister(referanse.getAktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .map(Yrkesaktivitet::getArbeidsgiver)
            .distinct()
            .collect(Collectors.toMap(a -> a, arbeidsgiverTjeneste::hent));
        return mapArbeidsforholdOpplysninger(arbeidsgiverOpplysninger, iayGrunnlag.getArbeidsforholdOverstyringer());
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        var ytelseTypeKode = ytelseType.getKode();
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
    }
}
