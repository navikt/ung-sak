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
    
    InnhentDokumentTjeneste() {
        // CDI proxy
    }

    @Inject
    public InnhentDokumentTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                   @Any Instance<Dokumentmottaker> mottakere) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.mottakere = mottakere;
    }

    public void utfør(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(mottattDokument.getFagsakId());
        DokumentGruppe dokumentGruppe = DokumentGruppe.INNTEKTSMELDING;
        Dokumentmottaker dokumentmottaker = finnMottaker(dokumentGruppe, fagsak.getYtelseType());
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, behandlingÅrsakType);
    }

    private Dokumentmottaker finnMottaker(DokumentGruppe dokumentGruppe, FagsakYtelseType fagsakYtelseType) {
        String dokumentgruppeKode = dokumentGruppe.getKode();
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(dokumentgruppeKode));

        if (selected.isAmbiguous()) {
            selected = selected.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(fagsakYtelseTypeKode));
        }

        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for DokumentGruppe=" + dokumentgruppeKode + ", FagsakYtelseType=" + fagsakYtelseTypeKode);
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for DokumentGruppe=" + dokumentgruppeKode + ", FagsakYtelseType=" + fagsakYtelseTypeKode);
        }
        Dokumentmottaker minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }
}
