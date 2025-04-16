package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.ung.sak.formidling.vedtaksbrevvalg.VedtaksbrevValgEntitet;
import no.nav.ung.sak.formidling.vedtaksbrevvalg.VedtaksbrevValgRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class FormidlingTjeneste {

    private BrevGenerererTjeneste brevGenerererTjeneste;
    private VedtaksbrevRegler vedtaksbrevRegler;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    private static final Logger LOG = LoggerFactory.getLogger(FormidlingTjeneste.class);
    private static final String PDF_MEDIA_STRING = "application/pdf";

    @Inject
    public FormidlingTjeneste(
        BrevGenerererTjeneste brevGenerererTjeneste,
        VedtaksbrevRegler vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }

    FormidlingTjeneste() {
    }


    public VedtaksbrevValgDto vedtaksbrevOperasjoner(BehandlingIdDto dto) {

        var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(dto.getBehandlingId());

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(Long.valueOf(dto.getId()));
        LOG.info("VedtaksbrevRegelResultat: {}", resultat.safePrint());

        var egenskaper = resultat.vedtaksbrevEgenskaper();

        return new VedtaksbrevValgDto(
            egenskaper.harBrev(),
            null,
            false,
            egenskaper.kanHindre(),
            valg.map(VedtaksbrevValgEntitet::isHindret).orElse(false),
            egenskaper.kanOverstyreHindre(),
            egenskaper.kanRedigere(),
            valg.map(VedtaksbrevValgEntitet::isRedigert).orElse(false),
            egenskaper.kanOverstyreRediger(),
            resultat.forklaring(),
            valg.map(VedtaksbrevValgEntitet::getRedigertBrevHtml).orElse(null)
        );
    }

    public Response lagreVedtaksbrev(VedtaksbrevValgRequestDto dto) {

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

    public Response forhåndsvisVedtaksbrev(VedtaksbrevForhåndsvisDto dto, String mediaTypeReq) {

        GenerertBrev generertBrev = mediaTypeReq.equals(MediaType.TEXT_HTML) ?
            brevGenerererTjeneste.genererVedtaksbrevKunHtml(dto.behandlingId()) :
            brevGenerererTjeneste.genererVedtaksbrev(dto.behandlingId());

        if (generertBrev == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        return switch (mediaTypeReq) {
            case PDF_MEDIA_STRING, MediaType.APPLICATION_JSON -> Response.ok(generertBrev.dokument().pdf()).build();
            case MediaType.TEXT_HTML -> Response.ok(generertBrev.dokument().html()).build();
            default -> Response.ok(generertBrev.dokument().pdf()) //Kun for å få swagger til å laste ned pdf
                .header("Content-Disposition", String.format("attachment; filename=\"%s-%s.pdf\"", dto.behandlingId(), generertBrev.malType().getKode()))
                .build();

        };
    }

}

