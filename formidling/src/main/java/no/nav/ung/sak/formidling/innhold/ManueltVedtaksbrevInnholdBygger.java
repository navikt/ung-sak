package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.template.dto.ManuellVedtaksbrevDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

@Dependent
public class ManueltVedtaksbrevInnholdBygger implements VedtaksbrevInnholdBygger {

    private final VedtaksbrevValgRepository vedtaksbrevValgRepository;

    @Inject
    public ManueltVedtaksbrevInnholdBygger(VedtaksbrevValgRepository vedtaksbrevValgRepository) {
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        //TODO endre til å få inn valg fra input?
        var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId()).stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Ingen lagrede valg for behandling"));

        if (valg.getRedigertBrevHtml() == null || valg.getRedigertBrevHtml().isBlank()) {
            throw new IllegalStateException("Ingen lagret tekst for behandling");
        }

        var tekst = valg.getRedigertBrevHtml();
        //TODO saniter html

        return new TemplateInnholdResultat(
                TemplateType.MANUELT_VEDTAKSBREV,
            new ManuellVedtaksbrevDto(tekst),
            false);
    }
}
