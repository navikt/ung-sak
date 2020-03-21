package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FastsettBeregningsaktiviteterStegTest {

    @Inject
    private @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> instances;
    
    @Inject 
    @FagsakYtelseTypeRef
    @BehandlingStegRef(kode = "FASTSETT_STP_BER")
    @BehandlingTypeRef
    private FastsettBeregningsaktiviteterSteg steg;
    
    @Test
    public void skal_få_injected_steg() throws Exception {
        assertThat(steg).isNotNull();
        assertThat(instances).isNotEmpty();
        var mapper = FagsakYtelseTypeRef.Lookup.find(instances, FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        assertThat(mapper).isNotNull();
        
        assertThat(steg.getYtelsesspesifikkMapper(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)).isNotNull();
    }
}
