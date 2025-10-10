package no.nav.ung.sak.formidling.klage.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.klage.regler.VedtaksbrevReglerKlage;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererInput;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjenesteImpl;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class VedtaksbrevTjenesteKlage {

    private VedtaksbrevGenerererTjenesteImpl vedtaksbrevGenerererTjeneste;
    private VedtaksbrevReglerKlage vedtaksbrevRegler;

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevTjenesteKlage.class);

    @Inject
    public VedtaksbrevTjenesteKlage(
        VedtaksbrevGenerererTjenesteImpl vedtaksbrevGenerererTjeneste,
        @BehandlingTypeRef(BehandlingType.KLAGE) VedtaksbrevReglerKlage vedtaksbrevRegler) {
        this.vedtaksbrevGenerererTjeneste = vedtaksbrevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
    }

    public VedtaksbrevTjenesteKlage() {
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

