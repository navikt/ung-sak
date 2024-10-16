package no.nav.k9.sak.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import no.nav.k9.sak.web.app.jackson.ObjectMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.mottak.SøknadMottakTjenesteContainer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.PleiepengerBarnSøknadInnsending;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FordelRestTjenesteTest {

    @Inject
    private SøknadMottakTjenesteContainer søknadMottakere;

    @SuppressWarnings("rawtypes")
    @Test
    public void skal_finne_søknad_mottaker_PSB() throws Exception {
        SøknadMottakTjeneste mottakTjeneste = søknadMottakere.finnSøknadMottakerTjeneste(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        assertThat(mottakTjeneste).isNotNull();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void skal_finne_søknad_mottaker_OMP() throws Exception {
        SøknadMottakTjeneste mottakTjeneste = søknadMottakere.finnSøknadMottakerTjeneste(FagsakYtelseType.OMSORGSPENGER);
        assertThat(mottakTjeneste).isNotNull();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void skal_finne_søknad_mottaker_FRISINN() throws Exception {
        SøknadMottakTjeneste mottakTjeneste = søknadMottakere.finnSøknadMottakerTjeneste(FagsakYtelseType.FRISINN);
        assertThat(mottakTjeneste).isNotNull();
    }

    @Test
    public void deserialiser_pleiepengersøknad() throws Exception {

        ObjectMapper mapper = ObjectMapperFactory.createBaseObjectMapper();
        var json = mapper.writeValueAsString(new PleiepengerBarnSøknadInnsending());
        System.out.println(json);
        var dto = mapper.readValue(json, InnsendingInnhold.class);

        assertThat(dto).isInstanceOf(PleiepengerBarnSøknadInnsending.class);
    }
}
