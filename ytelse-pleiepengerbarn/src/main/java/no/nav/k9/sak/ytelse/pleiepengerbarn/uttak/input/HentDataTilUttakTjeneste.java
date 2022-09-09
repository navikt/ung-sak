package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynSmører;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeMedVarighet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død.HåndterePleietrengendeDødsfallTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.HentPerioderTilVurderingTjeneste;

@Dependent
public class HentDataTilUttakTjeneste {

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private SykdomVurderingTjeneste sykdomVurderingTjeneste;
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;
    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private OpptjeningRepository opptjeningRepository;
    private PleietrengendeKravprioritet pleietrengendeKravprioritet;
    private EtablertTilsynRepository etablertTilsynRepository;
    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste;
    private HentPerioderTilVurderingTjeneste hentPerioderTilVurderingTjeneste;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private LocalDate ukesmøringOmsorgstilbudFomDato;

    @Inject
    public HentDataTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    SykdomVurderingTjeneste sykdomVurderingTjeneste,
                                    PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository,
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
                                    @Any PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                    HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste,
                                    HentPerioderTilVurderingTjeneste hentPerioderTilVurderingTjeneste,
                                    UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                    @KonfigVerdi(value = "UKESMOERING_OMSORGSTILBUD_FOM_DATO", defaultVerdi = "", required = false) String ukesmøringOmsorgstilbudFomDatoString) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.sykdomVurderingTjeneste = sykdomVurderingTjeneste;
        this.pleietrengendeSykdomDokumentRepository = pleietrengendeSykdomDokumentRepository;
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
        this.håndterePleietrengendeDødsfallTjeneste = håndterePleietrengendeDødsfallTjeneste;
        this.hentPerioderTilVurderingTjeneste = hentPerioderTilVurderingTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        
        if (ukesmøringOmsorgstilbudFomDatoString == null || ukesmøringOmsorgstilbudFomDatoString.trim().isEmpty()) {
            this.ukesmøringOmsorgstilbudFomDato = null;
        } else {
            this.ukesmøringOmsorgstilbudFomDato = LocalDate.parse(ukesmøringOmsorgstilbudFomDatoString);
        }
    }

    public InputParametere hentUtData(BehandlingReferanse referanse, boolean brukUbesluttedeData) {
        boolean skalMappeHeleTidslinjen = brukUbesluttedeData;

        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleiebehov = pleiebehovResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());

        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = perioderTilVurderingTjeneste(referanse);
        final NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(referanse.getBehandlingId());
        var utvidetPeriodeSomFølgeAvDødsfall = håndterePleietrengendeDødsfallTjeneste.utledUtvidetPeriodeForDødsfall(referanse);
        final NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
        if (skalMappeHeleTidslinjen) {
            perioderTilVurdering = hentPerioderTilVurderingTjeneste.hentPerioderTilVurderingMedUbesluttet(behandling, utvidetPeriodeSomFølgeAvDødsfall);
        } else {
            perioderTilVurdering = hentPerioderTilVurderingTjeneste.hentPerioderTilVurderingUtenUbesluttet(behandling);
        }

        var utvidetRevurderingPerioder = perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(referanse);
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(referanse);
        var opptjeningsresultat = opptjeningRepository.finnOpptjening(referanse.getBehandlingId());
        var fagsak = behandling.getFagsak();
        var fagsakPeriode = fagsak.getPeriode();
        Set<Saksnummer> relaterteFagsaker = Objects.equals(fagsak.getYtelseType(), FagsakYtelseType.OPPLÆRINGSPENGER) ? Set.of() : fagsakRepository.finnFagsakRelatertTil(behandling.getFagsakYtelseType(),
                fagsak.getPleietrengendeAktørId(),
                null,
                fagsakPeriode.getFomDato().minusWeeks(25),
                fagsakPeriode.getTomDato().plusWeeks(25))
            .stream().map(Fagsak::getSaksnummer)
            .filter(it -> !fagsak.getSaksnummer().equals(it))
            .collect(Collectors.toSet());

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId());

        var unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksisterer(behandling.getId())
                .map(UnntakEtablertTilsynGrunnlag::getUnntakEtablertTilsynForPleietrengende);
        
        final List<PeriodeMedVarighet> etablertTilsynPerioder = hentOgSmørEtablertTilsynPerioder(referanse, brukUbesluttedeData, unntakEtablertTilsynForPleietrengende);        
        
        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(referanse.getFagsakId(), referanse.getPleietrengendeAktørId(), brukUbesluttedeData);
        var rettVedDød = rettPleiepengerVedDødRepository.hentHvisEksisterer(referanse.getBehandlingId());

        var perioderFraSøknad = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(referanse);

        var utsattePerioder = utsattBehandlingAvPeriodeRepository.hentGrunnlag(referanse.getBehandlingId());

        return new InputParametere()
            .medBehandling(behandling)
            .medVilkårene(vilkårene)
            .medDefinerendeVilkår(perioderTilVurderingTjeneste.definerendeVilkår())
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
            .medAutomatiskUtvidelseVedDødsfall(utvidetPeriodeSomFølgeAvDødsfall.orElse(null))
            .medUnntakEtablertTilsynForPleietrengende(unntakEtablertTilsynForPleietrengende.orElse(null))
            .medUtsattePerioder(utsattePerioder.orElse(null));
    }

    private List<PeriodeMedVarighet> hentOgSmørEtablertTilsynPerioder(BehandlingReferanse referanse,
            boolean brukUbesluttedeData,
            Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende) {

        final List<EtablertTilsynPeriode> etablertTilsynPerioderFraGrunnlag = hentEtablertTilsynForPleietrengendeUtenInnleggelse(referanse, brukUbesluttedeData);
        final List<PeriodeMedVarighet> ikkeSmurteEtablertTilsynPerioder = etablertTilsynPerioderFraGrunnlag.stream()
                .map(p -> new PeriodeMedVarighet(p.getPeriode(), p.getVarighet()))
                .collect(Collectors.toList());
        
        if (ukesmøringOmsorgstilbudFomDato == null) {
            return ikkeSmurteEtablertTilsynPerioder;
        }
        
        final List<PeriodeMedVarighet> smurteEtablertTilsynPerioder = smørEtablertTilsyn(referanse, unntakEtablertTilsynForPleietrengende, etablertTilsynPerioderFraGrunnlag);
        final LocalDateTimeline<PeriodeMedVarighet> etablertTilsynPerioderTidslinje = håndterSmøringFraFeatureToggleDato(ikkeSmurteEtablertTilsynPerioder, smurteEtablertTilsynPerioder);
        
        return toPeriodeMedVarighetList(etablertTilsynPerioderTidslinje);
    }

    private LocalDateTimeline<PeriodeMedVarighet> håndterSmøringFraFeatureToggleDato(
            final List<PeriodeMedVarighet> ikkeSmurteEtablertTilsynPerioder,
            final List<PeriodeMedVarighet> smurteEtablertTilsynPerioder) {
        
        final LocalDateTimeline<PeriodeMedVarighet> ikkeSmurtTidslinje = toPeriodeMedVarighetTidslinje(ikkeSmurteEtablertTilsynPerioder);
        LocalDateTimeline<PeriodeMedVarighet> smurtTidslinje = toPeriodeMedVarighetTidslinje(smurteEtablertTilsynPerioder);
        smurtTidslinje = smurtTidslinje.intersection(DatoIntervallEntitet.fraOgMed(ukesmøringOmsorgstilbudFomDato).toLocalDateInterval());
        
        return ikkeSmurtTidslinje.union(smurtTidslinje, StandardCombinators::coalesceRightHandSide);
    }

    private List<PeriodeMedVarighet> smørEtablertTilsyn(BehandlingReferanse referanse,
            Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende,
            final List<EtablertTilsynPeriode> etablertTilsynPerioderFraGrunnlag) {
        final List<PeriodeMedVarighet> smurteEtablertTilsynPerioder;
        final LocalDateTimeline<Boolean> sykdomsperioderPåPleietrengende = sykdomVurderingTjeneste.hentPsbOppfyltePerioderPåPleietrengende(referanse.getPleietrengendeAktørId());
        if (ukesmøringOmsorgstilbudFomDato != null && !etablertTilsynPerioderFraGrunnlag.isEmpty() && !sykdomsperioderPåPleietrengende.isEmpty()) {
            smurteEtablertTilsynPerioder = EtablertTilsynSmører.smørEtablertTilsyn(sykdomsperioderPåPleietrengende, etablertTilsynPerioderFraGrunnlag, unntakEtablertTilsynForPleietrengende);
        } else {
            smurteEtablertTilsynPerioder = List.of();
        }
        return smurteEtablertTilsynPerioder;
    }

    private List<PeriodeMedVarighet> toPeriodeMedVarighetList(
            final LocalDateTimeline<PeriodeMedVarighet> etablertTilsynPerioderTidslinje) {
        return etablertTilsynPerioderTidslinje.stream().map(s -> new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getVarighet())).collect(Collectors.toList());
    }

    private LocalDateTimeline<PeriodeMedVarighet> toPeriodeMedVarighetTidslinje(
            final List<PeriodeMedVarighet> ikkeSmurteEtablertTilsynPerioder) {
        return new LocalDateTimeline<>(
            ikkeSmurteEtablertTilsynPerioder.stream().map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p)).collect(Collectors.toList())
        );
    }

    private List<EtablertTilsynPeriode> hentEtablertTilsynForPleietrengendeUtenInnleggelse(BehandlingReferanse referanse, boolean brukUbesluttedeData) {
        List<EtablertTilsynPeriode> etablertTilsynPerioderFraGrunnlag;
        final List<PleietrengendeSykdomInnleggelsePeriode> innleggelser = pleietrengendeSykdomDokumentRepository.hentInnleggelse(referanse.getPleietrengendeAktørId()).getPerioder();
        if (brukUbesluttedeData) {
            etablertTilsynPerioderFraGrunnlag = fjernInnleggelsesperioderFra(etablertTilsynTjeneste.utledGrunnlagForTilsynstidlinje(referanse).getPerioder(), innleggelser);
        } else {
            etablertTilsynPerioderFraGrunnlag = fjernInnleggelsesperioderFra(etablertTilsynRepository.hentHvisEksisterer(referanse.getBehandlingId())
                .map(EtablertTilsynGrunnlag::getEtablertTilsyn)
                .map(EtablertTilsyn::getPerioder)
                .orElse(List.of()), innleggelser);
        }
        return etablertTilsynPerioderFraGrunnlag;
    }

    private List<EtablertTilsynPeriode> fjernInnleggelsesperioderFra(List<EtablertTilsynPeriode> perioder, List<PleietrengendeSykdomInnleggelsePeriode> innleggelser) {
        if (innleggelser.isEmpty() || perioder.isEmpty()) {
            return perioder;
        }

        final LocalDateTimeline<EtablertTilsynPeriode> etablertTilsynTidslinje = new LocalDateTimeline<>(perioder.stream()
            .map(p -> new LocalDateSegment<>(
                p.getPeriode().getFomDato(),
                p.getPeriode().getTomDato(),
                p
            ))
            .collect(Collectors.toList()));
        
        final LocalDateTimeline<Boolean> innleggelseTidslinje = new LocalDateTimeline<>(innleggelser.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .collect(Collectors.toList()));

        return TidslinjeUtil.kunPerioderSomIkkeFinnesI(etablertTilsynTidslinje, innleggelseTidslinje)
            .stream()
            .map(s -> new EtablertTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getVarighet(), s.getValue().getJournalpostId()))
            .collect(Collectors.toList());
    }

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
    }
}
