package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrev;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

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

        //TODO håndtere flere resultater
        BehandlingVedtaksbrevResultat totalResultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Regel resultater: {}", totalResultat.safePrint());

        if (!totalResultat.harBrev()) {
            return mapIngenBrevResponse(totalResultat);
        }

        Vedtaksbrev resultat = totalResultat.vedtaksbrevResultater().stream()
                .findFirst()
                .orElseThrow();


        var egenskaper = resultat.vedtaksbrevEgenskaper();

        return new VedtaksbrevValgResponse(
            true,
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

    private static VedtaksbrevValgResponse mapIngenBrevResponse(BehandlingVedtaksbrevResultat totalResultat) {
        return new VedtaksbrevValgResponse(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            totalResultat.ingenBrevResultater().stream().map(IngenBrev::forklaring).collect(Collectors.joining(", ", "[", "]")),
            null
        );
    }

    public boolean måSkriveBrev(Long behandlingId) {
        var totalResultat = vedtaksbrevRegler.kjør(behandlingId);
        if (!totalResultat.harBrev()) {
            return false;
        }

        return totalResultat.vedtaksbrevResultater().stream()
            .map(Vedtaksbrev::vedtaksbrevEgenskaper)
            .anyMatch(
                egenskaper -> egenskaper.kanRedigere() && !egenskaper.kanOverstyreRediger()
            );
    }

    public VedtaksbrevValgEntitet lagreVedtaksbrev(VedtaksbrevValgRequest dto) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        if (behandling.erAvsluttet()) {
            throw new BadRequestException("Kan ikke endre vedtaksbrev på avsluttet behandling");
        }

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(dto.behandlingId());
        if (!totalresultater.harBrev()) {
            throw new BadRequestException("Ingen vedtaksbrev resultater for behandling");
        }

        var resultat = totalresultater.vedtaksbrevResultater().getFirst();

        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(dto.behandlingId())
            .orElse(VedtaksbrevValgEntitet.ny(dto.behandlingId()));

        //TODO håndtere flere resultater
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

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(dto.behandlingId());
        if (!totalresultater.harBrev()) {
            throw new IllegalArgumentException("Ingen vedtaksbrev resultater for behandling. Årsak: "+ totalresultater.ingenBrevResultater().stream()
                .map(IngenBrev::forklaring)
                .collect(Collectors.joining(", ", "[", "]")));
        }


        var kunHtml = Boolean.TRUE.equals(dto.htmlVersjon());

        if (dto.redigertVersjon() == null) {
            var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(dto.behandlingId()).orElse(null);
            if (valg != null) {
                if (valg.isHindret()) {
                    LOG.info("Vedtaksbrev er manuelt stoppet - lager ikke brev");
                    return null;
                }
                if (valg.isRedigert()) {
                    LOG.info("Vedtaksbrev er manuelt redigert - genererer manuell brev");
                    return vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), kunHtml);
                }
            }

            return genererAutomatiskVedtaksbrev(kunHtml, totalresultater, dto.behandlingId());
        }
        if (dto.redigertVersjon()) {
            return vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), kunHtml);
        }

        return genererAutomatiskVedtaksbrev(kunHtml, totalresultater, dto.behandlingId());
    }

    private GenerertBrev genererAutomatiskVedtaksbrev(boolean kunHtml, BehandlingVedtaksbrevResultat totalresultater, Long behandlingId) {
        return vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(
            new VedtaksbrevBestillingInput(
                behandlingId,
                totalresultater.vedtaksbrevResultater().getFirst(), totalresultater.detaljertResultatTimeline(), kunHtml
                //TODO håndtere flere resultater
            )
        );
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

