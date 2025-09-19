package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.template.dto.ManuellVedtaksbrevDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

@Dependent
public class TomVedtaksbrevInnholdBygger implements VedtaksbrevInnholdBygger {

    public static final String TOM_VEDTAKSBREV_HTML_OVERSKRIFT = "Fyll inn overskrift...";
    public static final String TOM_VEDTAKSBREV_HTML_BRØDTEKST = "Fyll inn brødtekst...";

    private static final String TOM_VEDTAKSBREV_HTML_MAL = """
        <h1>&lt;%s&gt;</h1>
        <p>&lt;%s&gt;</p>
        """.formatted(TOM_VEDTAKSBREV_HTML_OVERSKRIFT, TOM_VEDTAKSBREV_HTML_BRØDTEKST);

    @Inject
    public TomVedtaksbrevInnholdBygger() {
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        return new TemplateInnholdResultat(TemplateType.MANUELT_VEDTAKSBREV, new ManuellVedtaksbrevDto(TOM_VEDTAKSBREV_HTML_MAL), false);
    }
}
