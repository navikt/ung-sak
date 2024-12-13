package no.nav.ung.sak.formidling;


import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.dto.BrevbestillingDto;
import no.nav.ung.sak.formidling.dto.GenerertBrev;
import no.nav.ung.sak.formidling.dto.PartResponseDto;
import no.nav.ung.sak.formidling.kodeverk.IdType;
import no.nav.ung.sak.formidling.kodeverk.RolleType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@ApplicationScoped
public class BrevGenerererTjeneste {

    private BehandlingRepository behandlingRepository;
    private PersonBasisTjeneste personBasisTjeneste;
    private AktørTjeneste aktørTjeneste;

    @Inject
    public BrevGenerererTjeneste(
        BehandlingRepository behandlingRepository,
        PersonBasisTjeneste personBasisTjeneste,
        AktørTjeneste aktørTjeneste
    ) {
        this.behandlingRepository = behandlingRepository;
        this.personBasisTjeneste = personBasisTjeneste;
        this.aktørTjeneste = aktørTjeneste;
    }

    public GenerertBrev genererPdf(BrevbestillingDto brevbestillingDto) {

        Behandling behandling = behandlingRepository.hentBehandling(brevbestillingDto.behandlingId());
        PartResponseDto mottaker = hentMottaker(behandling);

        // valider mal via regel hvis vedtaksbrev

        // lag brev json via datasamler og velg template

        // konverter til pdf fra template


        return new GenerertBrev(
            "123".getBytes(),
            mottaker,
            mottaker,
            brevbestillingDto.malType()
        );
    }

    private PartResponseDto hentMottaker(Behandling behandling) {
        AktørId aktørId = behandling.getFagsak().getAktørId();
        var personIdent = aktørTjeneste.hentPersonIdentForAktørId(aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke person med aktørid"));
        PersoninfoBasis personinfoBasis = personBasisTjeneste.hentBasisPersoninfo(aktørId, personIdent);


        PartResponseDto mottaker = new PartResponseDto(aktørId.getId(), personinfoBasis.getNavn(), IdType.AKTØRID, RolleType.BRUKER);
        return mottaker;
    }
}
