package no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt;

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

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Saksnummer;

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
        req.setSaksnummer(new Saksnummer("AFC7SAK"));
        req.setSøknadsperioder(List.of(new Periode(fom, tom)));
        req.setBehandlingId(behandlingId);

        Person barn = new Person(null, fom, null);
        req.setBarn(barn);

        Person søker = new Person(null, fom.minusYears(30), null);
        req.setSøker(søker);

        req.setAndrePartersSaker(List.of(new AndrePartSak(new Saksnummer("HELLO1")), new AndrePartSak(new Saksnummer("HELLO2"))));

        var uttakArbeid = new UttakArbeid();
        var arbeidsforhold = new UttakArbeidsforhold("0140821423", null, UttakArbeidType.ARBEIDSTAKER, UUID.randomUUID().toString());
        uttakArbeid.setArbeidsforhold(arbeidsforhold);
        var arbeidsforholdInfo = new UttakArbeidsforholdPeriodeInfo(Duration.parse("P7D"), new BigDecimal(50));
        uttakArbeid.setPerioder(Map.of(new Periode(fom, tom), arbeidsforholdInfo));
        req.setArbeid(List.of(uttakArbeid));

        var tilsynsbehov = new UttakTilsynsbehov();
        tilsynsbehov.setProsent(100);
        req.setTilsynsbehov(Map.of(new Periode(fom, tom), tilsynsbehov));

        req.setMedlemskap(Map.of(new Periode(fom, tom), new UttakMedlemskap()));
        return req;
    }

}
