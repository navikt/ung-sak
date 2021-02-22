package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
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

    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;

    @Inject
    public MapInputTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var uttakGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleiebehov = pleiebehovResultatRepository.hent(referanse.getBehandlingId());
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(referanse);

        return toRequestData(behandling, personopplysningerAggregat, vurderteSøknadsperioder, vilkårene, uttakGrunnlag, pleiebehov);
    }

    private Uttaksgrunnlag toRequestData(Behandling behandling,
                                         PersonopplysningerAggregat personopplysningerAggregat,
                                         Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder,
                                         Vilkårene vilkårene,
                                         UttaksPerioderGrunnlag uttaksPerioderGrunnlag,
                                         PleiebehovResultat pleiebehov) {

        var perioderFraSøknader = uttaksPerioderGrunnlag.getRelevantSøknadsperioder().getUttakPerioder();
        var kravDokumenter = vurderteSøknadsperioder.keySet()
            .stream()
            .filter(it -> perioderFraSøknader.stream().anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())))
            .collect(Collectors.toCollection(TreeSet::new));
        var søkerPersonopplysninger = personopplysningerAggregat.getSøker();
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());

        var barn = new Barn(pleietrengendePersonopplysninger.getAktørId().getId(), pleietrengendePersonopplysninger.getDødsdato());
        var søker = new Søker(søkerPersonopplysninger.getAktørId().getId(), søkerPersonopplysninger.getFødselsdato(), søkerPersonopplysninger.getDødsdato());

        // TODO: Map:
        final List<String> andrePartersSaksnummer = List.of();

        final List<SøktUttak> søktUttak = mapUttakPerioder(kravDokumenter, perioderFraSøknader);

        // TODO: Se kommentarer/TODOs under denne:
        final List<Arbeid> arbeid = mapArbeid(kravDokumenter, perioderFraSøknader);

        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = toTilsynsbehov(pleiebehov);

        final List<LukketPeriode> lovbestemtFerie = mapFerie(kravDokumenter, perioderFraSøknader);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(vilkårene);
        
        final Map<LukketPeriode, Duration> tilsynsperioder = mapTilsynsperioder(kravDokumenter, perioderFraSøknader);

        return new Uttaksgrunnlag(
            barn,
            søker,
            behandling.getFagsak().getSaksnummer().getVerdi(),
            behandling.getUuid().toString(),
            andrePartersSaksnummer,
            søktUttak,
            arbeid,
            tilsynsbehov,
            lovbestemtFerie,
            inngangsvilkår,
            tilsynsperioder);
    }

    private Map<LukketPeriode, Duration> mapTilsynsperioder(TreeSet<KravDokument> kravDokumenter, Set<PerioderFraSøknad> perioderFraSøknader) {
        Map<LukketPeriode, Duration> result = new HashMap<>();

        kravDokumenter.stream()
            .sorted()
            .forEachOrdered(at -> {
                var dokumenter = perioderFraSøknader.stream()
                    .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
                    .collect(Collectors.toSet());
                if (dokumenter.size() == 1) {
                    // TODO: toTimeline and to map
                    dokumenter.stream()
                        .map(PerioderFraSøknad::getTilsynsordning)
                        .flatMap(Collection::stream)
                        .map(Tilsynsordning::getPerioder)
                        .flatMap(Collection::stream)
                        .forEach(it -> result.put(toLukketPeriode(it.getPeriode()), it.getVarighet()));
                } else {
                    throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
                }
            });

        return result;
    }

    private List<LukketPeriode> mapFerie(Set<KravDokument> kravDokumenter, Set<PerioderFraSøknad> perioderFraSøknader) {
        var result = new ArrayList<LukketPeriode>();

        kravDokumenter.stream()
            .sorted()
            .forEachOrdered(at -> {
                var dokumenter = perioderFraSøknader.stream()
                    .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
                    .collect(Collectors.toSet());
                if (dokumenter.size() == 1) {
                    // TODO: toTimeline and to map
                    result.addAll(dokumenter.stream()
                        .map(PerioderFraSøknad::getFerie)
                        .flatMap(Collection::stream)
                        .map(it -> toLukketPeriode(it.getPeriode()))
                        .collect(Collectors.toList()));
                } else {
                    throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
                }
            });

        return result;
    }

    private List<SøktUttak> mapUttakPerioder(Set<KravDokument> kravDokumenter,
                                             Set<PerioderFraSøknad> perioderFraSøknader) {

        var result = new ArrayList<SøktUttak>();

        kravDokumenter.stream()
            .sorted()
            .forEachOrdered(at -> {
                var dokumenter = perioderFraSøknader.stream()
                    .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
                    .collect(Collectors.toSet());
                if (dokumenter.size() == 1) {
                    result.addAll(dokumenter.stream()
                        .map(PerioderFraSøknad::getUttakPerioder)
                        .flatMap(Collection::stream)
                        .map(it -> new SøktUttak(toLukketPeriode(it.getPeriode()), it.getTimerPleieAvBarnetPerDag()))
                        .collect(Collectors.toList()));
                } else {
                    throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
                }
            });

        return result;
    }

    private Map<LukketPeriode, Pleiebehov> toTilsynsbehov(PleiebehovResultat pleiebehov) {
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

    private List<Arbeid> mapArbeid(Set<KravDokument> kravDokumenter,
                                   Set<PerioderFraSøknad> perioderFraSøknader) {
        final Map<ArbeidsgiverArbeidsforhold, List<ArbeidPeriode>> arbeidsforhold = new HashMap<>();

        kravDokumenter.stream()
            .sorted()
            .forEachOrdered(at -> {
                var dokumenter = perioderFraSøknader.stream()
                    .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
                    .collect(Collectors.toSet());
                if (dokumenter.size() == 1) {
                    dokumenter.stream()
                        .map(PerioderFraSøknad::getArbeidPerioder)
                        .flatMap(Collection::stream)
                        .forEach(p -> {
                            var key = new ArbeidsgiverArbeidsforhold(p.getArbeidsgiver(), p.getArbeidsforholdRef());
                            var perioder = arbeidsforhold.getOrDefault(key, new ArrayList<>());
                            perioder.add(p);
                            arbeidsforhold.put(key, perioder);
                        });
                } else {
                    throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
                }
            });

        return arbeidsforhold.values()
            .stream()
            .map(arbeidPeriodes -> {
                var uttakAktivitetPeriode = arbeidPeriodes.get(0);
                var perioder = new HashMap<LukketPeriode, ArbeidsforholdPeriodeInfo>();
                arbeidPeriodes.forEach(p -> {
                    var jobberNormalt = Optional.ofNullable(p.getJobberNormaltTimerPerDag()).orElse(Duration.ZERO);
                    var jobberFaktisk = Optional.ofNullable(p.getFaktiskArbeidTimerPerDag()).orElse(Duration.ZERO);
                    perioder.put(new LukketPeriode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()),
                        new ArbeidsforholdPeriodeInfo(jobberNormalt, jobberFaktisk));
                });

                return new Arbeid(mapArbeidsforhold(uttakAktivitetPeriode), perioder);
            })
            .collect(Collectors.toList());
    }

    private Arbeidsforhold mapArbeidsforhold(ArbeidPeriode uttakAktivitetPeriode) {
        return new Arbeidsforhold(uttakAktivitetPeriode.getAktivitetType().getKode(),
            uttakAktivitetPeriode.getArbeidsgiver().getArbeidsgiverOrgnr(),
            Optional.ofNullable(uttakAktivitetPeriode.getArbeidsgiver().getArbeidsgiverAktørId()).map(AktørId::getId).orElse(null),
            Optional.ofNullable(uttakAktivitetPeriode.getArbeidsforholdRef()).map(InternArbeidsforholdRef::getReferanse).orElse(null)
        );
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

    static class ArbeidsgiverArbeidsforhold {

        private Arbeidsgiver arbeidsgiver;
        private InternArbeidsforholdRef arbeidsforhold;

        public ArbeidsgiverArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforhold) {
            this.arbeidsgiver = Objects.requireNonNull(arbeidsgiver);
            this.arbeidsforhold = arbeidsforhold;
        }

        public Arbeidsgiver getArbeidsgiver() {
            return arbeidsgiver;
        }

        public InternArbeidsforholdRef getArbeidsforhold() {
            return arbeidsforhold;
        }

        public boolean identifisererSamme(ArbeidsgiverArbeidsforhold arbeidsforhold) {
            if (!arbeidsgiver.equals(arbeidsforhold.getArbeidsgiver())) {
                return false;
            }

            return this.arbeidsforhold.gjelderFor(arbeidsforhold.getArbeidsforhold());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArbeidsgiverArbeidsforhold that = (ArbeidsgiverArbeidsforhold) o;
            return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
                Objects.equals(arbeidsforhold, that.arbeidsforhold);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arbeidsgiver, arbeidsforhold);
        }
    }
}
