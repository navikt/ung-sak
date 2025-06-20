package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegelResulat;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class VedtaksbrevTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;
    private final VedtaksbrevRegler vedtaksbrevRegler;
    private final VedtaksbrevValgRepository vedtaksbrevValgRepository;

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevTjeneste.class);

    @Inject
    public VedtaksbrevTjeneste(
        VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste,
        VedtaksbrevRegler vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        BehandlingRepository behandlingRepository) {
        this.vedtaksbrevGenerererTjeneste = vedtaksbrevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.behandlingRepository = behandlingRepository;
    }


    public VedtaksbrevValgResponse vedtaksbrevValg(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var erAvsluttet = behandling.erAvsluttet();

        var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId);

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("VedtaksbrevRegelResultat: {}", resultat.safePrint());

        var egenskaper = resultat.vedtaksbrevEgenskaper();

        return new VedtaksbrevValgResponse(
            egenskaper.harBrev(),
            egenskaper.kanHindre(),
            valg.map(VedtaksbrevValgEntitet::isHindret).orElse(false),
            !erAvsluttet && egenskaper.kanOverstyreHindre(),
            egenskaper.kanRedigere(),
            valg.map(VedtaksbrevValgEntitet::isRedigert).orElse(false),
            !erAvsluttet && egenskaper.kanOverstyreRediger(),
            resultat.forklaring(),
            valg.map(VedtaksbrevValgEntitet::getRedigertBrevHtml).orElse(null)
        );
    }

    public boolean måSkriveBrev(Long behandlingId) {
        var regelResulat = vedtaksbrevRegler.kjør(behandlingId).vedtaksbrevEgenskaper();
        return regelResulat.harBrev() && regelResulat.kanRedigere() && !regelResulat.kanOverstyreRediger();
    }

    public VedtaksbrevValgEntitet lagreVedtaksbrev(VedtaksbrevValgRequest dto) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        if (behandling.erAvsluttet()) {
            throw new BadRequestException("Kan ikke endre vedtaksbrev på avsluttet behandling");
        }

        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(dto.behandlingId())
            .orElse(VedtaksbrevValgEntitet.ny(dto.behandlingId()));

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(dto.behandlingId());
        var vedtaksbrevEgenskaper = resultat.vedtaksbrevEgenskaper();

        if (!vedtaksbrevEgenskaper.kanRedigere() && dto.redigert() != null) {
            throw new BadRequestException("Brevet kan ikke redigeres.");
        }

        if (!vedtaksbrevEgenskaper.kanHindre() && dto.hindret() != null) {
            throw new BadRequestException("Brevet kan ikke hindres. ");
        }

        vedtaksbrevValgEntitet.setHindret(Boolean.TRUE.equals(dto.hindret()));
        vedtaksbrevValgEntitet.setRedigert(Boolean.TRUE.equals(dto.redigert()));
        vedtaksbrevValgEntitet.rensOgSettRedigertHtml(dto.redigertHtml());

        return vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);

    }

    public GenerertBrev forhåndsvis(VedtaksbrevForhåndsvisRequest dto) {
        var kunHtml = Boolean.TRUE.equals(dto.htmlVersjon());

        if (dto.redigertVersjon() == null) {
            return vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(dto.behandlingId(), kunHtml);
        }
        if (dto.redigertVersjon()) {
            return vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), kunHtml);
        }

        return vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(dto.behandlingId(), kunHtml);
    }

    public void ryddVedTilbakeHopp(Long behandlingId) {
        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId).orElse(null);
        if (vedtaksbrevValgEntitet == null) {
            return;
        }

        vedtaksbrevValgEntitet.tilbakestillVedTilbakehopp();
        vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);

    }


}

