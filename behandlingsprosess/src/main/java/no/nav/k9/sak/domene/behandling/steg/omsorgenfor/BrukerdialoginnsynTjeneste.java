package no.nav.k9.sak.domene.behandling.steg.omsorgenfor;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;

public interface BrukerdialoginnsynTjeneste {

    public static BrukerdialoginnsynTjeneste finnTjeneste(Instance<BrukerdialoginnsynTjeneste> tjenester, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(tjenester, fagsakYtelseType).orElseThrow(() -> new IllegalArgumentException("Har ikke BrukerdialoginnsynTjeneste for " + fagsakYtelseType));
    }

    public void publiserDokumentHendelse(Behandling behandling, MottattDokument mottattDokument);

    public void publiserOmsorgenForHendelse(Behandling behandling, boolean harOmsorg);
}
