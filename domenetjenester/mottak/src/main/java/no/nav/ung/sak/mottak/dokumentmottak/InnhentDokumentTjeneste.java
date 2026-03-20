package no.nav.ung.sak.mottak.dokumentmottak;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

import java.util.Collection;

public interface InnhentDokumentTjeneste {
    void mottaDokument(Fagsak fagsak, Collection<MottattDokument> mottattDokument);


    static InnhentDokumentTjeneste finnTjeneste(Instance<InnhentDokumentTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(InnhentDokumentTjeneste.class, instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType.getKode()));
    }

}
