package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.template.dto.ManuellVedtaksbrevDto;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.util.Set;

@Dependent
public class ManueltVedtaksbrevInnholdBygger {

    private final VedtaksbrevValgRepository vedtaksbrevValgRepository;

    @Inject
    public ManueltVedtaksbrevInnholdBygger(VedtaksbrevValgRepository vedtaksbrevValgRepository) {
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }

    public TemplateInnholdResultat bygg(Behandling behandling, DokumentMalType originalDokumentMalType) {
        var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId(), originalDokumentMalType)
            .orElseThrow(() -> new IllegalStateException("Ingen lagrede valg for dokumentMaltype " + originalDokumentMalType));

        valider(originalDokumentMalType, valg.getRedigertBrevHtml());

        var tekst = valg.getRedigertBrevHtml();

        return new TemplateInnholdResultat(
                TemplateType.MANUELT_VEDTAKSBREV,
            new ManuellVedtaksbrevDto(tekst),
            false);
    }

    private static void valider(DokumentMalType originalDokumentMalType, String redigertBrevHtml) {
        if (redigertBrevHtml == null || redigertBrevHtml.isBlank()) {
            throw new IllegalStateException("Ingen lagret tekst for originalDokumentMalType " + originalDokumentMalType);
        }

        var parsedHtml = Jsoup.parse(redigertBrevHtml, Parser.htmlParser());

        if (parsedHtml.body().text().trim().isEmpty()) {
            throw new IllegalStateException("Manuelt brev kan ikke være tom");
        }

        var forsteTagg = parsedHtml.body().firstElementChild();
        Set<String> gyldigeOverskriftTagger = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

        if (forsteTagg == null || !gyldigeOverskriftTagger.contains(forsteTagg.tagName())) {
            throw new IllegalStateException("Manuelt brev må ha overskift som første element, men fant html-tagg=" +
                (forsteTagg != null ? forsteTagg.tagName() : "ingen"));
        }

        if (forsteTagg.text().trim().isEmpty()) {
            throw new IllegalStateException("Manuelt brev har tom overskrift");
        }

        if (redigertBrevHtml.contains(TomVedtaksbrevInnholdBygger.TOM_VEDTAKSBREV_HTML_OVERSKRIFT)) {
            throw new IllegalStateException("Manuelt brev innholder preutfylt overskrift! ");
        }

        if (redigertBrevHtml.contains(TomVedtaksbrevInnholdBygger.TOM_VEDTAKSBREV_HTML_BRØDTEKST)) {
            throw new IllegalStateException("Manuelt brev innholder preutfylt brødtekst!");
        }
    }


}
