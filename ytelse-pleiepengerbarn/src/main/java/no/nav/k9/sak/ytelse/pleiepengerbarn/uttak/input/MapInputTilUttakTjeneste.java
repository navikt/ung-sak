package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie.MapFerie;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn.MapTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak.MapUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov;
import no.nav.pleiepengerbarn.uttak.kontrakter.Søker;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Vilkårsperiode;

@Dependent
public class MapInputTilUttakTjeneste {

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public MapInputTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    FagsakRepository fagsakRepository,
                                    InntektArbeidYtelseTjeneste iayTjeneste,
                                    @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                    @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.iayTjeneste = iayTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var uttakGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleiebehov = pleiebehovResultatRepository.hent(referanse.getBehandlingId());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var utvidetRevurderingPerioder = perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(referanse);
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(referanse);
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
        var fagsak = behandling.getFagsak();
        var fagsakPeriode = fagsak.getPeriode();
        var relaterteFagsaker = fagsakRepository.finnFagsakRelatertTil(behandling.getFagsakYtelseType(),
            fagsak.getPleietrengendeAktørId(),
            null,
            fagsakPeriode.getFomDato().minusWeeks(25),
            fagsakPeriode.getTomDato().plusWeeks(25))
            .stream().map(Fagsak::getSaksnummer)
            .filter(it -> !fagsak.getSaksnummer().equals(it))
            .collect(Collectors.toSet());

        var input = new InputParametere()
            .medBehandling(behandling)
            .medVilkårene(vilkårene)
            .medPleiebehovResultat(pleiebehov)
            .medPerioderTilVurdering(perioderTilVurdering)
            .medUtvidetPerioderRevurdering(utvidetRevurderingPerioder)
            .medVurderteSøknadsperioder(vurderteSøknadsperioder)
            .medInntektsmeldinger(sakInntektsmeldinger)
            .medPersonopplysninger(personopplysningerAggregat)
            .medRelaterteSaker(relaterteFagsaker)
            .medUttaksGrunnlag(uttakGrunnlag);

        return toRequestData(input);
    }

    private Uttaksgrunnlag toRequestData(InputParametere input) {

        var behandling = input.getBehandling();
        var vurderteSøknadsperioder = input.getVurderteSøknadsperioder();
        var uttaksPerioderGrunnlag = input.getUttaksGrunnlag();
        var personopplysningerAggregat = input.getPersonopplysningerAggregat();

        // Henter ut alt og lager tidlinje av denne for så å ta ut den delen som er relevant
        // NB! Kan gi issues ved lange fagsaker mtp ytelse
        var perioderFraSøknader = uttaksPerioderGrunnlag.getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene();
        var kravDokumenter = vurderteSøknadsperioder.keySet();

        if (perioderFraSøknader.size() != kravDokumenter.size()) {
            throw new IllegalStateException("Fant ikke alle dokumentene siden '" + kravDokumenter + "' != '" + perioderFraSøknader + "'");
        }

        var søkerPersonopplysninger = personopplysningerAggregat.getSøker();
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());

        var barn = new Barn(pleietrengendePersonopplysninger.getAktørId().getId(), pleietrengendePersonopplysninger.getDødsdato());
        var søker = new Søker(søkerPersonopplysninger.getAktørId().getId());

        final List<String> andrePartersSaksnummer = input.getRelaterteSaker()
            .stream()
            .map(Saksnummer::toString)
            .collect(Collectors.toList());

        var tidslinjeTilVurdering = new LocalDateTimeline<>(mapPerioderTilVurdering(input));

        final List<SøktUttak> søktUttak = new MapUttak().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering);

        // TODO: Se kommentarer/TODOs under denne:
        final List<Arbeid> arbeid = new MapArbeid().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering, input.getSakInntektsmeldinger());

        final Map<LukketPeriode, Pleiebehov> pleiebehov = toPleiebehov(input.getPleiebehovResultat());

        final List<LukketPeriode> lovbestemtFerie = new MapFerie().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(input.getVilkårene());

        final Map<LukketPeriode, Duration> tilsynsperioder = new MapTilsyn().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering);

        //TODO: fyll beredskap og nattevåksperioder med data fra aksjonspunkt når det er ferdig
        final Set<LukketPeriode> beredskapsperioder = new HashSet<>();
        final Set<LukketPeriode> nattevåksperioder = new HashSet<>();

        return new Uttaksgrunnlag(
            barn,
            søker,
            behandling.getFagsak().getSaksnummer().getVerdi(),
            behandling.getUuid().toString(),
            andrePartersSaksnummer,
            søktUttak,
            arbeid,
            pleiebehov,
            lovbestemtFerie,
            inngangsvilkår,
            tilsynsperioder,
            beredskapsperioder,
            nattevåksperioder
            );
    }

    private List<LocalDateSegment<Boolean>> mapPerioderTilVurdering(InputParametere input) {

        var result = input.getPerioderTilVurdering()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toCollection(ArrayList::new));

        result.addAll(input.getUtvidetRevurderingPerioder()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));

        return result;
    }

    private Map<LukketPeriode, Pleiebehov> toPleiebehov(PleiebehovResultat pleiebehov) {
        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = new HashMap<>();
        pleiebehov.getPleieperioder().getPerioder().forEach(p -> {
            tilsynsbehov.put(toLukketPeriode(p.getPeriode()), mapToPleiebehov(p.getGrad()));
        });
        return tilsynsbehov;
    }

    private LukketPeriode toLukketPeriode(DatoIntervallEntitet periode) {
        return new LukketPeriode(periode.getFomDato(), periode.getTomDato());
    }

    private Pleiebehov mapToPleiebehov(Pleiegrad grad) {
        switch (grad) {
            case INGEN:
                return Pleiebehov.PROSENT_0;
            case KONTINUERLIG_TILSYN:
                return Pleiebehov.PROSENT_100;
            case UTVIDET_KONTINUERLIG_TILSYN:
            case INNLEGGELSE:
                return Pleiebehov.PROSENT_200;
            default:
                throw new IllegalStateException("Ukjent Pleiegrad: " + grad);
        }
    }

    private HashMap<String, List<Vilkårsperiode>> toInngangsvilkår(Vilkårene vilkårene) {
        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = new HashMap<>();
        vilkårene.getVilkårene().forEach(v -> {
            if (v.getVilkårType() == VilkårType.BEREGNINGSGRUNNLAGVILKÅR) {
                return;
            }
            final List<Vilkårsperiode> vilkårsperioder = v.getPerioder()
                .stream()
                .map(vp -> new Vilkårsperiode(new LukketPeriode(vp.getFom(), vp.getTom()), Utfall.valueOf(vp.getUtfall().getKode())))
                .collect(Collectors.toList());
            inngangsvilkår.put(v.getVilkårType().getKode(), vilkårsperioder);
        });
        return inngangsvilkår;
    }

}
