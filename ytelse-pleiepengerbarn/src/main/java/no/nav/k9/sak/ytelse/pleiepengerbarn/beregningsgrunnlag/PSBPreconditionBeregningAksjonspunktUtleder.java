package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.PreconditionBeregningAksjonspunktUtleder;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening.PSBOppgittOpptjeningFilter;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBPreconditionBeregningAksjonspunktUtleder implements PreconditionBeregningAksjonspunktUtleder {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private PSBOpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private FagsakRepository fagsakRepository;
    private PSBOppgittOpptjeningFilter oppgittOpptjeningFilter;


    private boolean toggleMigrering;
    private PåTversAvHelgErKantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();


    public PSBPreconditionBeregningAksjonspunktUtleder() {
    }

    @Inject
    public PSBPreconditionBeregningAksjonspunktUtleder(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       @FagsakYtelseTypeRef("PSB") PSBOpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                                       @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                       FagsakRepository fagsakRepository,
                                                       @FagsakYtelseTypeRef("PSB") PSBOppgittOpptjeningFilter oppgittOpptjeningFilter,
                                                       @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering) {
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.oppgittOpptjeningFilter = oppgittOpptjeningFilter;
        this.toggleMigrering = toggleMigrering;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (!toggleMigrering) {
            return Collections.emptyList();
        }
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        if (perioderTilVurdering.isEmpty()) {
            return Collections.emptyList();
        }

        var eksisterendeInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(param.getRef().getFagsakId());
        var eksisterendeMigreringTilVurdering = finnEksisterendeMigreringTilVurdering(perioderTilVurdering, eksisterendeInfotrygdMigreringer);
        var psbInfotrygdFilter = finnPSBInfotryd(param);
        List<YtelseAnvist> anvistePeriodeSomManglerSøknad = finnAnvistePerioderFraInfotrygdUtenSøknad(param, perioderTilVurdering, psbInfotrygdFilter);

        if (!anvistePeriodeSomManglerSøknad.isEmpty()) {
            return List.of(ventepunkt(Venteårsak.MANGLER_SØKNAD_FOR_PERIODER_I_INFOTRYGD));
        }

        if (!eksisterendeMigreringTilVurdering.isEmpty()) {
            var harNæringUtenSøknad = harNæringIInfotrygdOgManglerSøknad(
                perioderTilVurdering,
                eksisterendeMigreringTilVurdering,
                psbInfotrygdFilter, param.getBehandlingId());
            var harFrilansUtenSøknad = harFrilansIInfotrygdOgManglerSøknad(
                perioderTilVurdering,
                eksisterendeMigreringTilVurdering,
                psbInfotrygdFilter, param.getBehandlingId());
            if (harNæringUtenSøknad || harFrilansUtenSøknad) {
                return List.of(ventepunkt(harNæringUtenSøknad ? Venteårsak.MANGLER_SØKNADOPPLYSNING_NÆRING : Venteårsak.MANGLER_SØKNADOPPLYSNING_FRILANS));
            }
            var harKunDagpenger = harBrukerKunPleiepengerAvDagpenger(param, perioderTilVurdering, eksisterendeMigreringTilVurdering);
            if (harKunDagpenger) {
                return Collections.emptyList();
            }
            return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT));
        }

        return Collections.emptyList();
    }

    private AksjonspunktResultat ventepunkt(Venteårsak venteårsak) {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(
            AksjonspunktDefinisjon.AUTO_VENT_PÅ_KOMPLETT_SØKNAD_VED_OVERGANG_FRA_INFOTRYGD,
            venteårsak,
            LocalDateTime.now().plusMonths(3));
    }

    private boolean harNæringIInfotrygdOgManglerSøknad(NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                                       List<SakInfotrygdMigrering> eksisterendeMigreringTilVurdering,
                                                       YtelseFilter psbInfotrygdFilter, Long behandlingId) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        return eksisterendeMigreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .anyMatch(stp -> harNæringIInfotrygd(perioderTilVurdering, psbInfotrygdFilter, stp) &&
                !harOppgittNæringISøknad(iayGrunnlag, behandlingId, stp)
            );
    }

    private boolean harFrilansIInfotrygdOgManglerSøknad(NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                                       List<SakInfotrygdMigrering> eksisterendeMigreringTilVurdering,
                                                       YtelseFilter psbInfotrygdFilter, Long behandlingId) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        return eksisterendeMigreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .anyMatch(stp -> harFrilansIInfotrygd(perioderTilVurdering, psbInfotrygdFilter, stp) &&
                !harOppgittFrilansISøknad(iayGrunnlag, behandlingId, stp)
            );
    }


    private boolean harOppgittNæringISøknad(InntektArbeidYtelseGrunnlag iayGrunnlag, Long behandlingId, LocalDate stp) {
        return oppgittOpptjeningFilter.hentOppgittOpptjening(behandlingId, iayGrunnlag, stp).stream().anyMatch(oo -> !oo.getEgenNæring().isEmpty());
    }

    private boolean harOppgittFrilansISøknad(InntektArbeidYtelseGrunnlag iayGrunnlag, Long behandlingId, LocalDate stp) {
        return oppgittOpptjeningFilter.hentOppgittOpptjening(behandlingId, iayGrunnlag, stp).stream().anyMatch(oo -> oo.getFrilans().isPresent());
    }


    private boolean harNæringIInfotrygd(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter psbInfotrygdFilter, LocalDate stp) {
        return psbInfotrygdFilter.getFiltrertYtelser()
            .stream().filter(y -> harOverlappendePeriode(perioderTilVurdering, stp, y))
            .allMatch(this::harKategoriMedNæring);
    }

    private boolean harFrilansIInfotrygd(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter psbInfotrygdFilter, LocalDate stp) {
        return psbInfotrygdFilter.getFiltrertYtelser()
            .stream().filter(y -> harOverlappendePeriode(perioderTilVurdering, stp, y))
            .allMatch(this::harKategoriMedFrilans);
    }

    private boolean harOverlappendePeriode(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate stp, Ytelse y) {
        return y.getYtelseAnvist().stream().anyMatch(ya -> gjelderMigrertSkjæringstidspunkt(perioderTilVurdering, stp, ya));
    }

    private boolean harKategoriMedNæring(Ytelse ya) {
        return ya.getYtelseGrunnlag().stream()
            .flatMap(yg -> yg.getArbeidskategori().stream())
            .anyMatch(ak -> ak.equals(Arbeidskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
                || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_FISKER)
                || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_JORDBRUKER)
                || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE));
    }

    private boolean harKategoriMedFrilans(Ytelse ya) {
        return ya.getYtelseGrunnlag().stream()
            .flatMap(yg -> yg.getArbeidskategori().stream())
            .anyMatch(ak -> ak.equals(Arbeidskategori.FRILANSER)
                || ak.equals(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER));
    }


    private boolean gjelderMigrertSkjæringstidspunkt(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate stp, YtelseAnvist ya) {
        var anvistPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM());
        var periodeTilVurdering = finnPeriodeTilVurdering(perioderTilVurdering, stp);
        return anvistPeriode.inkluderer(stp) || kantIKantVurderer.erKantIKant(periodeTilVurdering, anvistPeriode);
    }

    private DatoIntervallEntitet finnPeriodeTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate stp) {
        return perioderTilVurdering.stream().filter(p -> p.getFomDato().equals(stp)).findFirst().orElseThrow();
    }

    private boolean harBrukerKunPleiepengerAvDagpenger(AksjonspunktUtlederInput param, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<SakInfotrygdMigrering> eksisterendeMigreringTilVurdering) {
        return eksisterendeMigreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .filter(stp -> perioderTilVurdering.stream().anyMatch(p -> p.getFomDato().equals(stp)))
            .allMatch(stp -> {
                var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(param.getRef(),
                    inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId()),
                    finnPeriodeTilVurdering(perioderTilVurdering, stp));
                if (opptjeningAktiviteter.isEmpty()) {
                    return false;
                }
                var aktiviteterPåStp = opptjeningAktiviteter.get().getOpptjeningPerioder().stream()
                    .filter(oa -> !oa.getPeriode().getTom().isBefore(stp))
                    .collect(Collectors.toList());
                if (aktiviteterPåStp.isEmpty()) {
                    return false;
                }
                return aktiviteterPåStp
                    .stream()
                    .allMatch(oa -> oa.getType().equals(OpptjeningAktivitetType.PLEIEPENGER_AV_DAGPENGER));
            });
    }

    private List<YtelseAnvist> finnAnvistePerioderFraInfotrygdUtenSøknad(AksjonspunktUtlederInput param, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter psbInfotrygdFilter) {
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(param.getBehandlingId());
        var førsteStpTilVurdering = perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder()).orElseThrow();
        var anvistePerioderUtenSøknad = psbInfotrygdFilter.getFiltrertYtelser().stream()
            .flatMap(y -> y.getYtelseAnvist().stream())
            .filter(ya -> harAnvisningSammeÅrSomFørstePeriodeTilVurdering(førsteStpTilVurdering, ya))
            .filter(ya -> !dekkesAvSøknad(fullstendigePerioder, ya, førsteStpTilVurdering.getYear()))
            .collect(Collectors.toList());
        return anvistePerioderUtenSøknad;
    }

    private YtelseFilter finnPSBInfotryd(AksjonspunktUtlederInput param) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        Optional<AktørYtelse> aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(param.getAktørId());
        YtelseFilter ytelseFilter = lagInfotrygdPSBFilter(aktørYtelse);
        return ytelseFilter;
    }

    private boolean dekkesAvSøknad(NavigableSet<DatoIntervallEntitet> fullstendigePerioder, YtelseAnvist ya, int year) {
        var førsteMandagIÅret = LocalDate.of(year, 1, 1).with(TemporalAdjusters.dayOfWeekInMonth(1, DayOfWeek.MONDAY));
        var anvistFom = ya.getAnvistFOM().isBefore(førsteMandagIÅret) ? førsteMandagIÅret : ya.getAnvistFOM();
        return fullstendigePerioder.stream().anyMatch(p -> p.getFomDato().equals(anvistFom) && !p.getTomDato().isBefore(ya.getAnvistTOM()));
    }

    private boolean harAnvisningSammeÅrSomFørstePeriodeTilVurdering(LocalDate førsteStpTilVurdering, YtelseAnvist ya) {
        return ya.getAnvistTOM().getYear() >= førsteStpTilVurdering.getYear() && ya.getAnvistFOM().getYear() <= førsteStpTilVurdering.getYear();
    }


    private List<SakInfotrygdMigrering> finnEksisterendeMigreringTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer) {
        var migreringTilVurdering = eksisterendeInfotrygdMigreringer.stream()
            .filter(sim -> perioderTilVurdering.stream().anyMatch(periode -> periode.inkluderer(sim.getSkjæringstidspunkt())))
            .collect(Collectors.toList());
        var antallPerioderMedOverlapp = perioderTilVurdering.stream().filter(periode -> migreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .anyMatch(periode::inkluderer)).count();
        if (migreringTilVurdering.size() > antallPerioderMedOverlapp) {
            throw new IllegalStateException("Forventer maksimalt en migrering til vurdering per periode");
        }
        return migreringTilVurdering;
    }

    private YtelseFilter lagInfotrygdPSBFilter(Optional<AktørYtelse> aktørYtelse) {
        return new YtelseFilter(aktørYtelse).filter(y ->
            y.getYtelseType().equals(FagsakYtelseType.PSB) &&
                y.getKilde().equals(Fagsystem.INFOTRYGD));
    }


}
