package no.nav.k9.sak.domene.behandling.steg.omsorgenfor;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;

@FagsakYtelseTypeRef
@ApplicationScoped
public class DefaultBrukerdialoginnsynTjeneste implements BrukerdialoginnsynTjeneste {
    @Override
    public void publiserDokumentHendelse(Behandling behandling, MottattDokument mottattDokument) {

    }

    @Override
    public void publiserOmsorgenForHendelse(Behandling behandling, boolean harOmsorg) {

    }
}
