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
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
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

    @Inject
    public BeregningInkonsistensTjeneste(KalkulusTjeneste kalkulusTjeneste,
                                         BeregningsgrunnlagReferanserTjeneste beregningsgrunnlagReferanserTjeneste,
                                         @Any Instance<ForlengelseTjeneste> forlengelseTjeneste,
                                         VilkårTjeneste vilkårTjeneste,
                                         @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                         InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                         ProsessTriggereRepository prosessTriggereRepository) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsgrunnlagReferanserTjeneste = beregningsgrunnlagReferanserTjeneste;
        this.forlengelseTjeneste = forlengelseTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.opptjeningForBeregningTjenester = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    /**
     * Sjekker om Beregning og Opptjening har inkonsistent data
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
        NavigableSet<DatoIntervallEntitet> perioderSomRevurderes = finnPerioderMedInkonsistens(ref);
        if (!perioderSomRevurderes.isEmpty()) {
            prosessTriggereRepository.leggTil(ref.getId(), perioderSomRevurderes.stream().map(it -> new Trigger(BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG, it)).collect(Collectors.toSet()));
        }
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioderMedInkonsistens(BehandlingReferanse ref) {
        if (!ref.getBehandlingType().equals(BehandlingType.REVURDERING)) {
            return new TreeSet<>();
        }
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        var forlengelser = finnForlengelserIBeregning(ref);
        var opptjeningForBeregningTjeneste = finnOpptjeningForBeregningTjeneste(ref);
        return utledPerioderMedInkonsistens(ref,
            iayGrunnlag,
            forlengelser,
            opptjeningForBeregningTjeneste);
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioderMedInkonsistens(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                            NavigableSet<DatoIntervallEntitet> forlengelser,
                                                                            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste) {
        NavigableSet<DatoIntervallEntitet> perioderSomRevurderes = new TreeSet<>();
        var perioderSomSkalHaBrukersAndel = finnPerioderSomSkalHaBrukersAndel(ref, iayGrunnlag, forlengelser, opptjeningForBeregningTjeneste);
        if (!perioderSomSkalHaBrukersAndel.isEmpty()) {
            LOG.info("Fant perioder som skal ha brukers andel i beregning: {}", perioderSomSkalHaBrukersAndel);
        }
        var originaleBeregningsgrunnlag = finnOriginalBeregningsgrunnlagsliste(ref, perioderSomSkalHaBrukersAndel);
        originaleBeregningsgrunnlag.stream().map(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .flatMap(Optional::stream)
            .forEach(bg -> {
                var periode = perioderSomSkalHaBrukersAndel.stream().filter(p -> p.getFomDato().equals(bg.getSkjæringstidspunkt())).findFirst().orElseThrow();
                boolean erInkonsistent = !vurderHarBrukersAndel(bg);
                if (erInkonsistent) {
                    LOG.info("Fant inkonsistens mellom opptjening og beregning for periode {}. Trigger automatisk revurdering av beregning.", periode);
                    perioderSomRevurderes.add(periode);
                }
            });
        return perioderSomRevurderes;
    }

    private TreeSet<DatoIntervallEntitet> finnPerioderSomSkalHaBrukersAndel(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, NavigableSet<DatoIntervallEntitet> forlengelser, OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste) {
        return forlengelser.stream()
            .filter(periode -> skalHaBrukersAndelIBeregning(ref, opptjeningForBeregningTjeneste, iayGrunnlag, periode))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private NavigableSet<DatoIntervallEntitet> finnForlengelserIBeregning(BehandlingReferanse ref) {
        var perioderTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return ForlengelseTjeneste.finnTjeneste(forlengelseTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .utledPerioderSomSkalBehandlesSomForlengelse(ref, perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
    }

    private List<BeregningsgrunnlagGrunnlag> finnOriginalBeregningsgrunnlagsliste(BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> perioder) {
        var bgReferanser = beregningsgrunnlagReferanserTjeneste.finnReferanseEllerLagNy(ref.getOriginalBehandlingId().orElseThrow(),
            perioder.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toSet()),
            true,
            false);
        return kalkulusTjeneste.hentGrunnlag(ref, bgReferanser);
    }

    private boolean skalHaBrukersAndelIBeregning(BehandlingReferanse ref, OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                 DatoIntervallEntitet periode) {
        var erMidlertidigInaktiv = erMidlertidigInaktiv(ref, periode);
        boolean erKunYtelse = vurderHarKunYtelse(ref, opptjeningForBeregningTjeneste, iayGrunnlag, periode);
        return erKunYtelse || erMidlertidigInaktiv;
    }

    private static boolean vurderHarKunYtelse(BehandlingReferanse ref, OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet periode) {
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(ref, iayGrunnlag, periode);
        var aktiviteterPåStp = opptjeningAktiviteter.stream().flatMap(a -> a.getOpptjeningPerioder().stream())
            .filter(p -> p.getPeriode().overlaps(new Periode(periode.getFomDato().minusDays(1), periode.getFomDato().minusDays(1))))
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
        return !aktiviteterPåStp.isEmpty() && aktiviteterPåStp.stream().allMatch(a -> OpptjeningAktivitetType.YTELSE.contains(a.getType()));
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
