package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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
    private static final String PDF_MEDIA_STRING = "application/pdf";

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

    FormidlingTjeneste() {
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

    public Response lagreVedtaksbrev(VedtaksbrevValgRequestDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        if (behandling.erAvsluttet()) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Kan endre vedtaksbrev på avsluttet behandling")
                .build();
        }

        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(dto.behandlingId())
            .orElse(VedtaksbrevValgEntitet.ny(dto.behandlingId()));

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(dto.behandlingId());
        var vedtaksbrevEgenskaper = resultat.vedtaksbrevEgenskaper();

        if (!vedtaksbrevEgenskaper.kanRedigere() && dto.redigert() != null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Brevet kan ikke redigeres. ")
                .build();
        }

        if (Boolean.TRUE.equals(dto.redigert()) && (dto.redigertHtml() == null || dto.redigertHtml().isBlank())) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Redigert tekst kan ikke være tom samtidig som redigert er true")
                .build();
        }

        if ((dto.redigert() == null || !dto.redigert()) && dto.redigertHtml() != null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Kan ikke ha redigert tekst samtidig som redigert er false")
                .build();
        }

        if (!vedtaksbrevEgenskaper.kanHindre() && dto.hindret() != null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Brevet kan ikke hindres. ")
                .build();
        }


        vedtaksbrevValgEntitet.setHindret(Boolean.TRUE.equals(dto.hindret()));
        vedtaksbrevValgEntitet.setRedigert(Boolean.TRUE.equals(dto.redigert()));
        vedtaksbrevValgEntitet.setRedigertBrevHtml(dto.redigertHtml()); //TODO sanitize html!

        vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);

        return Response.ok().build();

    }

    public GenerertBrev forhåndsvisVedtaksbrev(VedtaksbrevForhåndsvisDto dto, boolean kunHtml) {
        if (dto.redigertVersjon()) {
            return brevGenerererTjeneste.genererBrevOverstyrRegler(dto.behandlingId(), kunHtml);
        }

        return brevGenerererTjeneste.genererVedtaksbrev(dto.behandlingId(), kunHtml);
    }

}

