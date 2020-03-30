package no.nav.k9.sak.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FordelRestTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    
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
}
