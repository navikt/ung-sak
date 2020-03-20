package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.behandling.steg.avklarfakta.AksjonspunktUtlederForTilleggsopplysninger;

public class AksjonspunktUtlederForTilleggsopplysningerTest {

    @Test
    public void skal_returnere_aksjonspunkt_for_tilleggsopplysninger_dersom_det_er_oppgitt_i_søknad() {
        List<AksjonspunktResultat> apResultater = utledAksjonspunktResultater("tillegg");

        assertThat(apResultater).hasSize(1);
        assertThat(apResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER);
    }

    @Test
    public void skal_returnere_ingen_aksjonspunkt_for_tilleggsopplysninger_dersom_det_ikke_er_oppgitt_i_søknad() {
        List<AksjonspunktResultat> apResultater = utledAksjonspunktResultater(null);
        assertThat(apResultater).isEmpty();
    }

    private List<AksjonspunktResultat> utledAksjonspunktResultater(String tilleggsopplysninger) {
        // Arrange
        var ref = mock(BehandlingReferanse.class);
        var søknadRepository = mock(SøknadRepository.class);
        var søknad = new SøknadEntitet.Builder().medTilleggsopplysninger(tilleggsopplysninger).build();

        Mockito.when(søknadRepository.hentSøknadHvisEksisterer(Mockito.any())).thenReturn(Optional.of(søknad));

        // Act
        AksjonspunktUtlederForTilleggsopplysninger aksjonspunktUtleder = new AksjonspunktUtlederForTilleggsopplysninger(søknadRepository);
        return aksjonspunktUtleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
    }
}
