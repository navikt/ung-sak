package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.DefaultLagFortsettRequest;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.LagFortsettRequest;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@ExtendWith(CdiAwareExtension.class)
class PSBVurderRefusjonLagFortsettRequestTest {

    @Inject
    @Any
    private Instance<LagFortsettRequest> tjenester;

    @Test
    void skal_finne_egen_implementasjon_for_psb_vurder_refusjon() {
        var ytelseType = FagsakYtelseType.PSB;
        var behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
        var behandlingStegType = BehandlingStegType.VURDER_REF_BERGRUNN;
        var lagFortsettRequest = LagFortsettRequest.finnTjeneste(tjenester, ytelseType, behandlingType, behandlingStegType);
        assertThat(lagFortsettRequest.getClass().getSimpleName()).isEqualTo(getSimpleName(PleiepengerVurderRefusjonLagFortsettRequest.class));
    }

    @Test
    void skal_finne_default_implementasjon_for_psb_vurder_vilkår() {
        var ytelseType = FagsakYtelseType.PSB;
        var behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
        var behandlingStegType = BehandlingStegType.VURDER_VILKAR_BERGRUNN;
        var lagFortsettRequest = LagFortsettRequest.finnTjeneste(tjenester, ytelseType, behandlingType, behandlingStegType);
        assertThat(lagFortsettRequest.getClass().getSimpleName()).isEqualTo(getSimpleName(DefaultLagFortsettRequest.class));
    }

    private <T> String getSimpleName(Class<T> requestClass) {
        String weldClientSuffix = "$Proxy$_$$_WeldClientProxy";
        return requestClass.getSimpleName() + weldClientSuffix;
    }

}
