package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.log.audit.Auditdata;
import no.nav.k9.felles.log.audit.Auditlogger;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EksternSystemAuditloggerTest {

    private static final String PRE_AUTHORIZED_APPS = """
        [
          {"name": "dev-gcp:aap:behandlingsflyt", "clientId": "klient-aap"},
          {"name": "dev-gcp:bidrag:bidrag-grunnlag", "clientId": "klient-bidrag"},
          {"name": "dev-gcp:bidrag:bidrag-grunnlag-alias", "clientId": "klient-bidrag"}
        ]
        """;

    private final Auditlogger auditlogger = mock(Auditlogger.class);

    @Test
    void skal_slå_opp_systemnavn_fra_klient_id() {
        var logger = new EksternSystemAuditlogger(auditlogger, () -> "klient-aap", PRE_AUTHORIZED_APPS);

        assertThat(logger.kallendeSystem()).isEqualTo("dev-gcp:aap:behandlingsflyt");
    }

    @Test
    void skal_ta_med_alle_navn_når_klient_id_har_flere_navn() {
        var logger = new EksternSystemAuditlogger(auditlogger, () -> "klient-bidrag", PRE_AUTHORIZED_APPS);

        assertThat(logger.kallendeSystem()).isEqualTo("dev-gcp:bidrag:bidrag-grunnlag,dev-gcp:bidrag:bidrag-grunnlag-alias");
    }

    @Test
    void skal_bruke_klient_id_når_systemet_ikke_er_pre_autorisert() {
        var logger = new EksternSystemAuditlogger(auditlogger, () -> "ukjent-klient", PRE_AUTHORIZED_APPS);

        assertThat(logger.kallendeSystem()).isEqualTo("ukjent-klient");
    }

    @Test
    void skal_bruke_klient_id_når_pre_authorized_apps_mangler_eller_er_ugyldig() {
        assertThat(new EksternSystemAuditlogger(auditlogger, () -> "klient-aap", null).kallendeSystem()).isEqualTo("klient-aap");
        assertThat(new EksternSystemAuditlogger(auditlogger, () -> "klient-aap", "ikke json").kallendeSystem()).isEqualTo("klient-aap");
    }

    @Test
    void skal_auditlogge_oppslaget_med_system_person_og_endepunkt() {
        when(auditlogger.getDefaultVendor()).thenReturn("k9");
        when(auditlogger.getDefaultProduct()).thenReturn("ung-sak");
        var logger = new EksternSystemAuditlogger(auditlogger, () -> "klient-aap", PRE_AUTHORIZED_APPS);

        var fnr = new FiktiveFnr().nesteFnr();

        logger.loggOppslag("dev-gcp:aap:behandlingsflyt", "/ekstern/ytelse/v1/vedtak", new PersonIdent(fnr));

        var captor = ArgumentCaptor.forClass(Auditdata.class);
        verify(auditlogger).logg(captor.capture());
        assertThat(captor.getValue().toString())
            .startsWith("CEF:0|k9|ung-sak|1.0|audit:access|ABAC Sporingslogg|INFO|")
            .contains(
                "suid=dev-gcp:aap:behandlingsflyt",
                "duid=" + fnr,
                "request=/ekstern/ytelse/v1/vedtak",
                "requestContext=tjeneste.kan.kun.kalles.av.eksterne.systemer",
                "act=read",
                "end=");
    }

    @Test
    void skal_feile_heller_enn_å_auditlogge_uten_berørt_person() {
        var logger = new EksternSystemAuditlogger(auditlogger, () -> "klient-aap", PRE_AUTHORIZED_APPS);

        assertThatThrownBy(() -> logger.loggOppslag("dev-gcp:aap:behandlingsflyt", "/ekstern/ytelse/v1/vedtak", new PersonIdent("12345678901")))
            .isInstanceOf(NullPointerException.class)
            .hasMessageNotContaining("12345678901");
        verify(auditlogger, never()).logg(ArgumentMatchers.any());
    }
}
