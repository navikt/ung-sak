package no.nav.ung.sak.formidling.klage.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.klage.regler.KlageVedtaksbrevRegler;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererInput;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class KlageVedtaksbrevTjeneste {

    private VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;
    private KlageVedtaksbrevRegler vedtaksbrevRegler;

    private static final Logger LOG = LoggerFactory.getLogger(KlageVedtaksbrevTjeneste.class);

    @Inject
    public KlageVedtaksbrevTjeneste(
        VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste,
        @BehandlingTypeRef(BehandlingType.KLAGE) KlageVedtaksbrevRegler vedtaksbrevRegler) {
        this.vedtaksbrevGenerererTjeneste = vedtaksbrevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
    }

    public KlageVedtaksbrevTjeneste() {
    }

    public GenerertBrev forhåndsvis(Long behandlingId, boolean kunHtml) {
        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandlingId);

        var vedtaksbrev = totalresultater.vedtaksbrevResultater().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Ingen dokumentmal funnet for behandligen. Resultat fra regler: " + totalresultater.safePrint()));

        VedtaksbrevGenerererInput input = new VedtaksbrevGenerererInput(behandlingId, vedtaksbrev, totalresultater.detaljertResultatTimeline(), kunHtml);
        return vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(input);
    }
}

