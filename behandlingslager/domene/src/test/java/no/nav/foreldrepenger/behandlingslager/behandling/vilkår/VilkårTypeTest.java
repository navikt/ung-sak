package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;

public class VilkårTypeTest {

    @Test
    public void skal_hente_ut_riktig_lovreferanse_basert_på_fagsakYtelseType_engangsstønad() {
        assertThat(VilkårType.SØKERSOPPLYSNINGSPLIKT.getLovReferanse(FagsakYtelseType.ENGANGSTØNAD)).isEqualTo("§§ 21-3 og 21-7");
    }


    @Test
    public void skal_hente_ut_riktig_lovreferanse_basert_på_fagsakYtelseType_foreldrepenger() {
        assertThat(VilkårType.BEREGNINGSGRUNNLAGVILKÅR.getLovReferanse(FagsakYtelseType.FORELDREPENGER)).isEqualTo("§ 14-7");
    }


}
