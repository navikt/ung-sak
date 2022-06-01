package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.PreconditionBeregningAksjonspunktUtleder;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening.PSBOppgittOpptjeningFilter;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class PSBPreconditionBeregningAksjonspunktUtleder implements PreconditionBeregningAksjonspunktUtleder {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private PSBOpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private FagsakRepository fagsakRepository;
    private PSBOppgittOpptjeningFilter oppgittOpptjeningFilter;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private VilkårPeriodeFilterProvider periodeFilterProvider;

    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private BehandlingRepository behandlingRepository;

    private boolean toggleMigrering;
    private boolean enableForlengelse;
    private final PåTversAvHelgErKantIKantVurderer kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();


    public PSBPreconditionBeregningAksjonspunktUtleder() {
    }

    @Inject
    public PSBPreconditionBeregningAksjonspunktUtleder(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) PSBOpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                                       FagsakRepository fagsakRepository,
                                                       @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) PSBOppgittOpptjeningFilter oppgittOpptjeningFilter,
                                                       BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                                       VilkårPeriodeFilterProvider periodeFilterProvider,
                                                       BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                       BehandlingRepository behandlingRepository,
                                                       @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering,
                                                       @KonfigVerdi(value = "forlengelse.beregning.enablet", defaultVerdi = "false") Boolean enableForlengelse) {
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.oppgittOpptjeningFilter = oppgittOpptjeningFilter;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.periodeFilterProvider = periodeFilterProvider;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.toggleMigrering = toggleMigrering;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.enableForlengelse = enableForlengelse;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (!toggleMigrering) {
            return Collections.emptyList();
        }
        var perioderTilVurdering = finnPerioderTilVurdering(param);
        if (perioderTilVurdering.isEmpty()) {
            return Collections.emptyList();
        }

        var eksisterendeInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(param.getRef().getFagsakId());
        var eksisterendeMigreringTilVurdering = finnEksisterendeMigreringTilVurdering(perioderTilVurdering, eksisterendeInfotrygdMigreringer);
        var psbInfotrygdFilter = finnPSBInfotryd(param);

        if (skalOverstyreInput(param, eksisterendeMigreringTilVurdering)) {
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

    /** Vi oppretter ikke aksjonspunkt dersom det er en revurdering som ikke er manuelt opprettet og vi allerede har kopiert overstyrt input fra forrige behandling for alle perioder til vurdering
     *
     * @param param inneholder informasjon om behandling
     * @param eksisterendeMigreringTilVurdering eksisterende migreringer til vurdering
     * @return
     */
    private boolean skalOverstyreInput(AksjonspunktUtlederInput param, List<SakInfotrygdMigrering> eksisterendeMigreringTilVurdering) {
        var inputOverstyringer = beregningPerioderGrunnlagRepository.hentGrunnlag(param.getBehandlingId())
            .stream()
            .flatMap(gr -> gr.getInputOverstyringPerioder().stream())
            .toList();

        var harOverstyringForAllePerioder = eksisterendeMigreringTilVurdering.stream().allMatch(it -> inputOverstyringer.stream().anyMatch(o -> o.getSkjæringstidspunkt().equals(it.getSkjæringstidspunkt())));
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var erManueltOpprettet = behandling.erManueltOpprettet();
        var erRevurderingMedKopiertInputOverstyring = behandling.erRevurdering() && !erManueltOpprettet && harOverstyringForAllePerioder;
        return !eksisterendeMigreringTilVurdering.isEmpty() && !erRevurderingMedKopiertInputOverstyring;
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioderTilVurdering(AksjonspunktUtlederInput param) {
        var periodeFilter = periodeFilterProvider.getFilter(param.getRef(), false);
        if (enableForlengelse) {
            periodeFilter.ignorerForlengelseperioder();
        }
        periodeFilter.ignorerAvslåttePerioderInkludertKompletthet();
        return beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(param.getRef(), periodeFilter);
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
        var overlappendePerioder = finnOverlappendePerioder(perioderTilVurdering, psbInfotrygdFilter, stp);
        return !overlappendePerioder.isEmpty() && overlappendePerioder.stream().allMatch(this::harKategoriMedNæring);
    }

    private List<Ytelse> finnOverlappendePerioder(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter psbInfotrygdFilter, LocalDate stp) {
        return psbInfotrygdFilter.getFiltrertYtelser()
            .stream().filter(y -> harOverlappendePeriode(perioderTilVurdering, stp, y))
            .toList();
    }

    private boolean harFrilansIInfotrygd(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, YtelseFilter psbInfotrygdFilter, LocalDate stp) {
        var overlappendePerioder = finnOverlappendePerioder(perioderTilVurdering, psbInfotrygdFilter, stp);
        return !overlappendePerioder.isEmpty() && overlappendePerioder.stream().allMatch(this::harKategoriMedFrilans);
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


    private YtelseFilter finnPSBInfotryd(AksjonspunktUtlederInput param) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        Optional<AktørYtelse> aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(param.getAktørId());
        YtelseFilter ytelseFilter = lagInfotrygdPSBFilter(aktørYtelse);
        return ytelseFilter;
    }


    private List<SakInfotrygdMigrering> finnEksisterendeMigreringTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer) {
        var migreringTilVurdering = eksisterendeInfotrygdMigreringer.stream()
            .filter(sim -> perioderTilVurdering.stream().anyMatch(periode -> periode.inkluderer(sim.getSkjæringstidspunkt())))
            .collect(Collectors.toList());
        var antallPerioderMedOverlapp = perioderTilVurdering.stream().filter(periode -> migreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .anyMatch(periode::inkluderer)).count();
        if (migreringTilVurdering.size() > antallPerioderMedOverlapp) {
            throw new IllegalStateException(
                String.format("Forventer maksimalt en migrering til vurdering per periode. " +
                        "Migrerte skjæringstidspunkt : %s, " +
                        "Perioder til vurdering: %s",
                    eksisterendeInfotrygdMigreringer.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt).collect(Collectors.toSet()),
                    perioderTilVurdering));
        }
        return migreringTilVurdering;
    }

    private YtelseFilter lagInfotrygdPSBFilter(Optional<AktørYtelse> aktørYtelse) {
        return new YtelseFilter(aktørYtelse).filter(y ->
            y.getYtelseType().equals(FagsakYtelseType.PSB) &&
                y.getKilde().equals(Fagsystem.INFOTRYGD));
    }


}
