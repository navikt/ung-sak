package no.nav.ung.ytelse.ungdomsprogramytelsen.kontrollbehandling;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.ung.sak.domene.iverksett.YtelseOpprettProsessTaskIverksett;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegel;
import no.nav.ung.sak.formidling.vedtak.regler.YtelseVedtaksbrevRegler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tjenester som slås opp på behandlingstype, og som kontrollbehandlingen trenger for å fatte og iverksette vedtak.
 */
@CdiDbAwareTest
class KontrollbehandlingTypeoppslagTest {

    @Inject
    @Any
    private Instance<OpprettProsessTaskIverksett> iverksettTjenester;

    @Inject
    @Any
    private Instance<VedtaksbrevRegel> vedtaksbrevRegler;

    @Test
    void kontrollbehandling_skal_iverksettes_som_ytelsesbehandling() {
        var tjeneste = BehandlingTypeRef.Lookup.find(OpprettProsessTaskIverksett.class, iverksettTjenester, FagsakYtelseType.UNGDOMSYTELSE, BehandlingType.KONTROLL);

        assertThat(tjeneste).isPresent();
        assertThat(tjeneste.get()).isInstanceOf(YtelseOpprettProsessTaskIverksett.class);
    }

    @Test
    void kontrollbehandling_skal_bruke_vedtaksbrevreglene_for_ytelsesbehandling() {
        var regel = VedtaksbrevRegel.hentVedtaksbrevRegel(vedtaksbrevRegler, FagsakYtelseType.UNGDOMSYTELSE, BehandlingType.KONTROLL);

        assertThat(regel).isInstanceOf(YtelseVedtaksbrevRegler.class);
    }
}
