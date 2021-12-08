package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.PreconditionBeregningAksjonspunktUtleder;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBPreconditionBeregningAksjonspunktUtleder implements PreconditionBeregningAksjonspunktUtleder {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private FagsakRepository fagsakRepository;
    private boolean toggleMigrering;

    public PSBPreconditionBeregningAksjonspunktUtleder() {
    }

    @Inject
    public PSBPreconditionBeregningAksjonspunktUtleder(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                       FagsakRepository fagsakRepository,
                                                       @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.toggleMigrering = toggleMigrering;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (!toggleMigrering) {
            return Collections.emptyList();
        }
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        if (harEksisterendeMigreringTilVurdering(param, perioderTilVurdering)) {
            return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT));
        }
        var stpMedOverlapp = finnSkjæringstidspunktMedOverlapp(param, perioderTilVurdering);
        if (stpMedOverlapp.isPresent()) {
            fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(param.getRef().getFagsakId(), stpMedOverlapp.get()));
            return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT));
        }
        return Collections.emptyList();
    }

    private Optional<LocalDate> finnSkjæringstidspunktMedOverlapp(AksjonspunktUtlederInput param, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        Optional<AktørYtelse> aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(param.getAktørId());
        YtelseFilter ytelseFilter = lagInfotrygdPSBFilter(aktørYtelse);
        LocalDateTimeline<Boolean> vilkårsperioderTidslinje = lagPerioderTilVureringTidslinje(perioderTilVurdering);
        Map<Ytelse, NavigableSet<LocalDateInterval>> psbOverlapp = OverlappendeYtelserTjeneste.doFinnOverlappendeYtelser(vilkårsperioderTidslinje, ytelseFilter);
        var kantIKantPeriode = finnKantIKantPeriode(ytelseFilter, perioderTilVurdering);
        if (!psbOverlapp.isEmpty()) {
            return finnSkjæringstidspunktMedOverlapp(perioderTilVurdering, psbOverlapp);
        }
        return kantIKantPeriode.map(DatoIntervallEntitet::getFomDato);
    }

    private boolean harEksisterendeMigreringTilVurdering(AksjonspunktUtlederInput param, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var eksisterendeInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigrering(param.getRef().getFagsakId());
        if (eksisterendeInfotrygdMigrering.isPresent()) {
            var migrertStp = eksisterendeInfotrygdMigrering.get().getSkjæringstidspunkt();
            return perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato)
                .anyMatch(migrertStp::equals);
        }
        return false;
    }

    private Optional<DatoIntervallEntitet> finnKantIKantPeriode(YtelseFilter ytelseFilter, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        return perioderTilVurdering.stream()
            .filter(p -> ytelseFilter.getFiltrertYtelser().stream()
                .anyMatch(y -> y.getYtelseAnvist().stream().anyMatch(ya -> p.getFomDato().equals(ya.getAnvistTOM().plusDays(1)))))
            .findFirst();
    }

    private Optional<LocalDate> finnSkjæringstidspunktMedOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Map<Ytelse, NavigableSet<LocalDateInterval>> psbOverlapp) {
        Set<LocalDateInterval> overlappPerioder = psbOverlapp.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Set<LocalDate> stpMedInfotrygdoverlapp = finnStpMedOverlapp(perioderTilVurdering, overlappPerioder);
        if (stpMedInfotrygdoverlapp.isEmpty()) {
            return Optional.empty();
        }
        if (stpMedInfotrygdoverlapp.size() == 1) {
            return Optional.of(stpMedInfotrygdoverlapp.iterator().next());
        }
        throw new IllegalStateException("Fant flere skjæringstidspunkter med overlapp mot PSB i infotrygd");
    }

    private Set<LocalDate> finnStpMedOverlapp(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Set<LocalDateInterval> overlappPerioder) {
        return perioderTilVurdering.stream()
            .filter(p -> overlappPerioder.stream().map(ldi -> DatoIntervallEntitet.fraOgMedTilOgMed(ldi.getFomDato(), ldi.getTomDato()))
                .anyMatch(op -> op.overlapper(p)))
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toSet());
    }

    private LocalDateTimeline<Boolean> lagPerioderTilVureringTidslinje(NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        List<LocalDateSegment<Boolean>> segmenter = perioderTilVurdering.stream()
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), true))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segmenter);
    }

    private YtelseFilter lagInfotrygdPSBFilter(Optional<AktørYtelse> aktørYtelse) {
        return new YtelseFilter(aktørYtelse).filter(y ->
            y.getYtelseType().equals(FagsakYtelseType.PSB) &&
                y.getKilde().equals(Fagsystem.INFOTRYGD));
    }
}
