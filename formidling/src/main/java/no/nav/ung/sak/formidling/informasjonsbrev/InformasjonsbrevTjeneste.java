package no.nav.ung.sak.formidling.informasjonsbrev;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingResultat;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingTjeneste;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevBestillingDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerValgDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;

import java.util.List;

@Dependent
public class InformasjonsbrevTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final PersonopplysningRepository personopplysningRepository;
    private final InformasjonsbrevGenerererTjeneste informasjonsbrevGenerererTjeneste;
    private final BrevbestillingTjeneste brevbestillingTjeneste;

    @Inject
    public InformasjonsbrevTjeneste(BehandlingRepository behandlingRepository, PersonopplysningRepository personopplysningRepository, InformasjonsbrevGenerererTjeneste informasjonsbrevGenerererTjeneste, BrevbestillingTjeneste brevbestillingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.personopplysningRepository = personopplysningRepository;
        this.informasjonsbrevGenerererTjeneste = informasjonsbrevGenerererTjeneste;
        this.brevbestillingTjeneste = brevbestillingTjeneste;
    }

    public List<InformasjonsbrevValgDto> informasjonsbrevValg(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        return List.of(new InformasjonsbrevValgDto(
            InformasjonsbrevMalType.GENERELT_FRITEKSTBREV,
            mapMottakere(behandling),
            false,
            true,
            false
        ));
    }

    private List<InformasjonsbrevMottakerValgDto> mapMottakere(Behandling behandling) {
        boolean dødsfall = erDødsfall(behandling);

        String aktørid = behandling.getFagsak().getAktørId().getId();
        return List.of(new InformasjonsbrevMottakerValgDto(aktørid, IdType.AKTØRID,
            dødsfall ? UtilgjengeligÅrsak.PERSON_DØD : null));
    }

    private boolean erDødsfall(Behandling behandling) {
        var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        return personopplysningGrunnlag.getGjeldendeVersjon().getPersonopplysninger().stream()
            .anyMatch(it -> it.getDødsdato() != null);
    }

    public GenerertBrev forhåndsvis(InformasjonsbrevBestillingDto dto, Boolean kunHtml) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        return validerOgGenererBrev(behandling, dto, kunHtml);
    }

    public BrevbestillingResultat bestill(InformasjonsbrevBestillingDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        GenerertBrev generertBrev = validerOgGenererBrev(behandling, dto, false);
        return brevbestillingTjeneste.bestillBrev(behandling, generertBrev);
    }

    private GenerertBrev validerOgGenererBrev(Behandling behandling, InformasjonsbrevBestillingDto dto, Boolean kunHtml) {
        if (erDødsfall(behandling)) {
            throw new IllegalStateException("Støtter ikke generelt brev der mottaker er død");
        }

        return informasjonsbrevGenerererTjeneste.genererInformasjonsbrev(
            new InformasjonsbrevRequest(dto.behandlingId(), dto.informasjonsbrevMalType(), dto.innhold(), kunHtml));
    }

}
