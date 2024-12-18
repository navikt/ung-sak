package no.nav.ung.sak.formidling;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.RolleType;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.dto.PartResponseDto;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.InnvilgelseDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
public class BrevGenerererTjeneste {

    private BehandlingRepository behandlingRepository;
    private PersonBasisTjeneste personBasisTjeneste;
    private AktørTjeneste aktørTjeneste;
    private PdfGenKlient pdfGen;

    @Inject
    public BrevGenerererTjeneste(
        BehandlingRepository behandlingRepository,
        PersonBasisTjeneste personBasisTjeneste,
        AktørTjeneste aktørTjeneste,
        PdfGenKlient pdfGen) {
        this.behandlingRepository = behandlingRepository;
        this.personBasisTjeneste = personBasisTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.pdfGen = pdfGen;
    }

    public BrevGenerererTjeneste() {
    }

    public GenerertBrev generer(Brevbestilling brevbestilling) {
        Behandling behandling = behandlingRepository.hentBehandling(brevbestilling.behandlingId());
        PartResponseDto mottaker = hentMottaker(behandling);

        // valider mal via regel hvis vedtaksbrev

        // lag brev json via datasamler og velg templateData
        var input = new TemplateInput(TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                FellesDto.automatisk(new MottakerDto(mottaker.navn(), mottaker.id()))
            ));


        // konverter til pdf fra templateData
        PdfGenDokument dokument = pdfGen.lagDokument(input);

        return new GenerertBrev(
            dokument,
            mottaker,
            mottaker,
            brevbestilling.malType(),
            input.templateType()
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
