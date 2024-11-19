package no.nav.ung.sak.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.mottak.SøknadMottakTjeneste;
import no.nav.ung.sak.mottak.SøknadMottakTjenesteContainer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FordelRestTjenesteTest {

    @Inject
    private SøknadMottakTjenesteContainer søknadMottakere;

    @SuppressWarnings("rawtypes")
    @Test
    public void skal_finne_søknad_mottaker_PSB() throws Exception {
        SøknadMottakTjeneste mottakTjeneste = søknadMottakere.finnSøknadMottakerTjeneste(FagsakYtelseType.UNGDOMSYTELSE);
        assertThat(mottakTjeneste).isNotNull();
    }


}
