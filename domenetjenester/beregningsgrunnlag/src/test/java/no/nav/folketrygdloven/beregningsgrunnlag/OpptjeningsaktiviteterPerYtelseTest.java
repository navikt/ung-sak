package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.input.OpptjeningsaktiviteterPerYtelse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class OpptjeningsaktiviteterPerYtelseTest {

    @Test
    public void aap_relevant_for_foreldrepenger() {
        // Act
        var opptjeningsaktiviteterPerYtelse =  new OpptjeningsaktiviteterPerYtelse(FagsakYtelseType.FORELDREPENGER);
        boolean relevant = opptjeningsaktiviteterPerYtelse.erRelevantAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null);

        // Assert
        assertThat(relevant).isTrue();
    }

    @Test
    public void dp_relevant_for_foreldrepenger() {
        // Act
        var opptjeningsaktiviteterPerYtelse =  new OpptjeningsaktiviteterPerYtelse(FagsakYtelseType.FORELDREPENGER);
        boolean relevant = opptjeningsaktiviteterPerYtelse.erRelevantAktivitet(OpptjeningAktivitetType.DAGPENGER, null);

        // Assert
        assertThat(relevant).isTrue();
    }

    @Test
    public void aap_ikke_relevant_for_svp() {
        // Act
        var opptjeningsaktiviteterPerYtelse = new OpptjeningsaktiviteterPerYtelse(FagsakYtelseType.SVANGERSKAPSPENGER);
        boolean relevant = opptjeningsaktiviteterPerYtelse.erRelevantAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null);

        // Assert
        assertThat(relevant).isFalse();
    }

    @Test
    public void dp_ikke_relevant_for_svp() {
        // Act
        var opptjeningsaktiviteterPerYtelse = new OpptjeningsaktiviteterPerYtelse(FagsakYtelseType.SVANGERSKAPSPENGER);
        boolean relevant = opptjeningsaktiviteterPerYtelse.erRelevantAktivitet(OpptjeningAktivitetType.DAGPENGER, null);

        // Assert
        assertThat(relevant).isFalse();
    }
}
