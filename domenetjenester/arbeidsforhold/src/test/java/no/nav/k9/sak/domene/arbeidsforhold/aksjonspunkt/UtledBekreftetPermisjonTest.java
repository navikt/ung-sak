package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.sak.domene.iay.modell.BekreftetPermisjon;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PermisjonDto;

public class UtledBekreftetPermisjonTest {

    private static final LocalDate DAGENS_DATO = LocalDate.now();

    @Test
    public void skal_utlede_bekreftet_permisjon_med_status_UGYLDIGE_PERIODER_og_periode_lik_null(){
        // Arrange
        var permisjonDto1 = new PermisjonDto(DAGENS_DATO, DAGENS_DATO.plusWeeks(1), BigDecimal.valueOf(100), PermisjonsbeskrivelseType.PERMISJON);
        var permisjonDto2 = new PermisjonDto(DAGENS_DATO, null, BigDecimal.valueOf(100), PermisjonsbeskrivelseType.PERMISJON);
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        arbeidsforholdDto.setPermisjoner(List.of(permisjonDto1, permisjonDto2));
        // Act
        BekreftetPermisjon bekreftetPermisjon = UtledBekreftetPermisjon.utled(arbeidsforholdDto);
        // Assert
        assertThat(bekreftetPermisjon.getStatus()).isEqualTo(BekreftetPermisjonStatus.UGYLDIGE_PERIODER);
        assertThat(bekreftetPermisjon.getPeriode()).isNull();
    }

    @Test
    public void skal_utlede_bekreftet_permisjon_med_status_BRUK_PERMISJON_og_periode_med_tom_definert(){
        // Arrange
        var permisjonDto = new PermisjonDto(DAGENS_DATO, DAGENS_DATO.plusWeeks(1), BigDecimal.valueOf(100), PermisjonsbeskrivelseType.PERMISJON);
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        arbeidsforholdDto.setPermisjoner(List.of(permisjonDto));
        // Act
        BekreftetPermisjon bekreftetPermisjon = UtledBekreftetPermisjon.utled(arbeidsforholdDto);
        // Assert
        assertThat(bekreftetPermisjon.getStatus()).isEqualTo(BekreftetPermisjonStatus.BRUK_PERMISJON);
        assertThat(bekreftetPermisjon.getPeriode().getFomDato()).isEqualTo(DAGENS_DATO);
        assertThat(bekreftetPermisjon.getPeriode().getTomDato()).isEqualTo(DAGENS_DATO.plusWeeks(1));
    }

    @Test
    public void skal_utlede_bekreftet_permisjon_med_status_BRUK_PERMISJON_og_periode_med_tom_lik_null(){
        // Arrange
        var permisjonDto = new PermisjonDto(DAGENS_DATO, null, BigDecimal.valueOf(100), PermisjonsbeskrivelseType.PERMISJON);
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        arbeidsforholdDto.setPermisjoner(List.of(permisjonDto));
        // Act
        BekreftetPermisjon bekreftetPermisjon = UtledBekreftetPermisjon.utled(arbeidsforholdDto);
        // Assert
        assertThat(bekreftetPermisjon.getStatus()).isEqualTo(BekreftetPermisjonStatus.BRUK_PERMISJON);
        assertThat(bekreftetPermisjon.getPeriode().getFomDato()).isEqualTo(DAGENS_DATO);
        assertThat(bekreftetPermisjon.getPeriode().getTomDato()).isEqualTo(TIDENES_ENDE);
    }

    @Test(expected = IllegalStateException.class)
    public void skal_kaste_exception_hvis_utledet_status_er_udefinert(){
        // Arrange
        AvklarArbeidsforholdDto arbeidsforholdDto = new AvklarArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(null);
        arbeidsforholdDto.setPermisjoner(List.of());
        // Act
        UtledBekreftetPermisjon.utled(arbeidsforholdDto);
    }

}
