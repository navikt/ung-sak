package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

public interface Dokumentmottaker {

    void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling);

    List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument);

    static Dokumentmottaker finnDokumentMottaker(Instance<Dokumentmottaker> dokumentmottakere, Brevkode brevkode, FagsakYtelseType fagsakYtelseType) {
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = dokumentmottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode.getKode()));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmottaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + brevkode));
    }

}



