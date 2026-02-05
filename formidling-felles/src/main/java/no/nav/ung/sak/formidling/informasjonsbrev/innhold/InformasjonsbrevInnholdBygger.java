package no.nav.ung.sak.formidling.informasjonsbrev.innhold;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;

public interface InformasjonsbrevInnholdBygger<T> {
    TemplateInnholdResultat bygg(Behandling behandling, T innhold);
}
