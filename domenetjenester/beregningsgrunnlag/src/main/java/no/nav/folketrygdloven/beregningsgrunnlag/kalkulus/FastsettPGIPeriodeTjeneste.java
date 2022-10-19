package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.PGIPeriode;

@Dependent
public class FastsettPGIPeriodeTjeneste {

    private static final Logger log = LoggerFactory.getLogger(FastsettPGIPeriodeTjeneste.class);

    private final BeregningPerioderGrunnlagRepository grunnlagRepository;
    private final VilkårTjeneste vilkårTjeneste;
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private final OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private final ProsessTriggereRepository prosessTriggereRepository;


    @Inject
    public FastsettPGIPeriodeTjeneste(BeregningPerioderGrunnlagRepository grunnlagRepository,
                                      VilkårTjeneste vilkårTjeneste,
                                      InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                      OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider,
                                      ProsessTriggereRepository prosessTriggereRepository) {
        this.grunnlagRepository = grunnlagRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
        this.prosessTriggereRepository = prosessTriggereRepository;
    }


    /**
     * Fastsetter PGI som ble brukt til å fatte vedtak for å støtte § 8-35 andre ledd (TSF-2701)
     * <p>
     * Lagrer ned referanse til hvilket iay-grunnlag som ble brukt for hvert skjæringstidspunkt i beregning dersom bruker er selvstendig næringsdrivende eller er midlertidig inaktiv.
     * <p>
     * Fra lovteksten:
     * Jf § 8-35 andre ledd er Det er de ferdiglignede inntektene som foreligger på vedtakstidspunktet som brukes.
     * Dette avviker fra prinsippet om at det er opplysningene som foreligger på sykmeldingstidspunktet som skal anvendes.
     * Årsaken til at prinsippet fravikes er at Arbeids- og velferdsetaten ikke har opplysninger om dato for skatteoppgjøret til den enkelte
     * <p>
     * Vi behandler et vedtak som førstegangsvedtak dersom forrige søknad for stp ikke hadde opplysning om næring eller bruker ikke var midlertidig inaktiv
     *
     * @param behandlingId BehandlingId
     */
    public void fastsettPGIDersomRelevant(Long behandlingId) {
        var vilkårene = vilkårTjeneste.hentVilkårResultat(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        if (vilkår.isPresent()) {
            var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
            var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(behandlingId);
            var skjæringstidspunkter = finnSkjæringstidspunkter(vilkår.get());
            var pgiPerioder = finnEksisterendePGIPerioder(behandlingId);
            var perioderSomSkalFjernes = finnPerioderSomIkkeErSkjæringstidspunktISaken(skjæringstidspunkter, pgiPerioder);
            // Vi behandler et vedtak for SN som førstegangsvedtak dersom forrige søknad for stp ikke hadde opplysning om næring
            var nyeSkjæringstidspunkt = finnSTPMedFørstegangsvedtakSomOmfattesAv8_35(behandlingId, iayGrunnlag, oppgittOpptjeningFilter, skjæringstidspunkter, pgiPerioder);
            grunnlagRepository.lagreOgDeaktiverPGIPerioder(behandlingId, nyeSkjæringstidspunkt, perioderSomSkalFjernes);
        }
    }

    /**
     * Fjerner periode for PGI dersom bruker ikke omfattes av § 8-35
     *
     * @param behandlingId BehandlingId
     */
    public void fjernPGIDersomIkkeRelevant(Long behandlingId) {
        var vilkårene = vilkårTjeneste.hentVilkårResultat(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        if (vilkår.isPresent()) {
            var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
            var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(behandlingId);
            var skjæringstidspunkter = finnSkjæringstidspunkter(vilkår.get());
            var sigruninntektPerioder = finnEksisterendePGIPerioder(behandlingId);
            var perioderSomSkalFjernes = finnPerioderSomSkalFjernes(behandlingId, iayGrunnlag, oppgittOpptjeningFilter, skjæringstidspunkter, sigruninntektPerioder);
            if (!perioderSomSkalFjernes.isEmpty()) {
                log.info("Fjerner PGI-perioder " + perioderSomSkalFjernes);
            }
            grunnlagRepository.lagreOgDeaktiverPGIPerioder(behandlingId, Collections.emptyList(), perioderSomSkalFjernes);
        }
    }

    private List<PGIPeriode> finnSTPMedFørstegangsvedtakSomOmfattesAv8_35(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, OppgittOpptjeningFilter oppgittOpptjeningFilter, List<LocalDate> skjæringstidspunkter, List<PGIPeriode> PGIPerioder) {
        return skjæringstidspunkter.stream().
            filter(stp -> omfattesAv8_35(behandlingId, iayGrunnlag, oppgittOpptjeningFilter, stp))
            .filter(stp -> PGIPerioder.stream().noneMatch(p -> p.getSkjæringstidspunkt().equals(stp)))
            .map(stp -> new PGIPeriode(iayGrunnlag.getEksternReferanse(), stp))
            .collect(Collectors.toList());
    }

    private List<PGIPeriode> finnEksisterendePGIPerioder(Long behandlingId) {
        var grunnlagOpt = grunnlagRepository.hentGrunnlag(behandlingId);
        return grunnlagOpt.map(BeregningsgrunnlagPerioderGrunnlag::getPGIPerioder)
            .orElse(Collections.emptyList());
    }

    private List<LocalDate> finnSkjæringstidspunkter(Vilkår vilkår) {
        return vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .sorted()
            .distinct()
            .collect(Collectors.toList());
    }

    private List<PGIPeriode> finnPerioderSomIkkeErSkjæringstidspunktISaken(List<LocalDate> skjæringstidspunkter,
                                                                           List<PGIPeriode> PGIPerioder) {
        return PGIPerioder.stream()
            .filter(p -> !skjæringstidspunkter.contains(p.getSkjæringstidspunkt()))
            .collect(Collectors.toCollection(ArrayList::new));
    }


    private ArrayList<PGIPeriode> finnPerioderSomSkalFjernes(Long behandlingId,
                                                             InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                             OppgittOpptjeningFilter oppgittOpptjeningFilter,
                                                             List<LocalDate> skjæringstidspunkter,
                                                             List<PGIPeriode> PGIPerioder) {
        var nyInnhentingTriggere = finnRelevanteProsessTriggere(behandlingId);
        return PGIPerioder.stream()
            .filter(p -> skjæringstidspunkter.contains(p.getSkjæringstidspunkt()))
            .filter(p -> !omfattesAv8_35(behandlingId, iayGrunnlag, oppgittOpptjeningFilter, p.getSkjæringstidspunkt())
                || harTriggerForInnhentingPåNytt(nyInnhentingTriggere, p)
            )
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Set<Trigger> finnRelevanteProsessTriggere(Long behandlingId) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandlingId);
        return prosessTriggere.stream().flatMap(p -> p.getTriggere().stream())
            .filter(p -> p.getÅrsak().equals(BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT))
            .collect(Collectors.toSet());
    }

    public boolean omfattesAv8_35(Long behandlingId,
                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                  OppgittOpptjeningFilter oppgittOpptjeningFilter,
                                  LocalDate skjæringstidspunkt) {
        return erSelvstendigNæringsdrivende(behandlingId, iayGrunnlag, oppgittOpptjeningFilter, skjæringstidspunkt) || erMidlertidigInaktiv(behandlingId, skjæringstidspunkt);
    }

    private boolean harTriggerForInnhentingPåNytt(Set<Trigger> nyInnhentingTriggere, PGIPeriode p) {
        return nyInnhentingTriggere.stream().anyMatch(trigger -> trigger.getPeriode().inkluderer(p.getSkjæringstidspunkt()));
    }

    private boolean erMidlertidigInaktiv(Long behandlingId, LocalDate skjæringstidspunkt) {
        var opptjeningsvilkår = vilkårTjeneste.hentVilkårResultat(behandlingId).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        var vilkårUtfallMerknad = finnVilkårmerknadForOpptjening(opptjeningsvilkår, skjæringstidspunkt);
        return VilkårUtfallMerknad.VM_7847_A.equals(vilkårUtfallMerknad) || VilkårUtfallMerknad.VM_7847_B.equals(vilkårUtfallMerknad);
    }

    private VilkårUtfallMerknad finnVilkårmerknadForOpptjening(Optional<Vilkår> opptjeningsvilkår, LocalDate skjæringstidspunkt) {
        VilkårUtfallMerknad vilkårsMerknad = null;
        if (opptjeningsvilkår.isPresent()) {
            vilkårsMerknad = opptjeningsvilkår.get().finnPeriodeForSkjæringstidspunkt(skjæringstidspunkt).getMerknad();
        }
        return vilkårsMerknad;
    }

    private boolean erSelvstendigNæringsdrivende(Long behandlingId,
                                                 InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                 OppgittOpptjeningFilter oppgittOpptjeningFilter,
                                                 LocalDate stp) {
        return oppgittOpptjeningFilter.hentOppgittOpptjening(behandlingId, iayGrunnlag, stp).stream()
            .flatMap(oo -> oo.getEgenNæring().stream())
            .anyMatch(e -> e.getPeriode().inkluderer(stp.minusDays(1)));
    }


}
