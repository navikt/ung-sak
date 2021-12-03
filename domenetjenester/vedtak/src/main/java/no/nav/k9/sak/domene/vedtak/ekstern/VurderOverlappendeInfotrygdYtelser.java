package no.nav.k9.sak.domene.vedtak.ekstern;

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

    // TODO: Riktige koder
    private static final String SP_OPPG_TEMA = "STO";
    private static final String SP_BEH_TEMA = "ab0271";
    private static final String SP_ANSV_ENHET_ID = "4488"; // Landsdekkende enhet for håndtering av Sykepenger

    private static final String FP_OPPG_TEMA = "STO";
    private static final String FP_BEH_TEMA = "ab0326";
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
        var ytelseTyperSomSjekkesMot = behandling.getFagsakYtelseType().hentEksterneYtelserForOverlappSjekk();

        var overlappendeYtelserInfoTrygd = overlappendeYtelserTjeneste.finnOverlappendeYtelser(ref, ytelseTyperSomSjekkesMot)
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().getKilde() == Fagsystem.INFOTRYGD)
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        for (Map.Entry<Ytelse, NavigableSet<LocalDateInterval>> entry : overlappendeYtelserInfoTrygd.entrySet()) {
            var ytelse = entry.getKey();
            var perioder = entry.getValue();
            var beskrivelse = "Ytelse=" + ytelse.getYtelseType() + ", saksnummer=" + ytelse.getSaksnummer() + ", perioder=" + perioder;

            var oppgaveId = switch (ytelse.getYtelseType()) {
                case SYKEPENGER -> oppgaveTjeneste.opprettVkyOppgaveOverlappendeYtelse(ref, beskrivelse, SP_OPPG_TEMA, SP_BEH_TEMA, SP_ANSV_ENHET_ID);
                case FORELDREPENGER -> oppgaveTjeneste.opprettVkyOppgaveOverlappendeYtelse(ref, beskrivelse, FP_OPPG_TEMA, FP_BEH_TEMA, FP_ANSV_ENHET_ID);
                default -> throw new IllegalArgumentException("Utviklerfeil: Ingen VKY-oppgave for InfoTrygd skal sendes for " + ytelse.getYtelseType());
            };
            log.info("Opprettet VKY-oppgave med oppgaveId={} for overlappende InfoTrygd-ytelse: {}", oppgaveId, beskrivelse);
        }
    }

}
