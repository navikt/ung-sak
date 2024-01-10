package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.kontrakt.vedtak.VedtaksbrevOverstyringDto;

@Dependent
public class FrisinnVedtaksvarselTjeneste {
    private VedtakVarselRepository vedtakVarselRepository;

    @Inject
    public FrisinnVedtaksvarselTjeneste(VedtakVarselRepository vedtakVarselRepository) {
        this.vedtakVarselRepository = vedtakVarselRepository;
    }

    void oppdaterVedtaksvarsel(VedtaksbrevOverstyringDto dto, Long behandlingId, FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType != FagsakYtelseType.FRISINN) {
            throw new IllegalArgumentException("Tjenesten skal bare brukes for FRISINN, fikk " + fagsakYtelseType);
        }

        vedtakVarselRepository.hentHvisEksisterer(behandlingId).ifPresent(v -> {
            v.setRedusertUtbetalingÅrsaker(dto.getRedusertUtbetalingÅrsaker());
            if (dto.isSkalUndertrykkeBrev()) {
                v.setVedtaksbrev(Vedtaksbrev.INGEN);
            }
            vedtakVarselRepository.lagre(behandlingId, v);
        });
    }
}
