package no.nav.ung.sak.domene.vedtak.ekstern;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.ung.sak.domene.iay.modell.Ytelse;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@ApplicationScoped
public class VurderOverlappendeInfotrygdYtelser {

    private static final Logger log = LoggerFactory.getLogger(VurderOverlappendeInfotrygdYtelser.class);

    // Felles
    private static final String BEH_TYPE_SAMHANDLING = "ae0119"; // Samhandling BS-FA-SP
    // Sykepenger
    private static final String SP_OPPG_TEMA = "SYK";
    private static final String SP_ANSV_ENHET_ID = "4488"; // Landsdekkende enhet for håndtering av Sykepenger
    // Foreldrepenger
    private static final String FP_OPPG_TEMA = "FOR";
    private static final String FP_ANSV_ENHET_ID = "4806"; // NFP Drammen

    private OverlappendeYtelserTjeneste overlappendeYtelserTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;

    VurderOverlappendeInfotrygdYtelser() {
        // for CDI
    }

    @Inject
    public VurderOverlappendeInfotrygdYtelser(OverlappendeYtelserTjeneste overlappendeYtelserTjeneste,
                                              OppgaveTjeneste oppgaveTjeneste,
                                              BeregningsresultatRepository beregningsresultatRepository) {
        this.overlappendeYtelserTjeneste = overlappendeYtelserTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    /**
     * Oppretter VKY-oppgaver dersom overlappende ytelser ligger i InfoTrygd (gjelder bare et subsett av ytelsene)
     */
    public void vurder(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        var eksterneYtelserSomSjekkesMot = behandling.getFagsakYtelseType().hentEksterneYtelserForOverlappSjekk();

        var overlappendeEksterneYtelser = overlappendeYtelserTjeneste.finnOverlappendeYtelser(ref, eksterneYtelserSomSjekkesMot)
            .entrySet()
            .stream()
            .filter(entry -> List.of(Fagsystem.INFOTRYGD, Fagsystem.VLSP).contains(entry.getKey().getKilde()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var tilkjentYtelseTimeline = hentTilkjentYtelsePerioder(ref);

        for (Map.Entry<Ytelse, LocalDateTimeline<Boolean>> entry : overlappendeEksterneYtelser.entrySet()) {
            var overlappendeTilkjentYtelse = tilkjentYtelseTimeline.intersection(entry.getValue());

            var overlappendePerioderBeskrivelse = overlappendeTilkjentYtelse.stream()
                .map(segment -> "" + segment.getValue().getPeriode() + " og utbetalingsgrad: " + segment.getValue().getLavestUtbetalingsgrad().orElse(BigDecimal.ZERO))
                .collect(Collectors.joining(", "));
            var beskrivelse = "K9-ytelse '" + behandling.getFagsakYtelseType().getNavn() + "' er innvilget for saksnummer " + ref.getSaksnummer() +  " med overlappende perioder: " + overlappendePerioderBeskrivelse;

            var oppgaveId = switch (entry.getKey().getYtelseType()) {
                case SYKEPENGER -> oppgaveTjeneste.opprettVkyOppgaveOverlappendeYtelse(ref, beskrivelse, SP_OPPG_TEMA, BEH_TYPE_SAMHANDLING, SP_ANSV_ENHET_ID);
                case FORELDREPENGER -> oppgaveTjeneste.opprettVkyOppgaveOverlappendeYtelse(ref, beskrivelse, FP_OPPG_TEMA, BEH_TYPE_SAMHANDLING, FP_ANSV_ENHET_ID);
                default -> throw new IllegalArgumentException("Utviklerfeil: Ingen VKY-oppgave for InfoTrygd skal sendes for " + entry.getKey().getYtelseType());
            };
            log.info("Opprettet VKY-oppgave med oppgaveId={} for overlappende InfoTrygd-ytelse: {}", oppgaveId, beskrivelse);
        }
    }

    private LocalDateTimeline<BeregningsresultatPeriode> hentTilkjentYtelsePerioder(BehandlingReferanse ref) {
        var segmenter = beregningsresultatRepository.hentBeregningsresultatAggregat(ref.getBehandlingId())
            .map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .filter(perioder -> !perioder.isEmpty())
            .map(perioder -> perioder.stream()
                .map(brPeriode -> new LocalDateSegment<>(brPeriode.getPeriode().getFomDato(), brPeriode.getPeriode().getTomDato(), brPeriode))
                .collect(Collectors.toSet()))
            .orElse(Set.of());
        return new LocalDateTimeline<>(segmenter);
    }

}
