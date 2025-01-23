package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface VedtaksbrevInnholdBygger {

    TemplateInnholdResultat bygg(Behandling behandlingId);

}


