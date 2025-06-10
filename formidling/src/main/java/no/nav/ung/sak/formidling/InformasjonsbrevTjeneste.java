package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;

import java.util.List;

@Dependent
public class InformasjonsbrevTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final PersonopplysningRepository personopplysningRepository;

    @Inject
    public InformasjonsbrevTjeneste(BehandlingRepository behandlingRepository, PersonopplysningRepository personopplysningRepository) {
        this.behandlingRepository = behandlingRepository;
        this.personopplysningRepository = personopplysningRepository;
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

    private List<InformasjonsbrevMottakerDto> mapMottakere(Behandling behandling) {
        boolean dødsfall = erDødsfall(behandling);

        String aktørid = behandling.getFagsak().getAktørId().getId();
        return List.of(new InformasjonsbrevMottakerDto(aktørid, IdType.AKTØRID,
            dødsfall ? UtilgjengeligÅrsak.PERSON_DØD : null));
    }

    private boolean erDødsfall(Behandling behandling) {
        var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        return personopplysningGrunnlag.getGjeldendeVersjon().getPersonopplysninger().stream()
            .anyMatch(it -> it.getDødsdato() != null);
    }

    public GenerertBrev forhåndsvis(InformasjonsbrevForhåndsvisDto dto) {


        return null;
    }
}
