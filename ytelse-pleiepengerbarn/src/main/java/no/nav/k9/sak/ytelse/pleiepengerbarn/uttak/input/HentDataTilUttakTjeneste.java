package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

@Dependent
public class HentDataTilUttakTjeneste {

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;
    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private OpptjeningRepository opptjeningRepository;
    private PleietrengendeKravprioritet pleietrengendeKravprioritet;
    private EtablertTilsynRepository etablertTilsynRepository;
    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    @Inject
    public HentDataTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste,
                                    UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    FagsakRepository fagsakRepository,
                                    PleietrengendeKravprioritet pleietrengendeKravprioritet,
                                    EtablertTilsynRepository etablertTilsynRepository,
                                    EtablertTilsynTjeneste etablertTilsynTjeneste,
                                    OpptjeningRepository opptjeningRepository,
                                    RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    @FagsakYtelseTypeRef("PSB") @FagsakYtelseTypeRef("PPN") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                    SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.pleietrengendeKravprioritet = pleietrengendeKravprioritet;
        this.etablertTilsynRepository = etablertTilsynRepository;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    public InputParametere hentUtData(BehandlingReferanse referanse, boolean brukUbesluttedeData) {
        boolean skalMappeHeleTidslinjen = brukUbesluttedeData;

        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleiebehov = pleiebehovResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());

        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = perioderTilVurderingTjeneste(referanse);
        final NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(referanse.getBehandlingId());
        final NavigableSet<DatoIntervallEntitet> perioderTilVurdering = hentPerioderTilVurdering(referanse, skalMappeHeleTidslinjen, behandling);

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

        final List<EtablertTilsynPeriode> etablertTilsynPerioder;
        if (brukUbesluttedeData) {
            etablertTilsynPerioder = fjernInnleggelsesperioderFra(etablertTilsynTjeneste.utledGrunnlagForTilsynstidlinje(referanse).getPerioder(), pleiebehov);
        } else {
            etablertTilsynPerioder = fjernInnleggelsesperioderFra(etablertTilsynRepository.hentHvisEksisterer(referanse.getBehandlingId())
                .map(EtablertTilsynGrunnlag::getEtablertTilsyn)
                .map(EtablertTilsyn::getPerioder)
                .orElse(List.of()), pleiebehov);
        }
        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(referanse.getFagsakId(), referanse.getPleietrengendeAktørId(), brukUbesluttedeData);
        var rettVedDød = rettPleiepengerVedDødRepository.hentHvisEksisterer(referanse.getBehandlingId());

        var unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksisterer(behandling.getId())
            .map(UnntakEtablertTilsynGrunnlag::getUnntakEtablertTilsynForPleietrengende);
        var perioderFraSøknad = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(referanse);

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
            .medPerioderFraSøknad(perioderFraSøknad)
            .medEtablertTilsynPerioder(etablertTilsynPerioder)
            .medKravprioritet(kravprioritet)
            .medIAYGrunnlag(inntektArbeidYtelseGrunnlag)
            .medOpptjeningsresultat(opptjeningsresultat.orElse(null))
            .medRettPleiepengerVedDødGrunnlag(rettVedDød.orElse(null))
            .medUnntakEtablertTilsynForPleietrengende(unntakEtablertTilsynForPleietrengende.orElse(null));

        return input;
    }

    private NavigableSet<DatoIntervallEntitet> hentPerioderTilVurdering(BehandlingReferanse referanse, boolean skalMappeHeleTidslinjen, Behandling behandling) {
        LocalDateTimeline<Boolean> søknadsperioder;
        if (skalMappeHeleTidslinjen) {
            søknadsperioder = SykdomUtils.toLocalDateTimeline(søknadsperiodeTjeneste.utledFullstendigPeriode(behandling.getId()));
        } else {
            søknadsperioder = SykdomUtils.toLocalDateTimeline(finnSykdomsperioder(referanse));
        }

        final LocalDateTimeline<Boolean> trukkedeKrav = hentTrukkedeKravTidslinje(referanse, behandling);
        return SykdomUtils.kunPerioderSomIkkeFinnesI(søknadsperioder, trukkedeKrav).stream()
            .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> hentTrukkedeKravTidslinje(BehandlingReferanse referanse, Behandling behandling) {
        return SykdomUtils.toLocalDateTimeline(søknadsperiodeTjeneste.hentKravperioder(behandling.getFagsakId(), referanse.getBehandlingId())
            .stream()
            .filter(kp -> kp.isHarTrukketKrav())
            .map(SøknadsperiodeTjeneste.Kravperiode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new)));
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
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = perioderTilVurderingTjeneste(referanse);
        final var s1 = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        final var s2 = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        final var s3 = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.I_LIVETS_SLUTTFASE);
        final var resultat = new TreeSet<>(s1);
        resultat.addAll(s2);
        resultat.addAll(s3);
        return resultat;
    }

    public Map<LukketPeriode, List<String>> deprecatedMapKravprioritetsliste(LocalDateTimeline<List<Kravprioritet>> kravprioritet) {
        final Map<LukketPeriode, List<String>> resultat = new HashMap<>();
        kravprioritet.forEach(s -> {
            resultat.put(new LukketPeriode(s.getFom(), s.getTom()), s.getValue().stream().map(kp -> kp.getSaksnummer().getVerdi()).collect(Collectors.toList()));
        });
        return resultat;
    }

    public Map<LukketPeriode, List<String>> mapKravprioritetsliste(LocalDateTimeline<List<Kravprioritet>> kravprioritet) {
        final Map<LukketPeriode, List<String>> resultat = new HashMap<>();
        kravprioritet.forEach(s -> {
            resultat.put(new LukketPeriode(s.getFom(), s.getTom()), s.getValue().stream().map(kp -> kp.getAktuellBehandlingUuid().toString()).collect(Collectors.toList()));
        });
        return resultat;
    }

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste(BehandlingReferanse behandlingReferanse){
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
    }
}
