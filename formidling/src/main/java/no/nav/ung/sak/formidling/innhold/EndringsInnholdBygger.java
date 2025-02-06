package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@Dependent
public class EndringsInnholdBygger implements VedtaksbrevInnholdBygger  {


    @Override
    public TemplateInnholdResultat bygg(Behandling behandlingId) {
        return null;
    }
}
