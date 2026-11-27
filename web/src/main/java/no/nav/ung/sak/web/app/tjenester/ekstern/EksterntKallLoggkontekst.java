package no.nav.ung.sak.web.app.tjenester.ekstern;

import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.sikkerhet.context.SubjectHandler;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Legger konsument, endepunkt og saksnummer i MDC for kall fra eksterne systemer, slik at kallene kan spores i loggene.
 * Samme kontekst som resten av ung-sak, så saksnummeret havner i {@code prosess_saksnummer}. Aldri fnr eller aktørId.
 */
public final class EksterntKallLoggkontekst {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

    private EksterntKallLoggkontekst() {
    }

    /**
     * Konsumenten er {@code azp_name} fra tokenet ({@code cluster:namespace:app}). {@code consumerId} i MDC kan kalleren
     * styre selv med headeren {@code Nav-Consumer-Id}, så den brukes ikke.
     */
    public static void leggTilKall(String endepunkt) {
        LOG_CONTEXT.add("konsument", SubjectHandler.getSubjectHandler().getUid());
        LOG_CONTEXT.add("endepunkt", endepunkt);
    }

    public static void leggTilSaksnumre(Collection<String> saksnumre) {
        if (!saksnumre.isEmpty()) {
            LOG_CONTEXT.add("saksnummer", saksnumre.stream().distinct().sorted().collect(Collectors.joining(",")));
        }
    }
}
