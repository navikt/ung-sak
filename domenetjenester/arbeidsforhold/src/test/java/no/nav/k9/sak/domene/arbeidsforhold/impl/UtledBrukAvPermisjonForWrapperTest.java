package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.k9.sak.domene.iay.modell.BekreftetPermisjon;

public class UtledBrukAvPermisjonForWrapperTest {

    @Test
    public void skal_utlede_bruk_permisjon_lik_true_hvis_bekreftet_permisjon_status_er_BRUK_PERMISJON(){
        // Arrange
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = Optional.of(new BekreftetPermisjon(BekreftetPermisjonStatus.BRUK_PERMISJON));
        // Act
        Boolean brukPermisjon = UtledBrukAvPermisjonForWrapper.utled(bekreftetPermisjonOpt);
        // Assert
        assertThat(brukPermisjon).isTrue();
    }

    @Test
    public void skal_utlede_bruk_permisjon_lik_false_hvis_bekreftet_permisjon_status_er_IKKE_BRUK_PERMISJON(){
        // Arrange
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = Optional.of(new BekreftetPermisjon(BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON));
        // Act
        Boolean brukPermisjon = UtledBrukAvPermisjonForWrapper.utled(bekreftetPermisjonOpt);
        // Assert
        assertThat(brukPermisjon).isFalse();
    }

    @Test
    public void skal_utlede_bruk_permisjon_lik_false_hvis_bekreftet_permisjon_status_er_UGYLDIGE_PERIODER(){
        // Arrange
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = Optional.of(new BekreftetPermisjon(BekreftetPermisjonStatus.UGYLDIGE_PERIODER));
        // Act
        Boolean brukPermisjon = UtledBrukAvPermisjonForWrapper.utled(bekreftetPermisjonOpt);
        // Assert
        assertThat(brukPermisjon).isFalse();
    }

    @Test
    public void skal_utlede_bruk_permisjon_lik_null_hvis_bekreftet_permisjon_status_er_UDEFINERT(){
        // Arrange
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = Optional.of(new BekreftetPermisjon(BekreftetPermisjonStatus.UDEFINERT));
        // Act
        Boolean brukPermisjon = UtledBrukAvPermisjonForWrapper.utled(bekreftetPermisjonOpt);
        // Assert
        assertThat(brukPermisjon).isNull();
    }

    @Test
    public void skal_utlede_bruk_permisjon_lik_null_hvis_bekreftet_permisjon_er_empty() {
        // Arrange
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = Optional.empty();
        // Act
        Boolean brukPermisjon = UtledBrukAvPermisjonForWrapper.utled(bekreftetPermisjonOpt);
        // Assert
        assertThat(brukPermisjon).isNull();
    }

}
