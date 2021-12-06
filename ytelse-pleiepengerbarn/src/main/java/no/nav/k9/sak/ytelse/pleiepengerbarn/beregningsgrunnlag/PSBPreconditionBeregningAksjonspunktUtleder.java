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

import org.jetbrains.annotations.NotNull;

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

    private OverlappendeYtelserTjeneste overlappendeYtelserTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private FagsakRepository fagsakRepository;
    private boolean toggleMigrering;

    public PSBPreconditionBeregningAksjonspunktUtleder() {
    }

    @Inject
    public PSBPreconditionBeregningAksjonspunktUtleder(OverlappendeYtelserTjeneste overlappendeYtelserTjeneste,
                                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                       FagsakRepository fagsakRepository,
                                                       @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering) {
        this.overlappendeYtelserTjeneste = overlappendeYtelserTjeneste;
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
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        Optional<AktørYtelse> aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(param.getAktørId());
        YtelseFilter ytelseFilter = lagInfotrygdPSBFilter(aktørYtelse);
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        LocalDateTimeline<Boolean> vilkårsperioderTidslinje = lagPerioderTilVureringTidslinje(perioderTilVurdering);
        Map<Ytelse, NavigableSet<LocalDateInterval>> psbOverlapp = overlappendeYtelserTjeneste.doFinnOverlappendeYtelser(vilkårsperioderTidslinje, ytelseFilter);
        if (!psbOverlapp.isEmpty()) {
            lagreOverlappPerioder(param, perioderTilVurdering, psbOverlapp);
            return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT));
        } else {
            return Collections.emptyList();
        }
    }

    private void lagreOverlappPerioder(AksjonspunktUtlederInput param, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Map<Ytelse, NavigableSet<LocalDateInterval>> psbOverlapp) {
        Set<LocalDateInterval> overlappPerioder = psbOverlapp.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Set<LocalDate> stpMedInfotrygdoverlapp = finnStpMedOverlapp(perioderTilVurdering, overlappPerioder);
        stpMedInfotrygdoverlapp.stream().map(stp -> new SakInfotrygdMigrering(param.getRef().getFagsakId(), stp))
                .forEach(fagsakRepository::lagre);
    }

    @NotNull
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
