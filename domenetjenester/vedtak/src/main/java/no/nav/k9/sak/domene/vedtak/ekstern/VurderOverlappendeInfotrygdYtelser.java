package no.nav.k9.sak.domene.vedtak.ekstern;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

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

    VurderOverlappendeInfotrygdYtelser() {
        // for CDI
    }

    @Inject
    public VurderOverlappendeInfotrygdYtelser(OverlappendeYtelserTjeneste overlappendeYtelserTjeneste,
                                              OppgaveTjeneste oppgaveTjeneste) {
        this.overlappendeYtelserTjeneste = overlappendeYtelserTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
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
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        for (Map.Entry<Ytelse, NavigableSet<LocalDateInterval>> entry : overlappendeEksterneYtelser.entrySet()) {
            var ytelse = entry.getKey();
            var perioder = entry.getValue();
            var beskrivelse = "Ytelse=" + ytelse.getYtelseType() + ", saksnummer=" + ytelse.getSaksnummer() +  ", antall ytelser anvist=" + ytelse.getYtelseAnvist().size() + ", perioder=" + perioder;

            var oppgaveId = switch (ytelse.getYtelseType()) {
                case SYKEPENGER -> oppgaveTjeneste.opprettVkyOppgaveOverlappendeYtelse(ref, beskrivelse, SP_OPPG_TEMA, BEH_TYPE_SAMHANDLING, SP_ANSV_ENHET_ID);
                case FORELDREPENGER -> oppgaveTjeneste.opprettVkyOppgaveOverlappendeYtelse(ref, beskrivelse, FP_OPPG_TEMA, BEH_TYPE_SAMHANDLING, FP_ANSV_ENHET_ID);
                default -> throw new IllegalArgumentException("Utviklerfeil: Ingen VKY-oppgave for InfoTrygd skal sendes for " + ytelse.getYtelseType());
            };
            log.info("Opprettet VKY-oppgave med oppgaveId={} for overlappende InfoTrygd-ytelse: {}", oppgaveId, beskrivelse);
        }
    }

}
