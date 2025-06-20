package no.nav.ung.sak.formidling.informasjonsbrev;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.KodeverdiSomObjekt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingResultat;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingTjeneste;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevBestillingRequest;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerValgResponse;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.Locale;

@Dependent
public class InformasjonsbrevTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final InformasjonsbrevGenerererTjeneste informasjonsbrevGenerererTjeneste;
    private final BrevbestillingTjeneste brevbestillingTjeneste;
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public InformasjonsbrevTjeneste(BehandlingRepository behandlingRepository, InformasjonsbrevGenerererTjeneste informasjonsbrevGenerererTjeneste, BrevbestillingTjeneste brevbestillingTjeneste, PersonopplysningRepository personopplysningRepository) {
        this.behandlingRepository = behandlingRepository;
        this.informasjonsbrevGenerererTjeneste = informasjonsbrevGenerererTjeneste;
        this.brevbestillingTjeneste = brevbestillingTjeneste;
        this.personopplysningRepository = personopplysningRepository;
    }

    public List<InformasjonsbrevValgDto> informasjonsbrevValg(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        boolean støtterFritekst = false;
        boolean støtterTittelOgFritekst = true;
        return List.of(new InformasjonsbrevValgDto(
            new KodeverdiSomObjekt<>(DokumentMalType.GENERELT_FRITEKSTBREV),
            mapMottakere(behandling),
            støtterFritekst,
            støtterTittelOgFritekst
        ));
    }

    private List<InformasjonsbrevMottakerValgResponse> mapMottakere(Behandling behandling) {
        AktørId aktørId = behandling.getAktørId();
        var personopplysning = personopplysningRepository
            .hentPersonopplysninger(behandling.getId())
            .getGjeldendeVersjon()
            .getPersonopplysning(aktørId);

        return List.of(
            new InformasjonsbrevMottakerValgResponse(
                aktørId.getId(),
                IdType.AKTØRID,
                personopplysning.getFødselsdato(),
                formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()),
                //Kan ikke sende brev til død mottaker, men ønsker å vise det til klient.
                personopplysning.getDødsdato() != null ? UtilgjengeligÅrsak.PERSON_DØD : null
            ));

    }

    /**
     * Kopi fra PersonopplysningDtoTjeneste med en annen implementasjon men samme funksjonalitet
     */
    private static String formaterMedStoreOgSmåBokstaver(String text) {
        if (text == null || (text = text.trim()).isEmpty()) {
            return null;
        }
        String delimiters = " ()-_. ,/"; // All characters to treat as word boundaries
        char[] chars = text.toLowerCase(Locale.getDefault()).toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < chars.length; i++) {
            if (delimiters.indexOf(chars[i]) >= 0) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                chars[i] = Character.toTitleCase(chars[i]);
                capitalizeNext = false;
            }
        }
        return new String(chars);
    }

    public GenerertBrev forhåndsvis(InformasjonsbrevBestillingRequest dto, Boolean kunHtml) {
        return validerOgGenererBrev(dto, kunHtml);
    }

    public BrevbestillingResultat bestill(InformasjonsbrevBestillingRequest dto) {
        GenerertBrev generertBrev = validerOgGenererBrev(dto, false);
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        return brevbestillingTjeneste.bestillBrev(behandling, generertBrev);
    }

    private GenerertBrev validerOgGenererBrev(InformasjonsbrevBestillingRequest dto, Boolean kunHtml) {
        var informasjonsbrevValgDtos = informasjonsbrevValg(dto.behandlingId());
        if (informasjonsbrevValgDtos.isEmpty()) {
            throw new IllegalArgumentException("Ingen informasjonsbrevvalg funnet for behandlingen");
        }

        var valg = informasjonsbrevValgDtos.stream().
            filter(it -> it.malType().getKilde() == dto.dokumentMalType())
            .findFirst();
        if (valg.isEmpty()) {
            throw new IllegalArgumentException(("Støtter ikke maltype: " + dto.dokumentMalType()
                + ". Støtter kun: " + informasjonsbrevValgDtos.stream()
                .map(InformasjonsbrevValgDto::malType)
                .toList()));
        }

        boolean valgtMottakerErDød = valg.get().mottakere().stream().anyMatch(
            mottaker -> mottaker.idType() == dto.mottaker().type()
                && mottaker.id().equals(dto.mottaker().id())
                && mottaker.utilgjengeligÅrsak() == UtilgjengeligÅrsak.PERSON_DØD);

        if (valgtMottakerErDød) {
            throw new IllegalArgumentException(("Støtter ikke brev der mottaker er død"));
        }

        return informasjonsbrevGenerererTjeneste.genererInformasjonsbrev(
            new InformasjonsbrevBestillingInput(dto.behandlingId(), dto.dokumentMalType(), dto.innhold(), Boolean.TRUE.equals(kunHtml)));
    }

}
