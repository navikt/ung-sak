package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.db.util.JpaExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.FieldSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class YtelseStatistikkTest {

    @Inject
    @Any
    private Instance<YtelseStatistikk> ytelseStatistikk;

    @Test
    void skal_finne_egen_statistikk_for_hver_støttede_ytelse() {
        assertThat(YtelseStatistikk.finn(ytelseStatistikk, FagsakYtelseType.UNGDOMSYTELSE)).isInstanceOf(UngdomsprogramytelseStatistikk.class);
        assertThat(YtelseStatistikk.finn(ytelseStatistikk, FagsakYtelseType.AKTIVITETSPENGER)).isInstanceOf(AktivitetspengerStatistikk.class);
    }

    /**
     * Kjører alle spørringene mot Flyway-skjemaet, slik at en migrering som endrer en tabell eller kolonne
     * som statistikken leser fra, feiler her i stedet for i tasken som kjører hver time i produksjon.
     */
    @ParameterizedTest
    @FieldSource("no.nav.ung.sak.metrikker.bigquery.tabeller.YtelsesTabell#STØTTEDE_YTELSER")
    void skal_kunne_kjøre_alle_spørringer_mot_databaseskjemaet(FagsakYtelseType ytelse) {
        YtelseStatistikk statistikk = YtelseStatistikk.finn(ytelseStatistikk, ytelse);

        assertThatCode(() -> {
            statistikk.hentHyppigRapporterte(LocalDateTime.now().minusHours(1));
            statistikk.hentDagligRapporterte();
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"OBSOLETE", "UDEFINERT"})
    void skal_avvise_ytelse_som_ikke_er_støttet(FagsakYtelseType ytelse) {
        assertThatThrownBy(() -> YtelseStatistikk.finn(ytelseStatistikk, ytelse)).isInstanceOf(IllegalArgumentException.class);
    }
}
