package no.nav.ung.sak.formidling.informasjonsbrev;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingResultat;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingTjeneste;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.mottaker.PdlPerson;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevBestillingDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerValgDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;

import java.util.List;
import java.util.Locale;

@Dependent
public class InformasjonsbrevTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final InformasjonsbrevGenerererTjeneste informasjonsbrevGenerererTjeneste;
    private final BrevbestillingTjeneste brevbestillingTjeneste;
    private final BrevMottakerTjeneste brevMottakerTjeneste;

    @Inject
    public InformasjonsbrevTjeneste(BehandlingRepository behandlingRepository, InformasjonsbrevGenerererTjeneste informasjonsbrevGenerererTjeneste, BrevbestillingTjeneste brevbestillingTjeneste, BrevMottakerTjeneste brevMottakerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.informasjonsbrevGenerererTjeneste = informasjonsbrevGenerererTjeneste;
        this.brevbestillingTjeneste = brevbestillingTjeneste;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
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
        PdlPerson pdlPerson = brevMottakerTjeneste.hentMottaker(behandling);

        return List.of(
            new InformasjonsbrevMottakerValgDto(
                pdlPerson.aktørId().getId(),
                IdType.AKTØRID,
                formaterMedStoreOgSmåBokstaver(pdlPerson.navn()),
                pdlPerson.fnr(),
                //Kan ikke sende brev til død mottaker, men ønsker å vise det til klient.
                pdlPerson.dødsdato() != null ? UtilgjengeligÅrsak.PERSON_DØD : null));

    }

    /**
     * Kopi fra PersonopplysningDtoTjeneste
     */
    private static String formaterMedStoreOgSmåBokstaver(String tekst) {
        if (tekst == null || (tekst = tekst.trim()).isEmpty()) { // NOSONAR
            return null;
        }
        String skilletegnPattern = "(\\s|[()\\-_.,/])";
        char[] tegn = tekst.toLowerCase(Locale.getDefault()).toCharArray();
        boolean nesteSkalHaStorBokstav = true;
        for (int i = 0; i < tegn.length; i++) {
            boolean erSkilletegn = String.valueOf(tegn[i]).matches(skilletegnPattern);
            if (!erSkilletegn && nesteSkalHaStorBokstav) {
                tegn[i] = Character.toTitleCase(tegn[i]);
            }
            nesteSkalHaStorBokstav = erSkilletegn;
        }
        return new String(tegn);
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
            mottaker -> mottaker.idType().equals(dto.mottaker().type())
                && mottaker.id().equals(dto.mottaker().id())
                && mottaker.utilgjengeligÅrsak() == UtilgjengeligÅrsak.PERSON_DØD);

        if (valgtMottakerErDød) {
            throw new IllegalArgumentException(("Støtter ikke brev der mottaker er død"));
        }

        return informasjonsbrevGenerererTjeneste.genererInformasjonsbrev(
            new InformasjonsbrevBestillingInput(dto.behandlingId(), dto.informasjonsbrevMalType(), dto.innhold(), Boolean.TRUE.equals(kunHtml)));
    }

}
