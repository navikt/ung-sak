package no.nav.k9.sak.ytelse.frisinn.beregninsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FastsettBeregningsaktiviteterStegTest {

    @SuppressWarnings("rawtypes")
    @Inject
    private @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper> instances;

    @Inject
    @FagsakYtelseTypeRef("FRISINN")
    private KalkulusTjeneste tjeneste;

    @Disabled("venter på kalkulus ytelsespesifikt grunnlag")
    @Test
    public void skal_få_injected_steg() throws Exception {
        assertThat(tjeneste).isNotNull();
        assertThat(instances).isNotEmpty();
        var mapper = FagsakYtelseTypeRef.Lookup.find(instances, FagsakYtelseType.FRISINN);
        assertThat(mapper).isNotNull();

        assertThat(tjeneste.getYtelsesspesifikkMapper(FagsakYtelseType.FRISINN)).isNotNull();
    }
}
