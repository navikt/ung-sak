package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class FormidlingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BrevGenerererTjeneste brevGenerererTjeneste;
    private VedtaksbrevRegler vedtaksbrevRegler;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    private static final Logger LOG = LoggerFactory.getLogger(FormidlingTjeneste.class);

    @Inject
    public FormidlingTjeneste(
        BrevGenerererTjeneste brevGenerererTjeneste,
        VedtaksbrevRegler vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        BehandlingRepository behandlingRepository) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.behandlingRepository = behandlingRepository;
    }


    public VedtaksbrevValgDto vedtaksbrevValg(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var erAvsluttet = behandling.erAvsluttet();

        var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId);

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("VedtaksbrevRegelResultat: {}", resultat.safePrint());

        var egenskaper = resultat.vedtaksbrevEgenskaper();

        return new VedtaksbrevValgDto(
            egenskaper.harBrev(),
            null,
            false,
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

    public VedtaksbrevValgEntitet lagreVedtaksbrev(VedtaksbrevValgRequestDto dto) {
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
        vedtaksbrevValgEntitet.setRedigertBrevHtml(dto.redigertHtml()); //TODO sanitize html!

        return vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);

    }

    public GenerertBrev forhåndsvisVedtaksbrev(VedtaksbrevForhåndsvisDto dto, boolean kunHtml) {
        if (dto.redigertVersjon() == null) {
            return brevGenerererTjeneste.genererVedtaksbrevForBehandling(dto.behandlingId(), kunHtml);
        }
        if (dto.redigertVersjon()) {
            return brevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), kunHtml);
        }

        return brevGenerererTjeneste.genererAutomatiskVedtaksbrev(dto.behandlingId(), kunHtml);
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

