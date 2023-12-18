package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.AktivitetstatusOgArbeidsgiver;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.TilkommetAktivitetTjeneste;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeMedVarighet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død.HåndterePleietrengendeDødsfallTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.AktivitetIdentifikator;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.HentPerioderTilVurderingTjeneste;

@Dependent
public class HentDataTilUttakTjeneste {

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;
    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private OpptjeningRepository opptjeningRepository;
    private PleietrengendeKravprioritet pleietrengendeKravprioritet;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private Instance<HåndterePleietrengendeDødsfallTjeneste> håndterePleietrengendeDødsfallTjenester;
    private HentPerioderTilVurderingTjeneste hentPerioderTilVurderingTjeneste;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private HentEtablertTilsynTjeneste hentEtablertTilsynTjeneste;
    private TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste;
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private OverstyrUttakRepository overstyrUttakRepository;

    private boolean tilkommetAktivitetEnabled;
    private boolean nyRegelEnabled;

    @Inject
    public HentDataTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste,
                                    UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    FagsakRepository fagsakRepository,
                                    PleietrengendeKravprioritet pleietrengendeKravprioritet,
                                    OpptjeningRepository opptjeningRepository,
                                    RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    @Any PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                    @Any Instance<HåndterePleietrengendeDødsfallTjeneste> håndterePleietrengendeDødsfallTjenester,
                                    HentPerioderTilVurderingTjeneste hentPerioderTilVurderingTjeneste,
                                    UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                    HentEtablertTilsynTjeneste hentEtablertTilsynTjeneste,
                                    TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste,
                                    UttakNyeReglerRepository uttakNyeReglerRepository,
                                    OverstyrUttakRepository overstyrUttakRepository,
                                    @KonfigVerdi(value = "TILKOMMET_AKTIVITET_ENABLED", required = false, defaultVerdi = "false") boolean tilkommetAktivitetEnabled,
                                    @KonfigVerdi(value = "ENABLE_DATO_NY_REGEL_UTTAK", required = false, defaultVerdi = "false") boolean nyRegelEnabled

    ) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.pleietrengendeKravprioritet = pleietrengendeKravprioritet;
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.håndterePleietrengendeDødsfallTjenester = håndterePleietrengendeDødsfallTjenester;
        this.hentPerioderTilVurderingTjeneste = hentPerioderTilVurderingTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.hentEtablertTilsynTjeneste = hentEtablertTilsynTjeneste;
        this.tilkommetAktivitetTjeneste = tilkommetAktivitetTjeneste;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.tilkommetAktivitetEnabled = tilkommetAktivitetEnabled;
        this.nyRegelEnabled = nyRegelEnabled;
    }

    public InputParametere hentUtData(BehandlingReferanse referanse, boolean brukUbesluttedeData, boolean medInntektsgradering) {
        boolean skalMappeHeleTidslinjen = brukUbesluttedeData;

        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleiebehov = pleiebehovResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());

        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = perioderTilVurderingTjeneste(referanse);
        final NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(referanse.getBehandlingId());
        HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste = HåndterePleietrengendeDødsfallTjeneste.velgTjeneste(håndterePleietrengendeDødsfallTjenester, referanse);
        var utvidetPeriodeSomFølgeAvDødsfall = håndterePleietrengendeDødsfallTjeneste.utledUtvidetPeriodeForDødsfall(referanse);
        final NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
        if (skalMappeHeleTidslinjen) {
            perioderTilVurdering = hentPerioderTilVurderingTjeneste.hentPerioderTilVurderingMedUbesluttet(behandling, utvidetPeriodeSomFølgeAvDødsfall);
        } else {
            perioderTilVurdering = hentPerioderTilVurderingTjeneste.hentPerioderTilVurderingUtenUbesluttet(behandling);
        }

        LocalDate virkningsdatoNyeRegler = uttakNyeReglerRepository.finnDatoForNyeRegler(referanse.getBehandlingId()).orElse(null);
        if (virkningsdatoNyeRegler != null && !nyRegelEnabled) {
            throw new IllegalStateException("Har lagret virkningsdato for nye regler i uttak, men de nye reglene er skrudd av.");
        }

        final Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> tilkommetAktivitetsperioder;
        if (tilkommetAktivitetEnabled) {
            final Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> tilkommedeAktiviteterRaw = tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(referanse.getFagsakId(), virkningsdatoNyeRegler);
            tilkommetAktivitetsperioder = tilkommedeAktiviteterRaw.entrySet().stream()
                .collect(Collectors.toMap(e -> new AktivitetIdentifikator(e.getKey().getAktivitetType(), e.getKey().getArbeidsgiver(), null), e -> e.getValue()));
        } else {
            tilkommetAktivitetsperioder = new HashMap<>();
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

        final List<PeriodeMedVarighet> etablertTilsynPerioder = hentEtablertTilsynTjeneste.hentOgSmørEtablertTilsynPerioder(referanse, unntakEtablertTilsynForPleietrengende, brukUbesluttedeData);

        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(referanse.getFagsakId(), referanse.getPleietrengendeAktørId(), brukUbesluttedeData);
        var rettVedDød = rettPleiepengerVedDødRepository.hentHvisEksisterer(referanse.getBehandlingId());

        var perioderFraSøknad = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(referanse);

        var utsattePerioder = utsattBehandlingAvPeriodeRepository.hentGrunnlag(referanse.getBehandlingId());

        var kravprioritetsBehandlinger = kravprioritet.stream().map(LocalDateSegment::getValue).flatMap(Collection::stream).map(Kravprioritet::getAktuellBehandlingUuid).collect(Collectors.toSet());
        Map<UUID, UUID> sisteVedtatteBehandlinger = new HashMap<>();
        for (UUID uuid : kravprioritetsBehandlinger) {
            var b = behandlingRepository.hentBehandling(uuid);
            if (b.erSaksbehandlingAvsluttet()) {
                sisteVedtatteBehandlinger.put(uuid, uuid);
            } else {
                sisteVedtatteBehandlinger.put(uuid, b.getOriginalBehandlingId().map(it -> behandlingRepository.hentBehandling(it)).map(Behandling::getUuid).orElse(null));
            }
        }
        LocalDateTimeline<OverstyrtUttakPeriode> overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());


        LocalDateTimeline<BigDecimal> nedjustertUttakgradTidslinje;

        if (tilkommetAktivitetEnabled && medInntektsgradering) {
            nedjustertUttakgradTidslinje = tilkommetAktivitetTjeneste.finnInntektsgradering(referanse.getFagsakId());
        } else {
            nedjustertUttakgradTidslinje = LocalDateTimeline.empty();
        }


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
            .medUtsattePerioder(utsattePerioder.orElse(null))
            .medSisteVedtatteBehandlingForBehandling(sisteVedtatteBehandlinger)
            .medTilkommetAktivitetsperioder(tilkommetAktivitetsperioder)
            .medVirkningsdatoNyeRegler(virkningsdatoNyeRegler)
            .medOverstyrtUttak(overstyrtUttak)
            .medNedjustertUttaksgrad(nedjustertUttakgradTidslinje)
            ;
    }

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
    }
}
