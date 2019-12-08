package no.nav.foreldrepenger.domene.vedtak.innsyn;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;

@ApplicationScoped
public class VedtakInnsynTjeneste {

    @SuppressWarnings("unused")
    private LagretVedtakRepository lagretVedtakRepository;

    public VedtakInnsynTjeneste() {
        //CDI
    }

    @Inject
    public VedtakInnsynTjeneste(LagretVedtakRepository lagretVedtakRepository) {
        this.lagretVedtakRepository = lagretVedtakRepository;
    }

    public String hentVedtaksdokument(@SuppressWarnings("unused") Long behandlingId) {
        // FIXME K9 vedtak dokument
        return "<html><body>TODO : Vedtakstruktur</body></html>";
    }

}
