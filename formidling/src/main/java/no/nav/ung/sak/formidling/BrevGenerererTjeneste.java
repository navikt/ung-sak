package no.nav.ung.sak.formidling;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.dto.BrevbestillingDto;
import no.nav.ung.sak.formidling.pdfgen.PdfGen;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.PartResponseDto;
import no.nav.ung.sak.formidling.kodeverk.IdType;
import no.nav.ung.sak.formidling.kodeverk.RolleType;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.FellesTemplateData;
import no.nav.ung.sak.formidling.template.dto.InnvilgelseTemplate;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
public class BrevGenerererTjeneste {

    private BehandlingRepository behandlingRepository;
    private PersonBasisTjeneste personBasisTjeneste;
    private AktørTjeneste aktørTjeneste;
    private final PdfGen pdfGen;

    @Inject
    public BrevGenerererTjeneste(
        BehandlingRepository behandlingRepository,
        PersonBasisTjeneste personBasisTjeneste,
        AktørTjeneste aktørTjeneste,
        PdfGen pdfGen) {
        this.behandlingRepository = behandlingRepository;
        this.personBasisTjeneste = personBasisTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.pdfGen = pdfGen;
    }

    public GenerertBrev genererPdf(BrevbestillingDto brevbestillingDto) {

        Behandling behandling = behandlingRepository.hentBehandling(brevbestillingDto.behandlingId());
        PartResponseDto mottaker = hentMottaker(behandling);

        // valider mal via regel hvis vedtaksbrev

        // lag brev json via datasamler og velg templateData
        var input = new TemplateInput(TemplateType.INNVILGELSE,
            new InnvilgelseTemplate(
                new FellesTemplateData()
            ));


        // konverter til pdf fra templateData
        byte[] pdf = pdfGen.lagPdf(input);

        return new GenerertBrev(
            pdf,
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
