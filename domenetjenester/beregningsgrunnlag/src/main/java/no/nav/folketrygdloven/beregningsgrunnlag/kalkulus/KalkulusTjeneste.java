package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.FraKalkulusMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.MapFraKalkulusTilK9;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.MapEndringsresultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.UuidDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdReferanseDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsgiverOpplysningerDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.ErEndringIBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagGrunnlagForReferanseRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.frisinn.Vilkårsavslagsårsak;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringRespons;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAksjonspunktDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvslagsårsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningVenteårsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
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
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

/**
 * KalkulusTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt (https://github.com/navikt/ft-kalkulus/)
 */
@ApplicationScoped
@FagsakYtelseTypeRef
@Default
public class KalkulusTjeneste implements KalkulusApiTjeneste {

    protected KalkulusRestTjeneste restTjeneste;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private KalkulatorInputTjeneste kalkulatorInputTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public KalkulusTjeneste() {
    }

    @Inject
    public KalkulusTjeneste(KalkulusRestTjeneste restTjeneste,
                            FagsakRepository fagsakRepository,
                            VilkårResultatRepository vilkårResultatRepository, KalkulatorInputTjeneste kalkulatorInputTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.restTjeneste = restTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.kalkulatorInputTjeneste = kalkulatorInputTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public static List<ArbeidsgiverOpplysningerDto> mapArbeidsforholdOpplysninger(Map<Arbeidsgiver, ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger, List<ArbeidsforholdOverstyring> overstyringer) {
        List<ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysningerDtos = new ArrayList<>();
        arbeidsgiverOpplysninger.forEach((key, value) -> arbeidsgiverOpplysningerDtos.add(mapOpplysning(key, value)));
        overstyringer
            .stream()
            .filter(overstyring -> overstyring.getArbeidsgiverNavn() != null) // Vi er kun interessert i overstyringer der SBH har endret navn på arbeidsgiver
            .findFirst()
            .ifPresent(arbeidsforhold -> arbeidsgiverOpplysningerDtos.add(new ArbeidsgiverOpplysningerDto(mapArbeidsgiver(arbeidsforhold.getArbeidsgiver()), arbeidsforhold.getArbeidsgiverNavn())));
        return arbeidsgiverOpplysningerDtos;

    }

    public static ArbeidsgiverOpplysningerDto mapOpplysning(Arbeidsgiver key, ArbeidsgiverOpplysninger arbeidsgiverOpplysninger) {
        return new ArbeidsgiverOpplysningerDto(mapArbeidsgiver(key), arbeidsgiverOpplysninger.getNavn(), arbeidsgiverOpplysninger.getFødselsdato());
    }

    private static Aktør mapArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.getErVirksomhet()) {
            return new Organisasjon(arbeidsgiver.getIdentifikator());
        }
        return new AktørIdPersonident(arbeidsgiver.getIdentifikator());
    }

    @Override
    public KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, UUID bgReferanse, LocalDate skjæringstidspunkt) {
        StartBeregningRequest startBeregningRequest = initStartRequest(referanse, ytelseGrunnlag, bgReferanse, skjæringstidspunkt);
        TilstandResponse tilstandResponse = restTjeneste.startBeregning(startBeregningRequest);
        return mapFraTilstand(tilstandResponse);
    }

    @Override
    public KalkulusResultat fortsettBeregning(FagsakYtelseType fagsakYtelseType, UUID bgReferanse, BehandlingStegType stegType) {
        TilstandResponse tilstandResponse = restTjeneste.fortsettBeregning(new FortsettBeregningRequest(bgReferanse,
            new YtelseTyperKalkulusStøtterKontrakt(fagsakYtelseType.getKode()), new StegType(stegType.getKode())));
        return mapFraTilstand(tilstandResponse);
    }

    @Override
    public OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, UUID bgReferanse) {
        HåndterBeregningRequest håndterBeregningRequest = new HåndterBeregningRequest(håndterBeregningDto, bgReferanse);
        OppdateringRespons oppdateringRespons = restTjeneste.oppdaterBeregning(håndterBeregningRequest);
        return MapEndringsresultat.mapFraOppdateringRespons(oppdateringRespons);
    }

    @Override
    public Beregningsgrunnlag hentEksaktFastsatt(FagsakYtelseType fagsakYtelseType, UUID bgReferanse) {
        Optional<Beregningsgrunnlag> beregningsgrunnlag = hentGrunnlag(fagsakYtelseType, bgReferanse).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        if (beregningsgrunnlag.isPresent()) {
            return beregningsgrunnlag.get();
        }
        throw new IllegalStateException("Kalkulus har ikke fastsatt for " + bgReferanse);
    }

    @Override
    public BeregningsgrunnlagDto hentBeregningsgrunnlagDto(BehandlingReferanse referanse, UUID bgReferanse) {
        HentBeregningsgrunnlagDtoForGUIRequest request = lagHentBeregningsgrunnlagRequest(bgReferanse, referanse);
        return restTjeneste.hentBeregningsgrunnlagDto(request);
    }

    @Override
    public Optional<Beregningsgrunnlag> hentFastsatt(UUID bgReferanse, FagsakYtelseType fagsakYtelseType) {
        YtelseTyperKalkulusStøtterKontrakt ytelse = new YtelseTyperKalkulusStøtterKontrakt(fagsakYtelseType.getKode());

        HentBeregningsgrunnlagRequest hentBeregningsgrunnlagRequest = new HentBeregningsgrunnlagRequest(bgReferanse, ytelse);
        no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.BeregningsgrunnlagDto beregningsgrunnlagDto = restTjeneste.hentFastsatt(hentBeregningsgrunnlagRequest);
        if (beregningsgrunnlagDto == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(MapFraKalkulusTilK9.mapBeregningsgrunnlag(beregningsgrunnlagDto));
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(FagsakYtelseType fagsakYtelseType, UUID bgReferanse) {
        YtelseTyperKalkulusStøtterKontrakt ytelseSomSkalBeregnes = new YtelseTyperKalkulusStøtterKontrakt(fagsakYtelseType.getKode());
        HentBeregningsgrunnlagRequest request = new HentBeregningsgrunnlagRequest(
            bgReferanse,
            ytelseSomSkalBeregnes
        );
        BeregningsgrunnlagGrunnlagDto beregningsgrunnlagGrunnlagDto = restTjeneste.hentBeregningsgrunnlagGrunnlag(request);
        if (beregningsgrunnlagGrunnlagDto == null) {
            return Optional.empty();
        }
        return Optional.of(FraKalkulusMapper.mapBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlagDto));
    }

    @Override
    public void lagreBeregningsgrunnlag(BehandlingReferanse behandlingReferanse, Beregningsgrunnlag beregningsgrunnlag, BeregningsgrunnlagTilstand opprettet) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public Optional<Beregningsgrunnlag> hentBeregningsgrunnlagForId(UUID bgReferanse, FagsakYtelseType fagsakYtelseType, UUID bgGrunnlagsVersjon) {
        YtelseTyperKalkulusStøtterKontrakt ytelseSomSkalBeregnes = new YtelseTyperKalkulusStøtterKontrakt(fagsakYtelseType.getKode());
        HentBeregningsgrunnlagGrunnlagForReferanseRequest request = new HentBeregningsgrunnlagGrunnlagForReferanseRequest(
            bgReferanse,
            ytelseSomSkalBeregnes,
            bgGrunnlagsVersjon
        );
        BeregningsgrunnlagGrunnlagDto beregningsgrunnlagGrunnlagDto = restTjeneste.hentBeregningsgrunnlagGrunnlagForReferanse(request);
        if (beregningsgrunnlagGrunnlagDto == null) {
            return Optional.empty();
        }
        Beregningsgrunnlag beregningsgrunnlag = FraKalkulusMapper.mapBeregningsgrunnlag(beregningsgrunnlagGrunnlagDto.getBeregningsgrunnlag());
        return Optional.of(beregningsgrunnlag);
    }

    @Override
    public void deaktiverBeregningsgrunnlag(FagsakYtelseType fagsakYtelseType, UUID bgReferanse) {
        YtelseTyperKalkulusStøtterKontrakt ytelseSomSkalBeregnes = new YtelseTyperKalkulusStøtterKontrakt(fagsakYtelseType.getKode());
        HentBeregningsgrunnlagRequest request = new HentBeregningsgrunnlagRequest(
            bgReferanse,
            ytelseSomSkalBeregnes
        );
        restTjeneste.deaktiverBeregningsgrunnlag(request);
    }

    @Override
    public Boolean erEndringIBeregning(FagsakYtelseType fagsakYtelseType1, UUID bgReferanse1, FagsakYtelseType fagsakYtelseType2, UUID bgReferanse2) {

        if (!fagsakYtelseType1.equals(fagsakYtelseType2)) {
            throw new IllegalArgumentException("Kan ikkje sjekke endring for forskjellige ytelsetyper");
        }

        YtelseTyperKalkulusStøtterKontrakt ytelseSomSkalBeregnes = new YtelseTyperKalkulusStøtterKontrakt(fagsakYtelseType1.getKode());
        ErEndringIBeregningRequest request = new ErEndringIBeregningRequest(
            bgReferanse1,
            bgReferanse2,
            ytelseSomSkalBeregnes
        );
        return restTjeneste.erEndringIBeregning(request);
    }

    private HentBeregningsgrunnlagDtoForGUIRequest lagHentBeregningsgrunnlagRequest(UUID bgReferanse, BehandlingReferanse referanse) {
        YtelseTyperKalkulusStøtterKontrakt ytelseSomSkalBeregnes = new YtelseTyperKalkulusStøtterKontrakt(referanse.getFagsakYtelseType().getKode());
        List<ArbeidsgiverOpplysningerDto> arbeidsgiverOpplysningerListe = lagArbeidsgiverOpplysningListe(referanse);
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId());
        Set<ArbeidsforholdReferanseDto> referanser = inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon()
            .stream()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .flatMap(Collection::stream)
            .map(ref -> new ArbeidsforholdReferanseDto(TilKalkulusMapper.mapTilAktør(ref.getArbeidsgiver()),
                new InternArbeidsforholdRefDto(ref.getInternReferanse().getReferanse()),
                new EksternArbeidsforholdRef(ref.getEksternReferanse().getReferanse())))
            .collect(Collectors.toSet());

        return new HentBeregningsgrunnlagDtoForGUIRequest(
            bgReferanse,
            ytelseSomSkalBeregnes,
            arbeidsgiverOpplysningerListe,
            referanser
        );
    }

    protected StartBeregningRequest initStartRequest(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, UUID bgReferanse, LocalDate skjæringstidspunkt) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(referanse.getFagsakId());

        AktørIdPersonident aktør = new AktørIdPersonident(fagsak.getAktørId().getId());
        var vilkårsPeriode = vilkårResultatRepository.hent(referanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow().finnPeriodeForSkjæringstidspunkt(skjæringstidspunkt).getPeriode();
        KalkulatorInputDto kalkulatorInputDto = kalkulatorInputTjeneste.byggDto(referanse, ytelseGrunnlag, vilkårsPeriode);

        return new StartBeregningRequest(
            new UuidDto(bgReferanse),
            fagsak.getSaksnummer().getVerdi(),
            aktør,
            new YtelseTyperKalkulusStøtterKontrakt(referanse.getFagsakYtelseType().getKode()),
            kalkulatorInputDto);
    }

    protected KalkulusResultat mapFraTilstand(TilstandResponse tilstandResponse) {
        List<BeregningAksjonspunktResultat> aksjonspunktResultatList = tilstandResponse.getAksjonspunktMedTilstandDto().stream().map(dto -> BeregningAksjonspunktResultat.opprettMedFristFor(BeregningAksjonspunktDefinisjon.fraKode(dto.getBeregningAksjonspunktDefinisjon().getKode()),
            dto.getVenteårsak() != null ? BeregningVenteårsak.fraKode(dto.getVenteårsak().getKode()) : null, dto.getVentefrist())).collect(Collectors.toList());
        KalkulusResultat kalkulusResultat = new KalkulusResultat(aksjonspunktResultatList);
        if (tilstandResponse.getVilkårsperioder() != null) {
            tilstandResponse.getVilkårsperioder().forEach(vp -> {
                Periode periode = vp.getVilkårsperiode();
                DatoIntervallEntitet vilkårPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
                if (!vp.isErVilkårOppfylt()) {
                    kalkulusResultat.leggTilVilkårResultat(vilkårPeriode, false, mapTilAvslagsårsak(vp.getVilkårsavslagsårsak()));
                } else {
                    kalkulusResultat.leggTilVilkårResultat(vilkårPeriode, true, null);

                }
            });
        }
        if (tilstandResponse.getVilkarOppfylt() != null) {
            if (tilstandResponse.getVilkårsavslagsårsak() != null && !tilstandResponse.getVilkarOppfylt()) {
                return kalkulusResultat.medAvslåttVilkår(mapTilAvslagsårsak(tilstandResponse.getVilkårsavslagsårsak()));
            }
            return kalkulusResultat.medVilkårResulatat(tilstandResponse.getVilkarOppfylt());
        }
        return kalkulusResultat;
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

    private List<ArbeidsgiverOpplysningerDto> lagArbeidsgiverOpplysningListe(BehandlingReferanse referanse) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId());

        Map<Arbeidsgiver, ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger = iayGrunnlag.getAktørArbeidFraRegister(referanse.getAktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .map(Yrkesaktivitet::getArbeidsgiver)
            .distinct()
            .collect(Collectors.toMap(a -> a, arbeidsgiverTjeneste::hent));
        return mapArbeidsforholdOpplysninger(arbeidsgiverOpplysninger, iayGrunnlag.getArbeidsforholdOverstyringer());
    }
}
