package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;
import no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtlederPersonopplysning;

@ExtendWith(CdiAwareExtension.class)
public class RegisterdataInnhentingTest {

    @Test
    void skal_hente_StartpunktUtleder_for_PersonInformasjon() throws Exception {
        var utlederOpt = EndringStartpunktUtleder.finnUtleder(CDI.current().select(EndringStartpunktUtleder.class), "PersonInformasjon", FagsakYtelseType.OMSORGSPENGER_KS);
        assertThat(utlederOpt).isPresent().get().isInstanceOf(StartpunktUtlederUtvidetRettPersonopplysning.class);
    }

    @Test
    void skal_hente_BehandlingÅrsakUtleder_for_PersonInformasjon() throws Exception {
        var utlederOpt = BehandlingÅrsakUtleder.finnUtleder(CDI.current().select(BehandlingÅrsakUtleder.class), "PersonInformasjon", FagsakYtelseType.OMSORGSPENGER_KS);
        assertThat(utlederOpt).isPresent().get().isInstanceOf(BehandlingÅrsakUtlederPersonopplysning.class);
    }
}
