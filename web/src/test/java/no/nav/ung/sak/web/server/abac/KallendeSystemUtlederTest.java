package no.nav.ung.sak.web.server.abac;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KallendeSystemUtlederTest {

    private static final String PRE_AUTHORIZED_APPS = """
        [
          {"name": "dev-gcp:aap:behandlingsflyt", "clientId": "klient-aap"},
          {"name": "dev-gcp:bidrag:bidrag-grunnlag", "clientId": "klient-bidrag"},
          {"name": "dev-gcp:bidrag:bidrag-grunnlag-alias", "clientId": "klient-bidrag"}
        ]
        """;

    @Test
    void skal_slå_opp_systemnavn_fra_klient_id() {
        var utleder = new KallendeSystemUtleder(() -> "klient-aap", PRE_AUTHORIZED_APPS);

        assertThat(utleder.kallendeSystem()).isEqualTo("dev-gcp:aap:behandlingsflyt");
    }

    @Test
    void skal_ta_med_alle_navn_når_klient_id_har_flere_navn() {
        var utleder = new KallendeSystemUtleder(() -> "klient-bidrag", PRE_AUTHORIZED_APPS);

        assertThat(utleder.kallendeSystem()).isEqualTo("dev-gcp:bidrag:bidrag-grunnlag,dev-gcp:bidrag:bidrag-grunnlag-alias");
    }

    @Test
    void skal_bruke_klient_id_når_systemet_ikke_er_pre_autorisert() {
        var utleder = new KallendeSystemUtleder(() -> "ukjent-klient", PRE_AUTHORIZED_APPS);

        assertThat(utleder.kallendeSystem()).isEqualTo("ukjent-klient");
    }

    @Test
    void skal_bruke_klient_id_når_pre_authorized_apps_mangler_eller_er_ugyldig() {
        assertThat(new KallendeSystemUtleder(() -> "klient-aap", null).kallendeSystem()).isEqualTo("klient-aap");
        assertThat(new KallendeSystemUtleder(() -> "klient-aap", "ikke json").kallendeSystem()).isEqualTo("klient-aap");
    }
}
