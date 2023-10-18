package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;

@Dependent
public class OpptjeningsaktiviteterPreconditionForBeregning {

    private static final Logger logger = LoggerFactory.getLogger(OpptjeningsaktiviteterPreconditionForBeregning.class);
    private static final Set<VilkårUtfallMerknad> MIDLERTIDIG_INAKTIV_KODER = Set.of(VilkårUtfallMerknad.VM_7847_A, VilkårUtfallMerknad.VM_7847_B);

    private final VilkårResultatRepository vilkårResultatRepository;
    private final OpptjeningRepository opptjeningRepository;

    @Inject
    public OpptjeningsaktiviteterPreconditionForBeregning(VilkårResultatRepository vilkårResultatRepository, OpptjeningRepository opptjeningRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.opptjeningRepository = opptjeningRepository;
    }

    public void sjekkOpptjeningsaktiviter(Long behandlingId) {
        Optional<OpptjeningResultat> opptjeningsresultat = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjeningsresultat.isEmpty()) {
            logger.warn("Har ikke noe opptjeningsresultat");
            return;
        }

        Vilkår opptjeningsvilkåret = vilkårResultatRepository.hent(behandlingId).getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();
        for (VilkårPeriode vilkårPeriode : opptjeningsvilkåret.getPerioder()) {
            var opptjening = opptjeningsresultat.get().finnOpptjening(vilkårPeriode.getSkjæringstidspunkt())
                .orElseThrow(() -> new IllegalStateException("Fant ikke opptjening for skjæringstidspunkt " + vilkårPeriode.getSkjæringstidspunkt()));
            if (vilkårPeriode.getUtfall() == Utfall.OPPFYLT && !MIDLERTIDIG_INAKTIV_KODER.contains(vilkårPeriode.getMerknad())) {
                sjekkHarAktivitetIHelePerioden(opptjening);
            }


            if (harIkkeInnhentetSigrunForAlleÅr(vilkårPeriode) && skalBrukeSigruninntekt(vilkårPeriode, opptjening)) {
                throw new IllegalStateException("Kan ikke beregne for status Midlertidig inaktiv eller SN for skjæringstidspunkt før 2019. Sjekk at søknadsperiode er riktig og håndter ved overstyring.");
            }
        }
    }

    private static boolean harIkkeInnhentetSigrunForAlleÅr(VilkårPeriode vilkårPeriode) {
        return vilkårPeriode.getSkjæringstidspunkt().getYear() < 2019;
    }

    private static boolean skalBrukeSigruninntekt(VilkårPeriode vilkårPeriode, Opptjening opptjening) {
        return MIDLERTIDIG_INAKTIV_KODER.contains(vilkårPeriode.getMerknad()) || harNæringVedSkjæringstidspunkt(vilkårPeriode, opptjening);
    }

    private static boolean harNæringVedSkjæringstidspunkt(VilkårPeriode vilkårPeriode, Opptjening opptjening) {
        return opptjening.getOpptjeningAktivitet().stream()
            .anyMatch(a ->
                a.getAktivitetType().equals(OpptjeningAktivitetType.NÆRING) &&
                    !a.getFom().isAfter(vilkårPeriode.getSkjæringstidspunkt().minusDays(1)) &&
                    !a.getTom().isBefore(vilkårPeriode.getSkjæringstidspunkt().minusDays(1)));
    }

    private void sjekkHarAktivitetForHelePerioden(LocalDate skjæringstidspunkt, OpptjeningResultat opptjeningResultat, Opptjening opptjening) {
        Optional<Opptjening> opptjeningOpt = opptjeningResultat.finnOpptjening(skjæringstidspunkt);
        if (opptjeningOpt.isEmpty()) {
            logger.warn("Fant ikke opptjening for skjæringstidspunkt {}", skjæringstidspunkt);
        } else {
            sjekkHarAktivitetIHelePerioden(opptjening);
        }
    }

    private void sjekkHarAktivitetIHelePerioden(Opptjening opptjening) {
        LocalDateTimeline<Boolean> opptjeningsperiode = new LocalDateTimeline<>(opptjening.getFom(), opptjening.getTom(), true);
        LocalDateTimeline<Boolean> harAktivitetTidslinje = new LocalDateTimeline<>(opptjening.getOpptjeningAktivitet().stream()
            .map(oa -> new LocalDateSegment<>(oa.getFom(), oa.getTom(), true))
            .toList(), StandardCombinators::alwaysTrueForMatch);

        LocalDateTimeline<Boolean> manglerAktivitetTidslinje = Hjelpetidslinjer.fjernHelger(opptjeningsperiode.disjoint(harAktivitetTidslinje));
        if (!manglerAktivitetTidslinje.isEmpty()) {
            logger.warn("Opptjening mangler aktiviet for {}", manglerAktivitetTidslinje.stream().map(LocalDateSegment::getLocalDateInterval).toList());
        }
    }

}
