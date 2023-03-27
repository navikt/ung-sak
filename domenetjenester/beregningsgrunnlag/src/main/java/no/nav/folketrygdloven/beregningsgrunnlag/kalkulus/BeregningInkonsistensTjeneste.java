package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@Dependent
public class BeregningInkonsistensTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BeregningInkonsistensTjeneste.class);

    private final KalkulusTjeneste kalkulusTjeneste;
    private final BeregningsgrunnlagReferanserTjeneste beregningsgrunnlagReferanserTjeneste;

    private final Instance<ForlengelseTjeneste> forlengelseTjeneste;

    private final VilkårTjeneste vilkårTjeneste;

    private final Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjenester;

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private final ProsessTriggereRepository prosessTriggereRepository;

    private final boolean sjekkEnabled;

    @Inject
    public BeregningInkonsistensTjeneste(KalkulusTjeneste kalkulusTjeneste,
                                         BeregningsgrunnlagReferanserTjeneste beregningsgrunnlagReferanserTjeneste,
                                         Instance<ForlengelseTjeneste> forlengelseTjeneste,
                                         VilkårTjeneste vilkårTjeneste,
                                         Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                         InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                         ProsessTriggereRepository prosessTriggereRepository,
                                         @KonfigVerdi(value = "BEREGNING_VURDER_INKONSISTENS", defaultVerdi = "false") boolean sjekkEnabled) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsgrunnlagReferanserTjeneste = beregningsgrunnlagReferanserTjeneste;
        this.forlengelseTjeneste = forlengelseTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.opptjeningForBeregningTjenester = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.sjekkEnabled = sjekkEnabled;
    }

    /**
     * Sjekker om Beregning og Opptjening har inkosistent data
     * <p>
     * Inkonsistens betyr at aktivitetstatus i beregning ikke matcher status fra opptjening.
     * Dette kan skje i følgende caser:
     * - divergerende logikk i beregning og opptjening
     * - ved feil input til beregning (kalkulus brukte før inputdata til opptjening)
     * <p>
     * Denne utledningen må skje før all prosessering i beregning siden den endrer på status for perioder (flipper fra forlengelse til revurdering).
     *
     * @param ref Behandlingreferanse
     */
    public void sjekkInkonsistensOgOpprettProsesstrigger(BehandlingReferanse ref) {
        if (!sjekkEnabled) {
            return;
        }
        NavigableSet<DatoIntervallEntitet> perioderSomRevurderes = finnPerioderMedInkonsistens(ref);
        if (!perioderSomRevurderes.isEmpty()) {
            prosessTriggereRepository.leggTil(ref.getId(), perioderSomRevurderes.stream().map(it -> new Trigger(BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG, it)).collect(Collectors.toSet()));
        }
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioderMedInkonsistens(BehandlingReferanse ref) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        var forlengelser = finnForlengelserIBeregning(ref);
        var gjeldendeGrunnlag = finnBeregningsgrunnlagsliste(ref, forlengelser);
        var opptjeningForBeregningTjeneste = finnOpptjeningForBeregningTjeneste(ref);
        return utledPerioderMedInkonsistens(ref,
            iayGrunnlag,
            forlengelser,
            gjeldendeGrunnlag,
            opptjeningForBeregningTjeneste);
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioderMedInkonsistens(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, NavigableSet<DatoIntervallEntitet> forlengelser, List<BeregningsgrunnlagGrunnlag> gjeldendeGrunnlag, OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste) {
        NavigableSet<DatoIntervallEntitet> perioderSomRevurderes = new TreeSet<>();
        gjeldendeGrunnlag.stream().map(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .flatMap(Optional::stream)
            .forEach(bg -> {
                var periode = forlengelser.stream().filter(p -> p.getFomDato().equals(bg.getSkjæringstidspunkt())).findFirst().orElseThrow();
                boolean erInkosistent = vurderInkosistensForPeriode(ref, opptjeningForBeregningTjeneste, iayGrunnlag, bg, periode);
                if (erInkosistent) {
                    LOG.warn("Fant inkosistens mellom opptjening og beregning for periode {}. Trigger automatisk revurdering av beregning.", periode);
                    perioderSomRevurderes.add(periode);
                }
            });
        return perioderSomRevurderes;
    }

    private NavigableSet<DatoIntervallEntitet> finnForlengelserIBeregning(BehandlingReferanse ref) {
        var perioderTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return ForlengelseTjeneste.finnTjeneste(forlengelseTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .utledPerioderSomSkalBehandlesSomForlengelse(ref, perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
    }

    private List<BeregningsgrunnlagGrunnlag> finnBeregningsgrunnlagsliste(BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> forlengelser) {
        var bgReferanser = beregningsgrunnlagReferanserTjeneste.finnReferanseEllerLagNy(ref.getBehandlingId(),
            forlengelser.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toSet()),
            true,
            false);
        return kalkulusTjeneste.hentGrunnlag(ref, bgReferanser);
    }

    private boolean vurderInkosistensForPeriode(BehandlingReferanse ref, OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, InntektArbeidYtelseGrunnlag iayGrunnlag, Beregningsgrunnlag bg, DatoIntervallEntitet periode) {
        boolean harBrukersAndel = vurderHarBrukersAndel(bg);
        boolean erKunYtelse = vurderHarKunYtelse(ref, opptjeningForBeregningTjeneste, iayGrunnlag, bg, periode);
        var erMidlertidigInaktiv = erMidlertidigInaktiv(ref, periode);
        return (erKunYtelse || erMidlertidigInaktiv) && !harBrukersAndel;
    }

    private static boolean vurderHarKunYtelse(BehandlingReferanse ref, OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, InntektArbeidYtelseGrunnlag iayGrunnlag, Beregningsgrunnlag bg, DatoIntervallEntitet periode) {
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(ref, iayGrunnlag, periode);
        var aktiviteterPåStp = opptjeningAktiviteter.stream().flatMap(a -> a.getOpptjeningPerioder().stream())
            .filter(p -> p.getPeriode().overlaps(new Periode(bg.getSkjæringstidspunkt(), bg.getSkjæringstidspunkt())))
            .toList();
        return erKunYtelse(aktiviteterPåStp);
    }

    private static boolean vurderHarBrukersAndel(Beregningsgrunnlag bg) {
        var førstePeriode = bg.getBeregningsgrunnlagPerioder().stream()
            .min(Comparator.comparing(BeregningsgrunnlagPeriode::getPeriode))
            .orElseThrow(() -> new IllegalStateException("Forventer at beregningsgrunnlaget har minst en periode ved forlengelser"));
        return førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.BRUKERS_ANDEL));
    }

    private static boolean erKunYtelse(List<OpptjeningAktiviteter.OpptjeningPeriode> aktiviteterPåStp) {
        return aktiviteterPåStp.stream().allMatch(a -> OpptjeningAktivitetType.YTELSE.contains(a.getType()));
    }

    private boolean erMidlertidigInaktiv(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        var opptjeningsvilkåret = vilkårTjeneste.hentVilkårResultat(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        return opptjeningsvilkåret.stream().flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getPeriode().equals(periode))
            .findFirst()
            .map(VilkårPeriode::getMerknad)
            .map(utfall -> utfall.equals(VilkårUtfallMerknad.VM_7847_A) || utfall.equals(VilkårUtfallMerknad.VM_7847_B))
            .orElse(false);
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse behandlingRef) {
        FagsakYtelseType ytelseType = behandlingRef.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjenester, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

}
