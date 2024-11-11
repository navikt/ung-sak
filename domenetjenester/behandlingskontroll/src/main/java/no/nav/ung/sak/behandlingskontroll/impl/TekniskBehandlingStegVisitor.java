package no.nav.ung.sak.behandlingskontroll.impl;

import java.util.function.UnaryOperator;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import no.nav.k9.felles.jpa.savepoint.Work;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.felles.log.trace.OpentelemetrySpanWrapper;
import no.nav.ung.sak.behandlingskontroll.BehandlingModellVisitor;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.StegProsesseringResultat;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

/**
 * Tekniske oppsett ved kjøring av et steg:<br>
 * <ul>
 * <li>Setter savepoint slik at dersom steg feiler så beholdes tidligere resultater.</li>
 * <li>Setter LOG_CONTEXT slik at ytterligere detaljer blir med i logging.</li>
 * </ul>
 */
public class TekniskBehandlingStegVisitor implements BehandlingModellVisitor {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$
    private static final OpentelemetrySpanWrapper SPAN_WRAPPER = OpentelemetrySpanWrapper.forApplikasjon();

    private final BehandlingskontrollKontekst kontekst;

    private BehandlingskontrollServiceProvider serviceProvider;

    public TekniskBehandlingStegVisitor(BehandlingskontrollServiceProvider serviceProvider,
                                        BehandlingskontrollKontekst kontekst) {
        this.serviceProvider = serviceProvider;
        this.kontekst = kontekst;
    }

    @Override
    public StegProsesseringResultat prosesser(BehandlingStegModell steg) {
        LOG_CONTEXT.add("steg", steg.getBehandlingStegType().getKode()); // NOSONAR //$NON-NLS-1$

        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        BehandlingStegTilstandSnapshot forrigeTilstand = BehandlingModellImpl.tilBehandlingsStegSnapshot(behandling.getSisteBehandlingStegTilstand());
        // lag ny for hvert steg som kjøres
        BehandlingStegVisitor stegVisitor = new BehandlingStegVisitor(serviceProvider, behandling, steg, kontekst);

        // kjøres utenfor savepoint. Ellers står vi nakne, med kun utførte steg
        stegVisitor.markerOvergangTilNyttSteg(steg.getBehandlingStegType(), forrigeTilstand);

        StegProsesseringResultat resultat = SPAN_WRAPPER.span("STEG " + steg.getBehandlingStegType().getKode(), stegAttributter(steg, behandling),
            () -> prosesserStegISavepoint(behandling, stegVisitor)
        );

        /*
         * NB: nullstiller her og ikke i finally block, siden det da fjernes før vi får logget det sammen exceptions.
         * Hele settet fjernes så i ResetLogContextHandler/Task eller tilsvarende uansett. Steg er del av koden så fanges uansett i
         * stacktrace men trengs her for å kunne ta med i log eks. på DEBUG/INFO/WARN nivå.
         */
        LOG_CONTEXT.remove("steg"); // NOSONAR //$NON-NLS-1$

        return resultat;
    }

    public static UnaryOperator<SpanBuilder> stegAttributter(BehandlingStegModell steg, Behandling behandling) {
        return spanBuilder -> spanBuilder
            .setAttribute("ytelsetype", behandling.getFagsakYtelseType().getKode())
            .setAttribute("behandlingtype", behandling.getType().getKode())
            .setAttribute("stegtype", steg.getBehandlingStegType().getKode())
            .setSpanKind(SpanKind.INTERNAL);
    }

    protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling, BehandlingStegVisitor stegVisitor) {
        // legger steg kjøring i et savepiont
        class DoInSavepoint implements Work<StegProsesseringResultat> {
            @Override
            public StegProsesseringResultat doWork() {
                StegProsesseringResultat resultat = prosesserSteg(stegVisitor);
                serviceProvider.lagreOgClear(behandling, kontekst.getSkriveLås());
                return resultat;
            }
        }

        StegProsesseringResultat resultat = serviceProvider.getTekniskRepository().doWorkInSavepoint(new DoInSavepoint());
        return resultat;
    }

    protected StegProsesseringResultat prosesserSteg(BehandlingStegVisitor stegVisitor) {
        return stegVisitor.prosesser();
    }

}
