package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Validation;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class UttaksplanRequestTest {

    private static final ObjectMapper OM = new ObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .registerModule(new JavaTimeModule());

    @Test
    public void skal_serialisere_uttaksplan_request() throws Exception {
        var req = opprettUttaksplanRequest();
        
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(req);
        assertThat(violations).isEmpty();
        
        var reqJson = OM.writeValueAsString(req);
        assertThat(reqJson).isNotEmpty();
        
        var response = OM.readValue(reqJson, UttaksplanRequest.class);
        
        assertThat(response.getBehandlingId()).isEqualTo(req.getBehandlingId());
        
        System.out.println(reqJson);
    }

    private UttaksplanRequest opprettUttaksplanRequest() {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);
        UUID behandlingId = UUID.randomUUID();
        
        var req = new UttaksplanRequest();
        req.setSaksnummer("AFC7SAK");
        req.setSøknadsperioder(List.of(new Periode(fom, tom)));
        req.setBehandlingId(behandlingId);
        
        Person barn = new Person();
        barn.setFødselsdato(fom);
        req.setBarn(barn);
        
        Person søker = new Person();
        søker.setFødselsdato(fom.minusYears(18));
        req.setSøker(søker);
        
        req.setAndrePartersBehandlinger(List.of(UUID.randomUUID(), UUID.randomUUID()));
        
        var arbeidsforhold = new UttakArbeidsforhold();
        var arbeidsforholdInfo = new UttakArbeidsforholdInfo();
        arbeidsforholdInfo.setJobberNormaltPerUke(Duration.parse("P7D"));
        arbeidsforholdInfo.setSkalJobbeProsent(new BigDecimal(50));
        arbeidsforhold.setPerioder(Map.of(new Periode(fom, tom), arbeidsforholdInfo));
        req.setArbeid(Map.of(UUID.randomUUID(), arbeidsforhold));
        
        var tilsynsbehov = new UttakTilsynsbehov();
        tilsynsbehov.setProsent(100);
        req.setTilsynsbehov(Map.of(new Periode(fom, tom), tilsynsbehov));
        
        req.setMedlemskap(Map.of(new Periode(fom, tom), new UttakMedlemskap()));
        return req;
    }
    
}
