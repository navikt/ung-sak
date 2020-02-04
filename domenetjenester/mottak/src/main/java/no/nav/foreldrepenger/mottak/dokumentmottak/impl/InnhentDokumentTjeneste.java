package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentGruppe;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;

@Dependent
public class InnhentDokumentTjeneste {

    private static Map<String, DokumentGruppe> DOKUMENTTYPE_TIL_GRUPPE = new HashMap<>();
    static {
        // Søknad
        DokumentTypeId.getSøknadTyper().forEach(v -> DOKUMENTTYPE_TIL_GRUPPE.put(v, DokumentGruppe.SØKNAD));

        // Inntektsmelding
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.INNTEKTSMELDING.getKode(), DokumentGruppe.INNTEKTSMELDING);

        // Endringssøknad
        DokumentTypeId.getEndringSøknadTyper().forEach(v -> DOKUMENTTYPE_TIL_GRUPPE.put(v, DokumentGruppe.ENDRINGSSØKNAD));

    }

    private static Map<DokumentKategori, DokumentGruppe> DOKUMENTKATEGORI_TIL_GRUPPE = new HashMap<>();
    static {
        DOKUMENTKATEGORI_TIL_GRUPPE.put(DokumentKategori.SØKNAD, DokumentGruppe.SØKNAD);
        DOKUMENTKATEGORI_TIL_GRUPPE.put(DokumentKategori.KLAGE_ELLER_ANKE, DokumentGruppe.KLAGE);
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
        DokumentTypeId dokumentTypeId = mottattDokument.getDokumentType();

        DokumentGruppe dokumentGruppe = brukDokumentKategori(dokumentTypeId, mottattDokument.getDokumentKategori()) ?
            DOKUMENTKATEGORI_TIL_GRUPPE.getOrDefault(mottattDokument.getDokumentKategori(), DokumentGruppe.VEDLEGG) :
            DOKUMENTTYPE_TIL_GRUPPE.getOrDefault(dokumentTypeId.getKode(), DokumentGruppe.VEDLEGG);

        Dokumentmottaker dokumentmottaker = finnMottaker(dokumentGruppe, fagsak.getYtelseType());
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, dokumentTypeId, behandlingÅrsakType);
    }

    public void opprettFraTidligereBehandling(MottattDokument sistMottatteSøknad, BehandlingÅrsakType behandlingÅrsakType) { //#SXX
        Objects.requireNonNull(sistMottatteSøknad.getBehandlingId(), "behandlingId"); //$NON-NLS-1$
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(sistMottatteSøknad.getFagsakId());
        Dokumentmottaker dokumentmottaker = finnMottaker(DokumentGruppe.SØKNAD, fagsak.getYtelseType());
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(fagsak, sistMottatteSøknad.getBehandlingId(), sistMottatteSøknad, behandlingÅrsakType);
    }

    private boolean brukDokumentKategori(DokumentTypeId dokumentTypeId, DokumentKategori dokumentKategori) {
        return DokumentTypeId.UDEFINERT.equals(dokumentTypeId) ||
            (DokumentKategori.SØKNAD.equals(dokumentKategori) && DokumentTypeId.ANNET.equals(dokumentTypeId));
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
