package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.regler.*;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValg;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

        BehandlingVedtaksbrevResultat totalResultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Regel resultater: {}", totalResultat.safePrint());

        if (!totalResultat.harBrev()) {
            return mapIngenBrevResponse(totalResultat);
        }

        var vedtaksbrevValg = totalResultat.vedtaksbrevResultater().stream()
            .map(it -> mapVedtaksbrev(it.vedtaksbrevEgenskaper(), valg, erAvsluttet, it))
            .toList();

        VedtaksbrevValg førsteValg = vedtaksbrevValg.getFirst();

        return new VedtaksbrevValgResponse(
            true,
            førsteValg.enableHindre(),
            førsteValg.hindret(),
            førsteValg.kanOverstyreHindre(),
            førsteValg.enableRediger(),
            førsteValg.redigert(),
            førsteValg.kanOverstyreRediger(),
            førsteValg.forklaring(),
            førsteValg.redigertBrevHtml(),
            vedtaksbrevValg);
    }

    @NotNull
    private static VedtaksbrevValg mapVedtaksbrev(VedtaksbrevEgenskaper egenskaper, Optional<VedtaksbrevValgEntitet> valg, boolean erAvsluttet, Vedtaksbrev resultat) {
        return new VedtaksbrevValg(
            resultat.dokumentMalType(), egenskaper.kanHindre(),
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
            null,
            Collections.emptyList()
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

    public List<GenerertBrev> forhåndsvis(VedtaksbrevForhåndsvisRequest dto) {

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
                    throw new IllegalArgumentException("Vedtaksbrev er manuelt hindret.");
                }
                if (valg.isRedigert()) {
                    LOG.info("Vedtaksbrev er manuelt redigert - genererer manuell brev");
                    return List.of(vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), kunHtml));
                }
            }

            return genererAutomatiskVedtaksbrev(kunHtml, totalresultater, dto.behandlingId(), dto.dokumentMalType());
        }
        if (dto.redigertVersjon()) {
            return List.of(vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), kunHtml));
        }

        return genererAutomatiskVedtaksbrev(kunHtml, totalresultater, dto.behandlingId(), dto.dokumentMalType());
    }

private List<GenerertBrev> genererAutomatiskVedtaksbrev(boolean kunHtml, BehandlingVedtaksbrevResultat totalresultater, Long behandlingId, DokumentMalType dokumentMalType) {
    return totalresultater.vedtaksbrevResultater().stream()
        .filter(vedtaksbrev -> dokumentMalType == null || vedtaksbrev.dokumentMalType() == dokumentMalType)
        .map(vedtaksbrev -> vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(
            new VedtaksbrevGenerererInput(
                behandlingId,
                vedtaksbrev,
                totalresultater.detaljertResultatTimeline(),
                kunHtml
            )))
        .toList();
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

