package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@Dependent
public class InnhentDokumentTjeneste {

    private Instance<Dokumentmottaker> mottakere;

    @Inject
    public InnhentDokumentTjeneste(@Any Instance<Dokumentmottaker> mottakere) {
        this.mottakere = mottakere;
    }

    public void utfør(Fagsak fagsak, List<MottattDokument> mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        var type = getMottattDokumentType(mottattDokument);
        Dokumentmottaker dokumentmottaker = finnMottaker(type, fagsak.getYtelseType());
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, behandlingÅrsakType);
    }

    private Brevkode getMottattDokumentType(List<MottattDokument> mottattDokument) {
        if (mottattDokument == null || mottattDokument.isEmpty()) {
            throw new IllegalArgumentException("Mottattdokument er null eller empty");
        }
        var type = mottattDokument.get(0).getType();
        var typer = mottattDokument.stream().map(m -> m.getType()).distinct().collect(Collectors.toList());
        if (typer.size() > 1) {
            throw new UnsupportedOperationException("Støtter ikke mottatt dokument med ulike typer: " + typer);
        }
        return type;
    }

    private Dokumentmottaker finnMottaker(Brevkode brevkode, FagsakYtelseType fagsakYtelseType) {
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmottaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + brevkode));
    }
}
