package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.template.dto.ManuellVedtaksbrevDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

@Dependent
public class ManuellVedtaksbrevInnholdBygger implements VedtaksbrevInnholdBygger {

    private final VedtaksbrevValgRepository vedtaksbrevValgRepository;

    @Inject
    public ManuellVedtaksbrevInnholdBygger(VedtaksbrevValgRepository vedtaksbrevValgRepository) {
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Ingen lagrede valg for behandling"));

        if (valg.getRedigertBrevHtml() == null || valg.getRedigertBrevHtml().isBlank()) {
            throw new IllegalStateException("Ingen lagret tekst for behandling");
        }

        var tekst = valg.getRedigertBrevHtml();
        //TODO saniter html

        return new TemplateInnholdResultat(
            DokumentMalType.MANUELT_VEDTAK_DOK,
            TemplateType.MANUELL_VEDTAKSBREV,
            new ManuellVedtaksbrevDto(tekst)
        );
    }
}
