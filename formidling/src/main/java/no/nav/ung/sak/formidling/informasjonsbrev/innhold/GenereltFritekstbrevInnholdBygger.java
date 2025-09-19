package no.nav.ung.sak.formidling.informasjonsbrev.innhold;


import jakarta.enterprise.context.Dependent;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.template.dto.GenereltFritekstBrevTemplateDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.GenereltFritekstBrevDto;

@InformasjonsbrevInnholdByggerTypeRef(DokumentMalType.GENERELT_FRITEKSTBREV)
@Dependent
public class GenereltFritekstbrevInnholdBygger implements InformasjonsbrevInnholdBygger<GenereltFritekstBrevDto> {

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, GenereltFritekstBrevDto innhold) {

        return new TemplateInnholdResultat(
                TemplateType.GENERELT_FRITEKSTBREV,
            new GenereltFritekstBrevTemplateDto(
                innhold.overskrift(),
                MarkdownParser.markdownTilHtml(innhold.brødtekst())
            ), false);
    }
}
