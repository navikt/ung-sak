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
            true
        ));
    }

    private List<InformasjonsbrevMottakerValgDto> mapMottakere(Behandling behandling) {
        //Kan ikke sende brev til død mottaker, men ønsker å vise det til klient.
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
        return validerOgGenererBrev(dto, kunHtml);
    }

    public BrevbestillingResultat bestill(InformasjonsbrevBestillingDto dto) {
        GenerertBrev generertBrev = validerOgGenererBrev(dto, false);
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        return brevbestillingTjeneste.bestillBrev(behandling, generertBrev);
    }

    private GenerertBrev validerOgGenererBrev(InformasjonsbrevBestillingDto dto, Boolean kunHtml) {
        var informasjonsbrevValgDtos = informasjonsbrevValg(dto.behandlingId());
        if (informasjonsbrevValgDtos.isEmpty()) {
            throw new IllegalArgumentException("Ingen informasjonsbrevvalg funnet for behandlingen");
        }

        var valg = informasjonsbrevValgDtos.stream().filter(it -> it.malType().equals(dto.informasjonsbrevMalType())).findFirst();
        if (valg.isEmpty()) {
            throw new IllegalArgumentException(("Støtter ikke maltype: " + dto.informasjonsbrevMalType()
                + ". Støtter kun: " + informasjonsbrevValgDtos.stream()
                .map(InformasjonsbrevValgDto::malType)
                .toList()));
        }

        boolean valgtMottakerErDød = valg.get().mottakere().stream().anyMatch(
            mottaker -> mottaker.idType().equals(dto.mottakerDto().type())
                && mottaker.id().equals(dto.mottakerDto().id())
                && mottaker.utilgjengeligÅrsak() == UtilgjengeligÅrsak.PERSON_DØD);

        if (valgtMottakerErDød) {
            throw new IllegalArgumentException(("Støtter ikke brev der mottaker er død"));
        }

        return informasjonsbrevGenerererTjeneste.genererInformasjonsbrev(
            new InformasjonsbrevBestillingInput(dto.behandlingId(), dto.informasjonsbrevMalType(), dto.innhold(), Boolean.TRUE.equals(kunHtml)));
    }

}
