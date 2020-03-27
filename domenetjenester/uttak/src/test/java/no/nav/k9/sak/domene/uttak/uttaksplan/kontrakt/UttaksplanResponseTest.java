package no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt;

import static javax.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.domene.uttak.uttaksplan.UttakUtbetalingsgrad;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;

public class UttaksplanResponseTest {

    private static final ObjectMapper OM = new ObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .registerModule(new JavaTimeModule());

    @Test
    public void skal_serialisere_deserialisere_UttaksplanResponse() throws Exception {
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusDays(10);
        
        var arbeidsforhold = new UttakArbeidsforhold("0140821423", null, UttakArbeidType.ARBEIDSTAKER, UUID.randomUUID().toString());
        var uttaksperiodeInfo = new InnvilgetUttaksplanperiode(100, List.of(new UttakUtbetalingsgrad(arbeidsforhold, new BigDecimal("100.00"))));
        var uttaksplan = new Uttaksplan(Map.of(new Periode(fom, tom ), uttaksperiodeInfo));
        
        @SuppressWarnings("resource")
        var validator = buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(uttaksplan);
        assertThat(violations).isEmpty();

        var reqJson = OM.writeValueAsString(uttaksplan);
        assertThat(reqJson).isNotEmpty();
        System.out.println(reqJson);

        var response = OM.readValue(reqJson, Uttaksplan.class);
        assertThat(response.getPerioder()).hasSameSizeAs(uttaksplan.getPerioder());
        
    }

}
