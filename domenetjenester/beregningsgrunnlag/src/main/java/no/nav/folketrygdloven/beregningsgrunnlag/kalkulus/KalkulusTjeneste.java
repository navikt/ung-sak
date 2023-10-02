package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
import no.nav.folketrygdloven.kalkulus.beregning.v1.AvklaringsbehovMedTilstandDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdReferanseDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.GrunnbeløpReguleringStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Vilkårsavslagsårsak;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoListeForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KontrollerGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.simulerTilkommetInntekt.SimulerTilkommetInntektForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.simulerTilkommetInntekt.SimulerTilkommetInntektListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.Grunnbeløp;
import no.nav.folketrygdloven.kalkulus.response.v1.GrunnbeløpReguleringRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandListeResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringListeRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.simulerTilkommetInntekt.SimulertTilkommetInntektPrReferanse;
import no.nav.folketrygdloven.kalkulus.response.v1.tilkommetAktivitet.UtledetTilkommetAktivitet;
import no.nav.folketrygdloven.kalkulus.response.v1.tilkommetAktivitet.UtledetTilkommetAktivitetPrReferanse;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvklaringsbehovDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvslagsårsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningVenteårsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * KalkulusTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt
 * (https://github.com/navikt/ft-kalkulus/)
 */
@ApplicationScoped
@FagsakYtelseTypeRef
@Default
public class KalkulusTjeneste implements KalkulusApiTjeneste {

    protected InntektArbeidYtelseTjeneste iayTjeneste;
    protected LagBeregnRequestTjeneste beregnRequestTjeneste;
    private KalkulusRestKlient restTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;
    private FinnInntektsmeldingForBeregning finnInntektsmeldingForBeregning;

    public KalkulusTjeneste() {
    }

    @Inject
    public KalkulusTjeneste(KalkulusRestKlient restTjeneste,
                            VilkårResultatRepository vilkårResultatRepository,
                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                            @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper,
                            LagBeregnRequestTjeneste beregnRequestTjeneste,
                            FinnInntektsmeldingForBeregning finnInntektsmeldingForBeregning) {
        this.restTjeneste = restTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
        this.beregnRequestTjeneste = beregnRequestTjeneste;
        this.finnInntektsmeldingForBeregning = finnInntektsmeldingForBeregning;
    }

    @Override
    public SamletKalkulusResultat beregn(BehandlingReferanse referanse,
                                         List<BeregnInput> beregningInput,
                                         BehandlingStegType stegType) {
        if (beregningInput.isEmpty()) {
            return new SamletKalkulusResultat(Collections.emptyMap(), Collections.emptyMap());
        }
        validerNyReferanse(beregningInput);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var sakInntektsmeldinger = finnInntektsmeldingForBeregning.finnInntektsmeldinger(referanse, beregningInput);
        var request = beregnRequestTjeneste.lagMedInput(stegType,
            referanse,
            beregningInput,
            iayGrunnlag, sakInntektsmeldinger);
        TilstandListeResponse tilstandResponse = restTjeneste.beregn(request);
        var bgReferanser = beregningInput.stream()
            .map(i -> new BgRef(i.getBgReferanse(), i.getSkjæringstidspunkt()))
            .toList();
        return mapFraTilstand(tilstandResponse.getTilstand(), bgReferanser);
    }

    private void validerNyReferanse(List<BeregnInput> beregningInput) {
        if (beregningInput.stream().anyMatch(i -> i.getOriginalReferanser().stream().anyMatch(r -> r.equals(i.getBgReferanse())))) {
            throw new IllegalStateException("Ny referanse skal vere ulik original referanse");
        }
    }

    @Override
    public void kopier(BehandlingReferanse referanse, List<BeregnInput> beregningInput, StegType stegType) {
        if (beregningInput.isEmpty()) {
            return;
        }
        var request = getKopierBeregningListeRequest(referanse, beregningInput, stegType);
        restTjeneste.kopierBeregning(request);
    }

    private KopierBeregningListeRequest getKopierBeregningListeRequest(BehandlingReferanse referanse, List<BeregnInput> beregningInput, StegType stegType) {
        return new KopierBeregningListeRequest(referanse.getSaksnummer().getVerdi(),
            referanse.getBehandlingUuid(),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode()),
            stegType,
            beregningInput.stream().map(i -> new KopierBeregningRequest(i.getBgReferanse(),
                i.getOriginalReferanseMedSammeSkjæringstidspunkt().orElseThrow(() -> new IllegalStateException("Forventer å finne original referanse med samme skjæringstidspunkt ved kopiering"))
            )).toList());
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
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
        var vilkår = vilkårResultatRepository.hent(referanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();
        var bergnInputListe = bgReferanser.stream().map(r -> BeregnInput.forAksjonspunktOppdatering(r.getRef(), vilkår.finnPeriodeForSkjæringstidspunkt(r.getStp()).getPeriode())).toList();
        var request = beregnRequestTjeneste.lagForAksjonspunktOppdateringMedInput(
            requestListe,
            referanse,
            bergnInputListe,
            iayGrunnlag,
            sakInntektsmeldinger);
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
    public void deaktiverBeregningsgrunnlag(FagsakYtelseType fagsakYtelseType, Saksnummer saksnummer, UUID behandlingUuid, List<UUID> bgReferanse) {
        var bgRequests = bgReferanse.stream().map(BeregningsgrunnlagRequest::new).collect(Collectors.toList());
        var request = new BeregningsgrunnlagListeRequest(saksnummer.getVerdi(), bgRequests, behandlingUuid);
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

    protected SamletKalkulusResultat mapFraTilstand(Collection<TilstandResponse> response, Collection<BgRef> bgReferanser) {

        Map<UUID, KalkulusResultat> resultater = new LinkedHashMap<>();
        for (var tilstandResponse : response) {
            var avklaringsbehovResultatList = tilstandResponse.getAvklaringsbehovMedTilstandDto().stream()
                .map(dto -> BeregningAvklaringsbehovResultat.opprettMedFristFor(mapTilAvklaringsbehov(dto),
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

    private BeregningAvklaringsbehovDefinisjon mapTilAvklaringsbehov(AvklaringsbehovMedTilstandDto dto) {
        return BeregningAvklaringsbehovDefinisjon.fraKode(dto.getBeregningAvklaringsbehovDefinisjon().getKode());
    }

    private Avslagsårsak mapTilAvslagsårsak(Vilkårsavslagsårsak vilkårsavslagsårsak) {
        if (vilkårsavslagsårsak.getKode().equals(BeregningAvslagsårsak.SØKT_FL_INGEN_FL_INNTEKT.getKode())) {
            return Avslagsårsak.SØKT_FRILANS_UTEN_FRILANS_INNTEKT;
        } else if (vilkårsavslagsårsak.getKode().equals(BeregningAvslagsårsak.FOR_LAVT_BG.getKode())) {
            return Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG;
        } else if (vilkårsavslagsårsak.getKode().equals(BeregningAvslagsårsak.FOR_LAVT_BG_8_47.getKode())) {
            return Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG_8_47;
        } else if (vilkårsavslagsårsak.getKode().equals(BeregningAvslagsårsak.AVKORTET_GRUNNET_ANNEN_INNTEKT.getKode())) {
            return Avslagsårsak.AVKORTET_GRUNNET_ANNEN_INNTEKT;
        }
        return Avslagsårsak.UDEFINERT;
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseType).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseType));
    }

    public Map<UUID, GrunnbeløpReguleringStatus> kontrollerBehovForGregulering(List<UUID> koblingerÅSpørreMot, Saksnummer saksnummer) {
        if (koblingerÅSpørreMot.isEmpty()) {
            return Map.of();
        }
        KontrollerGrunnbeløpRequest request = new KontrollerGrunnbeløpRequest(koblingerÅSpørreMot, saksnummer.getVerdi());
        GrunnbeløpReguleringRespons respons = restTjeneste.kontrollerBehovForGRegulering(request);
        return respons.getResultat();
    }

    public Map<UUID, List<DatoIntervallEntitet>> simulerTilkommetInntekt(FagsakYtelseType ytelseType, Map<UUID, DatoIntervallEntitet> koblingerOgPeriode,
                                                                         Saksnummer saksnummer) {
        if (koblingerOgPeriode.isEmpty()) {
            return Map.of();
        }
        final YtelseTyperKalkulusStøtterKontrakt ytelseTypeKalkulus = YtelseTyperKalkulusStøtterKontrakt.fraKode(ytelseType.getKode());
        var request = new SimulerTilkommetInntektListeRequest(
            saksnummer.getVerdi(),
            ytelseTypeKalkulus,
            koblingerOgPeriode.entrySet().stream()
                .map(e ->
                    new SimulerTilkommetInntektForRequest(e.getKey(),
                        List.of(new Periode(e.getValue().getFomDato(), e.getValue().getTomDato()))))
                .toList());
        var respons = restTjeneste.simulerTilkommetInntekt(request);
        return respons.getSimulertListe().stream().collect(Collectors.toMap(
            SimulertTilkommetInntektPrReferanse::getEksternReferanse,
            it -> it.getTilkommetAktivitetPerioder().stream().map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom())).toList()
        ));
    }


    public Map<UUID, List<UtledetTilkommetAktivitet>> utledTilkommetAktivitet(Map<UUID, DatoIntervallEntitet> koblingerOgPeriode,
                                                                              BehandlingReferanse referanse) {
        if (koblingerOgPeriode.isEmpty()) {
            return Map.of();
        }

        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
        var beregnInput = koblingerOgPeriode.entrySet().stream().map(e -> BeregnInput.forTilkommetInntektUtledning(e.getKey(), e.getValue())).toList();
        var request = beregnRequestTjeneste.lagForUtledningAvTilkommetInntekt(referanse, beregnInput, iayGrunnlag, sakInntektsmeldinger);
        var respons = restTjeneste.utledTilkommetAktivitet(request);
        return respons.getListe().stream().collect(Collectors.toMap(
            UtledetTilkommetAktivitetPrReferanse::getEksternReferanse,
            UtledetTilkommetAktivitetPrReferanse::getTilkommedeAktiviteter
        ));
    }
}
