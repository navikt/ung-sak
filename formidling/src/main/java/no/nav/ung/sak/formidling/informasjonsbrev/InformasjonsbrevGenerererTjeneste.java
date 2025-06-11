package no.nav.ung.sak.formidling.informasjonsbrev;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.BrevGenereringSemafor;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.GenerellFritekstbrevDto;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InformasjonsbrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InformasjonsbrevGenerererTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private PdfGenKlient pdfGen;
    private BrevMottakerTjeneste brevMottakerTjeneste;

    @Inject
    public InformasjonsbrevGenerererTjeneste(
        BehandlingRepository behandlingRepository,
        PdfGenKlient pdfGen,
        BrevMottakerTjeneste brevMottakerTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.pdfGen = pdfGen;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
    }

    public InformasjonsbrevGenerererTjeneste() {
    }

    @WithSpan
    public GenerertBrev genererInformasjonsbrev(InformasjonsbrevRequest informasjonsbrevRequest) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererInformasjonsbrev(informasjonsbrevRequest));
    }

    @WithSpan //WithSpan her for å kunne skille ventetid på semafor i opentelemetry
    private GenerertBrev doGenererInformasjonsbrev(InformasjonsbrevRequest informasjonsbrevRequest) {
        var behandling = behandlingRepository.hentBehandling(informasjonsbrevRequest.behandlingId());

        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);

        var input = new TemplateInput(TemplateType.GENERELT_FRITEKSTBREV,
            new TemplateDto(
                FellesDto.manuell(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                new GenerellFritekstbrevDto(
                    informasjonsbrevRequest.fritekstbrev().overskrift(),
                    informasjonsbrevRequest.fritekstbrev().brødtekst())

            )
        );

        PdfGenDokument dokument = pdfGen.lagDokument(input, informasjonsbrevRequest.kunHtml());
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            DokumentMalType.GENERELT_FRITEKSTBREV,
            TemplateType.GENERELT_FRITEKSTBREV
        );
    }


}

