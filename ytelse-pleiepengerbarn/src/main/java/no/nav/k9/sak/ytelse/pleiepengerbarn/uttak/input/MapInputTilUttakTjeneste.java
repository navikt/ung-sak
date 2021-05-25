package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtledetEtablertTilsyn;
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
    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private EtablertTilsynTjeneste tilsynTjeneste;

    @Inject
    public MapInputTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                    UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    FagsakRepository fagsakRepository,
                                    InntektArbeidYtelseTjeneste iayTjeneste,
                                    EtablertTilsynTjeneste tilsynTjeneste,
                                    @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                    @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.iayTjeneste = iayTjeneste;
        this.tilsynTjeneste = tilsynTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var uttakGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleiebehov = pleiebehovResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());
        var perioderTilVurdering = finnSykdomsperioder(referanse);
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
        final LocalDateTimeline<UtledetEtablertTilsyn> utledetEtablertTilsyn = tilsynTjeneste.beregnTilsynstidlinje(referanse);

        var input = new InputParametere()
            .medBehandling(behandling)
            .medVilkårene(vilkårene)
            .medPleiebehov(pleiebehov.map(pb -> pb.getPleieperioder().getPerioder()).orElse(List.of()))
            .medPerioderTilVurdering(perioderTilVurdering)
            .medUtvidetPerioderRevurdering(utvidetRevurderingPerioder)
            .medVurderteSøknadsperioder(vurderteSøknadsperioder)
            .medInntektsmeldinger(sakInntektsmeldinger)
            .medPersonopplysninger(personopplysningerAggregat)
            .medRelaterteSaker(relaterteFagsaker)
            .medUttaksGrunnlag(uttakGrunnlag)
            .medUtledetEtablertTilsyn(utledetEtablertTilsyn);

        return toRequestData(input);
    }

    private NavigableSet<DatoIntervallEntitet> finnSykdomsperioder(BehandlingReferanse referanse) {
        final var s1 = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        final var s2 = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        final var resultat = new TreeSet<>(s1);
        resultat.addAll(s2);
        return resultat;
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

        evaluerDokumenter(perioderFraSøknader, kravDokumenter);

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

        final Map<LukketPeriode, Pleiebehov> pleiebehov = toPleiebehov(input.getPleiebehov());

        final List<LukketPeriode> lovbestemtFerie = new MapFerie().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(input.getVilkårene());

        final Map<LukketPeriode, Duration> tilsynsperioder = new MapTilsyn().map(input.getUtledetEtablertTilsyn());

        var unntakEtablertTilsynGrunnlag = unntakEtablertTilsynGrunnlagRepository.hent(behandling.getId());
        var beredskapsperioder = tilBeredskap(unntakEtablertTilsynGrunnlag);
        var nattevåksperioder = tilNattevåk(unntakEtablertTilsynGrunnlag);

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
            nattevåksperioder);
    }

    private Map<LukketPeriode, Utfall> tilBeredskap(UnntakEtablertTilsynGrunnlag grunnlag) {
        if (grunnlag == null || grunnlag.getUnntakEtablertTilsynForPleietrengende() == null || grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap() == null) {
            return Map.of();
        }
        return tilUnntakEtablertTilsynMap(grunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap());
    }

    private Map<LukketPeriode, Utfall> tilNattevåk(UnntakEtablertTilsynGrunnlag grunnlag) {
        if (grunnlag == null || grunnlag.getUnntakEtablertTilsynForPleietrengende() == null || grunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk() == null) {
            return Map.of();
        }
        return tilUnntakEtablertTilsynMap(grunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk());
    }

    private Map<LukketPeriode, Utfall> tilUnntakEtablertTilsynMap(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        var map = new HashMap<LukketPeriode, Utfall>();
        unntakEtablertTilsyn.getPerioder().forEach(periode -> {
                var utfall = switch(periode.getResultat()) {
                    case OPPFYLT -> Utfall.OPPFYLT;
                    case IKKE_OPPFYLT -> Utfall.IKKE_OPPFYLT;
                    case IKKE_VURDERT -> throw new IllegalStateException("Skal ikke komme perioder som ikke er vurdert til uttak.");
                };
                map.put(new LukketPeriode(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato()), utfall);
            }
        );
        return map;
    }

    private void evaluerDokumenter(Set<PerioderFraSøknad> perioderFraSøknader, Set<KravDokument> kravDokumenter) {
        var journalpostIds = perioderFraSøknader.stream().map(PerioderFraSøknad::getJournalpostId).collect(Collectors.toSet());

        var relevanteKravdokumenter = kravDokumenter.stream().map(KravDokument::getJournalpostId).filter(journalpostIds::contains).collect(Collectors.toSet());

        if (journalpostIds.size() != relevanteKravdokumenter.size()) {
            throw new IllegalStateException("Fant ikke alle dokumentene siden '" + journalpostIds + "' != '" + relevanteKravdokumenter + "'");
        }
    }

    private List<LocalDateSegment<Boolean>> mapPerioderTilVurdering(InputParametere input) {

        var timeline = new LocalDateTimeline<>(input.getPerioderTilVurdering()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));

        var utvidetePerioder = new LocalDateTimeline<>(input.getUtvidetRevurderingPerioder()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));
        timeline = timeline.combine(utvidetePerioder, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return new ArrayList<>(timeline.toSegments());
    }

    private Map<LukketPeriode, Pleiebehov> toPleiebehov(List<EtablertPleieperiode> pleiebehov) {
        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = new HashMap<>();
        pleiebehov.forEach(p -> {
            tilsynsbehov.put(toLukketPeriode(p.getPeriode()), mapToPleiebehov(p.getGrad()));
        });
        return tilsynsbehov;
    }

    private LukketPeriode toLukketPeriode(DatoIntervallEntitet periode) {
        return new LukketPeriode(periode.getFomDato(), periode.getTomDato());
    }

    private Pleiebehov mapToPleiebehov(Pleiegrad grad) {
        return switch (grad) {
            case INGEN -> Pleiebehov.PROSENT_0;
            case KONTINUERLIG_TILSYN -> Pleiebehov.PROSENT_100;
            case UTVIDET_KONTINUERLIG_TILSYN, INNLEGGELSE -> Pleiebehov.PROSENT_200;
            default -> throw new IllegalStateException("Ukjent Pleiegrad: " + grad);
        };
    }

    private HashMap<String, List<Vilkårsperiode>> toInngangsvilkår(Vilkårene vilkårene) {
        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = new HashMap<>();
        vilkårene.getVilkårene().forEach(v -> {
            if (v.getVilkårType() == VilkårType.BEREGNINGSGRUNNLAGVILKÅR) {
                return;
            }
            final List<Vilkårsperiode> vilkårsperioder = v.getPerioder()
                .stream()
                .map(vp -> new Vilkårsperiode(new LukketPeriode(vp.getFom(), vp.getTom()), mapUtfall(v.getVilkårType(), vp)))
                .collect(Collectors.toList());
            inngangsvilkår.put(v.getVilkårType().getKode(), vilkårsperioder);
        });
        return inngangsvilkår;
    }

    private Utfall mapUtfall(VilkårType vilkårType, VilkårPeriode vp) {
        if (Arrays.stream(Utfall.values()).noneMatch(it -> it.name().equals(vp.getGjeldendeUtfall().getKode()))) {
            throw new IllegalStateException("Vilkårsperiode med ikke supportert utfall '" + vp.getGjeldendeUtfall() + "', vilkår='" + vilkårType + "', periode='" + vp.getPeriode() + "'");
        }
        return Utfall.valueOf(vp.getGjeldendeUtfall().getKode());
    }

}
