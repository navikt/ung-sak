package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface Dokumentmottaker {

    void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak);

    void mottaDokument(Collection<MottattDokument> mottattDokument, Fagsak fagsak);

    void validerDokument(MottattDokument mottattDokument, FagsakYtelseType ytelseType);

}
