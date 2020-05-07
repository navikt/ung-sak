package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentGruppe;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@Dependent
public class InnhentDokumentTjeneste {

    private static Map<String, DokumentGruppe> DOKUMENTTYPE_TIL_GRUPPE = new HashMap<>();
    static {
        // Inntektsmelding
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.INNTEKTSMELDING.getKode(), DokumentGruppe.INNTEKTSMELDING);

    }

    private Instance<Dokumentmottaker> mottakere;

    private FagsakRepository fagsakRepository;

    @Inject
    public InnhentDokumentTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                   @Any Instance<Dokumentmottaker> mottakere) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.mottakere = mottakere;
    }

    public void utfør(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(mottattDokument.getFagsakId());
        DokumentGruppe dokumentGruppe = DokumentGruppe.INNTEKTSMELDING; // eneste supporterte foreløpig.
        Dokumentmottaker dokumentmottaker = finnMottaker(dokumentGruppe, fagsak.getYtelseType());
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, behandlingÅrsakType);
    }

    private Dokumentmottaker finnMottaker(DokumentGruppe dokumentGruppe, FagsakYtelseType fagsakYtelseType) {
        String dokumentgruppeKode = dokumentGruppe.getKode();
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(dokumentgruppeKode));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmotaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + dokumentgruppeKode));
    }
}
