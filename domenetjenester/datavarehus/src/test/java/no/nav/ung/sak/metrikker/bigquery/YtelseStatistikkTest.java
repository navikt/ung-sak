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

import static org.assertj.core.api.Assertions.assertThat;
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

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"OBSOLETE", "UDEFINERT"})
    void skal_avvise_ytelse_som_ikke_er_støttet(FagsakYtelseType ytelse) {
        assertThatThrownBy(() -> YtelseStatistikk.finn(ytelseStatistikk, ytelse)).isInstanceOf(IllegalArgumentException.class);
    }
}
