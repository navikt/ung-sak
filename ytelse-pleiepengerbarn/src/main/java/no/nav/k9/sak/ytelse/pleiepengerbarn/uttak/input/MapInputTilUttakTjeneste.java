package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidstidMappingInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.InaktivitetUtlederInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.PerioderMedInaktivitetUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie.MapFerie;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn.MapTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak.MapUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov;
import no.nav.pleiepengerbarn.uttak.kontrakter.RettVedDød;
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
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private PleietrengendeKravprioritet pleietrengendeKravprioritet;
    private EtablertTilsynRepository etablertTilsynRepository;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;

    @Inject
    public MapInputTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                    UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    FagsakRepository fagsakRepository,
                                    PleietrengendeKravprioritet pleietrengendeKravprioritet,
                                    EtablertTilsynRepository etablertTilsynRepository,
                                    OpptjeningRepository opptjeningRepository,
                                    RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                    @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.pleietrengendeKravprioritet = pleietrengendeKravprioritet;
        this.etablertTilsynRepository = etablertTilsynRepository;
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.opptjeningRepository = opptjeningRepository;
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
        var opptjeningsresultat = opptjeningRepository.finnOpptjening(referanse.getBehandlingId());
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

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId());

        final List<EtablertTilsynPeriode> etablertTilsynPerioder = fjernInnleggelsesperioderFra(etablertTilsynRepository.hent(referanse.getBehandlingId()).getEtablertTilsyn().getPerioder(), pleiebehov);
        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(referanse.getFagsakId(), referanse.getPleietrengendeAktørId());
        var rettVedDød = rettPleiepengerVedDødRepository.hentHvisEksisterer(referanse.getBehandlingId());

        final NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(referanse.getBehandlingId());

        var input = new InputParametere()
            .medBehandling(behandling)
            .medVilkårene(vilkårene)
            .medPleiebehov(pleiebehov.map(pb -> pb.getPleieperioder().getPerioder()).orElse(List.of()))
            .medPerioderTilVurdering(perioderTilVurdering)
            .medUtvidetPerioderRevurdering(utvidetRevurderingPerioder)
            .medVurderteSøknadsperioder(vurderteSøknadsperioder)
            .medPerioderSomSkalTilbakestilles(perioderSomSkalTilbakestilles)
            .medPersonopplysninger(personopplysningerAggregat)
            .medRelaterteSaker(relaterteFagsaker)
            .medUttaksGrunnlag(uttakGrunnlag)
            .medEtablertTilsynPerioder(etablertTilsynPerioder)
            .medKravprioritet(kravprioritet)
            .medIAYGrunnlag(inntektArbeidYtelseGrunnlag)
            .medOpptjeningsresultat(opptjeningsresultat.orElse(null))
            .medRettPleiepengerVedDødGrunnlag(rettVedDød.orElse(null));

        return toRequestData(input);
    }

    private List<EtablertTilsynPeriode> fjernInnleggelsesperioderFra(List<EtablertTilsynPeriode> perioder, Optional<PleiebehovResultat> pleiebehov) {
        /*
         * TODO: Dette bør heller håndteres i pleiepenger-barn-uttak.
         */

        if (pleiebehov.isEmpty() || perioder.isEmpty() || pleiebehov.get().getPleieperioder().getPerioder().isEmpty()) {
            return perioder;
        }

        final LocalDateTimeline<EtablertTilsynPeriode> etablertTilsynTidslinje = new LocalDateTimeline<>(perioder.stream()
            .map(p -> new LocalDateSegment<>(
                p.getPeriode().getFomDato(),
                p.getPeriode().getTomDato(),
                p
            ))
            .collect(Collectors.toList()));
        final LocalDateTimeline<Boolean> innleggelseTidslinje = new LocalDateTimeline<>(pleiebehov.get()
            .getPleieperioder()
            .getPerioder()
            .stream()
            .filter(p -> p.getGrad() == Pleiegrad.INNLEGGELSE)
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), Boolean.TRUE))
            .collect(Collectors.toList()));


        return SykdomUtils.kunPerioderSomIkkeFinnesI(etablertTilsynTidslinje, innleggelseTidslinje)
            .stream()
            .map(s -> new EtablertTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getVarighet(), s.getValue().getJournalpostId()))
            .collect(Collectors.toList());
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

        RettVedDød rettVedDød = utledRettVedDød(input);
        var barn = new Barn(pleietrengendePersonopplysninger.getAktørId().getId(), pleietrengendePersonopplysninger.getDødsdato(), rettVedDød);

        var søker = new Søker(søkerPersonopplysninger.getAktørId().getId());

        final List<String> andrePartersSaksnummer = input.getRelaterteSaker()
            .stream()
            .map(Saksnummer::toString)
            .collect(Collectors.toList());

        var tidslinjeTilVurdering = new LocalDateTimeline<>(mapPerioderTilVurdering(input));

        final List<SøktUttak> søktUttak = new MapUttak().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering);

        var inaktivitetUtlederInput = new InaktivitetUtlederInput(behandling.getAktørId(), tidslinjeTilVurdering, input.getInntektArbeidYtelseGrunnlag());
        var inaktivTidslinje = new PerioderMedInaktivitetUtleder().utled(inaktivitetUtlederInput);

        var arbeidstidInput = new ArbeidstidMappingInput()
            .medKravDokumenter(kravDokumenter)
            .medPerioderFraSøknader(perioderFraSøknader)
            .medTidslinjeTilVurdering(tidslinjeTilVurdering)
            .medVilkår(input.getVilkårene().getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow())
            .medOpptjeningsResultat(input.getOpptjeningResultat().orElse(null))
            .medInaktivTidslinje(inaktivTidslinje);
        final List<Arbeid> arbeid = new MapArbeid().map(arbeidstidInput);

        final Map<LukketPeriode, Pleiebehov> pleiebehov = toPleiebehov(input.getPleiebehov());

        final List<LukketPeriode> lovbestemtFerie = new MapFerie().map(vurderteSøknadsperioder, perioderFraSøknader, tidslinjeTilVurdering);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(input.getVilkårene());

        final Map<LukketPeriode, Duration> tilsynsperioder = new MapTilsyn().map(input.getEtablertTilsynPerioder());

        var unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksisterer(behandling.getId())
            .map(UnntakEtablertTilsynGrunnlag::getUnntakEtablertTilsynForPleietrengende)
            .orElse(null);

        var innvilgedePerioderMedSykdom = finnInnvilgedePerioderSykdom(input.getVilkårene());

        var beredskapsperioder = tilBeredskap(unntakEtablertTilsynForPleietrengende, innvilgedePerioderMedSykdom);
        var nattevåksperioder = tilNattevåk(unntakEtablertTilsynForPleietrengende, innvilgedePerioderMedSykdom);
        final Map<LukketPeriode, List<String>> kravprioritet = mapKravprioritetsliste(input.getKravprioritet());
        final List<LukketPeriode> perioderSomSkalTilbakestilles = input.getPerioderSomSkalTilbakestilles().stream().map(p -> new LukketPeriode(p.getFomDato(), p.getTomDato())).toList();

        return new Uttaksgrunnlag(
            barn,
            søker,
            behandling.getFagsak().getSaksnummer().getVerdi(),
            behandling.getUuid().toString(),
            andrePartersSaksnummer,
            søktUttak,
            perioderSomSkalTilbakestilles,
            arbeid,
            pleiebehov,
            lovbestemtFerie,
            inngangsvilkår,
            tilsynsperioder,
            beredskapsperioder,
            nattevåksperioder,
            kravprioritet);
    }

    private Set<DatoIntervallEntitet> finnInnvilgedePerioderSykdom(Vilkårene vilkårene) {
        var s1 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow()
            .getPerioder()
            .stream();
        var s2 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow()
            .getPerioder()
            .stream();
        return Stream.concat(s1, s2)
            .filter(it -> no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT.equals(it.getUtfall()))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());
    }

    private RettVedDød utledRettVedDød(InputParametere input) {
        RettVedDød rettVedDød = null;
        if (input.getRettPleiepengerVedDødGrunnlag().isPresent()) {
            rettVedDød = switch (input.getRettPleiepengerVedDødGrunnlag().get().getRettVedPleietrengendeDød().getRettVedDødType()) {
                case RETT_6_UKER -> RettVedDød.RETT_6_UKER;
                case RETT_12_UKER -> RettVedDød.RETT_12_UKER;
            };
        }
        return rettVedDød;
    }

    public Map<LukketPeriode, List<String>> mapKravprioritetsliste(LocalDateTimeline<List<Kravprioritet>> kravprioritet) {
        final Map<LukketPeriode, List<String>> resultat = new HashMap<>();
        kravprioritet.forEach(s -> {
            resultat.put(new LukketPeriode(s.getFom(), s.getTom()), s.getValue().stream().map(kp -> kp.getSaksnummer().getVerdi()).collect(Collectors.toList()));
        });
        return resultat;
    }

    private Map<LukketPeriode, Utfall> tilBeredskap(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        if (unntakEtablertTilsynForPleietrengende == null || unntakEtablertTilsynForPleietrengende.getBeredskap() == null) {
            return Map.of();
        }
        return tilUnntakEtablertTilsynMap(unntakEtablertTilsynForPleietrengende.getBeredskap(), innvilgedePerioderMedSykdom);
    }

    private Map<LukketPeriode, Utfall> tilNattevåk(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        if (unntakEtablertTilsynForPleietrengende == null || unntakEtablertTilsynForPleietrengende.getNattevåk() == null) {
            return Map.of();
        }
        return tilUnntakEtablertTilsynMap(unntakEtablertTilsynForPleietrengende.getNattevåk(), innvilgedePerioderMedSykdom);
    }

    private Map<LukketPeriode, Utfall> tilUnntakEtablertTilsynMap(UnntakEtablertTilsyn unntakEtablertTilsyn, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        var map = new HashMap<LukketPeriode, Utfall>();
        unntakEtablertTilsyn.getPerioder()
            .stream()
            .filter(it -> erRelevantForBehandling(it, innvilgedePerioderMedSykdom))
            .forEach(periode -> {
                var utfall = switch (periode.getResultat()) {
                    case OPPFYLT -> Utfall.OPPFYLT;
                    case IKKE_OPPFYLT -> Utfall.IKKE_OPPFYLT;
                    case IKKE_VURDERT -> throw new IllegalStateException("Skal ikke komme perioder som ikke er vurdert til uttak.");
                };
                map.put(new LukketPeriode(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato()), utfall);
            }
        );
        return map;
    }

    private boolean erRelevantForBehandling(UnntakEtablertTilsynPeriode it, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        return Set.of(Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT).contains(it.getResultat())
            || innvilgedePerioderMedSykdom.stream().anyMatch(at -> at.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFomDato(), it.getPeriode().getTomDato())));
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

        return new ArrayList<>(timeline.compress().toSegments());
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
