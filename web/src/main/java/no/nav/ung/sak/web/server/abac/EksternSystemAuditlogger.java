package no.nav.ung.sak.web.server.abac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.log.audit.Auditdata;
import no.nav.k9.felles.log.audit.AuditdataHeader;
import no.nav.k9.felles.log.audit.Auditlogger;
import no.nav.k9.felles.log.audit.CefField;
import no.nav.k9.felles.log.audit.CefFieldName;
import no.nav.k9.felles.log.audit.EventClassId;
import no.nav.k9.felles.sikkerhet.oidc.azure.PreAuthorizedApp;
import no.nav.k9.sikkerhet.oidc.config.DefaultJsonMapper;
import no.nav.k9.sikkerhet.oidc.token.context.ContextAwareTokenProvider;
import no.nav.k9.sikkerhet.oidc.token.internal.JwtUtil;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Auditlogger oppslag fra eksterne systemer ({@code EKSTERN_SYSTEM}).
 * <p>
 * k9-felles auditlogger ikke systemkall ({@code TilgangType.SYSTEM}), så endepunkter som utleverer personopplysninger til
 * andre systemer må logge selv. Formatet er det samme som {@code AbacAuditlogger} bruker, med systemnavnet i {@code suid}.
 * <p>
 * Systemnavnet slås opp fra klient-ID-en (azp) i {@code AZURE_APP_PRE_AUTHORIZED_APPS}, på samme måte som PepImpl gjør
 * ved tilgangskontrollen. {@code azp_name} i tokenet brukes ikke, siden det bare er ment for visning.
 */
@Dependent
public class EksternSystemAuditlogger {

    static final String RESSURSTYPE = "tjeneste.kan.kun.kalles.av.eksterne.systemer";

    private static final Logger log = LoggerFactory.getLogger(EksternSystemAuditlogger.class);

    private final Auditlogger auditlogger;
    private final Supplier<String> azpSupplier;
    private final List<PreAuthorizedApp> preAuthorizedApps;

    @Inject
    public EksternSystemAuditlogger(Auditlogger auditlogger,
                                    ContextAwareTokenProvider tokenProvider,
                                    @KonfigVerdi(value = "AZURE_APP_PRE_AUTHORIZED_APPS", required = false) String preAuthorizedApps) {
        this(auditlogger, () -> JwtUtil.getAzp(tokenProvider.getToken().getToken()), preAuthorizedApps);
    }

    EksternSystemAuditlogger(Auditlogger auditlogger, Supplier<String> azpSupplier, String preAuthorizedApps) {
        this.auditlogger = auditlogger;
        this.azpSupplier = azpSupplier;
        this.preAuthorizedApps = parse(preAuthorizedApps);
    }

    /**
     * Navnet ({@code <cluster>:<namespace>:<app>}) på systemet som kaller. Faller tilbake på klient-ID-en dersom den ikke
     * finnes blant de pre-autoriserte appene, selv om PepImpl da normalt allerede har avvist kallet.
     */
    public String kallendeSystem() {
        var azp = azpSupplier.get();
        var navn = preAuthorizedApps.stream()
            .filter(app -> Objects.equals(azp, app.clientId()) && app.name() != null)
            .map(PreAuthorizedApp::name)
            .sorted()
            .distinct()
            .collect(Collectors.joining(","));
        return navn.isEmpty() ? azp : navn;
    }

    /**
     * Logger at systemet har slått opp opplysninger om personen. Skal kalles etter at oppslaget er gjort, også når det ikke
     * ga treff, og ikke når oppslaget feilet.
     *
     * @param request stien til endepunktet, f.eks. {@code /ekstern/ytelse/v1/vedtak}
     */
    public void loggOppslag(String kallendeSystem, String request, PersonIdent berørtPerson) {
        // getIdent() gir null for ugyldig fnr, og da ville duid stille blitt utelatt fra auditloggen
        var fnr = Objects.requireNonNull(berørtPerson.getIdent(), "Kan ikke auditlogge oppslag uten gyldig fnr eller d-nummer");
        var header = new AuditdataHeader.Builder()
            .medVendor(auditlogger.getDefaultVendor())
            .medProduct(auditlogger.getDefaultProduct())
            .medEventClassId(EventClassId.AUDIT_ACCESS)
            .medName("ABAC Sporingslogg")
            .medSeverity("INFO")
            .build();
        var felter = Set.of(
            new CefField(CefFieldName.EVENT_TIME, System.currentTimeMillis()),
            new CefField(CefFieldName.USER_ID, kallendeSystem),
            new CefField(CefFieldName.BERORT_BRUKER_ID, fnr),
            new CefField(CefFieldName.REQUEST, request),
            new CefField(CefFieldName.ABAC_RESOURCE_TYPE, RESSURSTYPE),
            new CefField(CefFieldName.ABAC_ACTION, "read"));
        auditlogger.logg(new Auditdata(header, felter));
    }

    private static List<PreAuthorizedApp> parse(String preAuthorizedApps) {
        if (preAuthorizedApps == null || preAuthorizedApps.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(DefaultJsonMapper.getObjectMapper().readValue(preAuthorizedApps, new TypeReference<List<PreAuthorizedApp>>() {
            }));
        } catch (JsonProcessingException e) {
            log.warn("Klarte ikke å lese AZURE_APP_PRE_AUTHORIZED_APPS. Bruker klient-ID som systemnavn i auditloggen.");
            return List.of();
        }
    }
}
