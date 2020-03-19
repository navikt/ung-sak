package no.nav.k9.sak.web.app.tjenester.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.web.app.tjenester.kodeverk.HentKodeverkTjeneste;

public class HentKodeverkTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste enhetsTjeneste = new BehandlendeEnhetTjeneste();

    @Test
    public void skal_filtere_arbeidtyper() {
        var kodeverk = new HentKodeverkTjeneste(enhetsTjeneste);

        var resultat = kodeverk.hentGruppertKodeliste();
        var arbeidType = resultat.get("ArbeidType");

        assertThat(arbeidType).hasSize(6);
    }
}
