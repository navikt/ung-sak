package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.klage.regler.VedtaksbrevReglerKlage;
import no.nav.ung.sak.formidling.klage.vedtak.VedtaksbrevGenerererTjenesteKlage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class VedtaksbrevTjenesteKlage {

    private final VedtaksbrevGenerererTjenesteKlage vedtaksbrevGenerererTjeneste;
    private final VedtaksbrevReglerKlage vedtaksbrevRegler;

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevTjenesteKlage.class);

    @Inject
    public VedtaksbrevTjenesteKlage(
        @Any VedtaksbrevGenerererTjenesteKlage vedtaksbrevGenerererTjeneste,
        @Any VedtaksbrevReglerKlage vedtaksbrevRegler) {
        this.vedtaksbrevGenerererTjeneste = vedtaksbrevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
    }

    public GenerertBrev forhåndsvis(Behandling behandling) {

        var regelResultat = vedtaksbrevRegler.kjør(behandling.getId());
        var dokumentMalType = regelResultat.vedtaksbrevResultater().getFirst().dokumentMalType();
        return vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(behandling, dokumentMalType, false);
    }
}

