package no.nav.k9.sak.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.PleiepengerBarnSøknadInnsending;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FordelRestTjenesteTest {

    @Inject
    private FordelRestTjeneste fordelRestTjeneste;

    @SuppressWarnings("rawtypes")
    @Test
    public void skal_finne_søknad_mottaker_PSB() throws Exception {
        SøknadMottakTjeneste mottakTjeneste = fordelRestTjeneste.finnSøknadMottakerTjeneste(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        assertThat(mottakTjeneste).isNotNull();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void skal_finne_søknad_mottaker_OMP() throws Exception {
        SøknadMottakTjeneste mottakTjeneste = fordelRestTjeneste.finnSøknadMottakerTjeneste(FagsakYtelseType.OMSORGSPENGER);
        assertThat(mottakTjeneste).isNotNull();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void skal_finne_søknad_mottaker_FRISINN() throws Exception {
        SøknadMottakTjeneste mottakTjeneste = fordelRestTjeneste.finnSøknadMottakerTjeneste(FagsakYtelseType.FRISINN);
        assertThat(mottakTjeneste).isNotNull();
    }

    @Test
    public void deserialiser_pleiepengersøknad() throws Exception {

        ObjectMapper mapper = new JacksonJsonConfig().getObjectMapper();
        var json = mapper.writeValueAsString(new PleiepengerBarnSøknadInnsending());
        System.out.println(json);
        var dto = mapper.readValue(json, InnsendingInnhold.class);

        assertThat(dto).isInstanceOf(PleiepengerBarnSøknadInnsending.class);
    }
}
