package no.nav.ung.sak.formidling.informasjonsbrev.innhold;


import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.template.dto.GenerellFritekstbrevDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.GenereltFritekstBrevDto;

@InformasjonsbrevInnholdByggerTypeRef(InformasjonsbrevMalType.GENERELT_FRITEKSTBREV)
public class GenereltFritekstbrevInnholdBygger implements InformasjonsbrevInnholdBygger<GenereltFritekstBrevDto> {

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, GenereltFritekstBrevDto innhold) {

        //TODO sanitize html!

        return new TemplateInnholdResultat(
            DokumentMalType.GENERELT_FRITEKSTBREV,
            TemplateType.GENERELT_FRITEKSTBREV,
            new GenerellFritekstbrevDto(
                innhold.overskrift(),
                innhold.brødtekst()
            ));
    }
}
