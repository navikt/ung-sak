package no.nav.ung.sak.formidling.informasjonsbrev;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.microsoftgraph.MicrosoftGraphTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.informasjonsbrev.innhold.InformasjonsbrevInnholdBygger;
import no.nav.ung.sak.formidling.informasjonsbrev.innhold.InformasjonsbrevInnholdByggerTypeRef;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.BrevAnsvarligDto;
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
    private Instance<InformasjonsbrevInnholdBygger<?>> informasjonsbrevInnholdByggere;
    private MicrosoftGraphTjeneste microsoftGraphTjeneste;
    private boolean enableBrevAnsvarlig;

    @Inject
    public InformasjonsbrevGenerererTjeneste(
        BehandlingRepository behandlingRepository,
        PdfGenKlient pdfGen,
        BrevMottakerTjeneste brevMottakerTjeneste,
        @Any Instance<InformasjonsbrevInnholdBygger<?>> informasjonsbrevInnholdByggere, MicrosoftGraphTjeneste microsoftGraphTjeneste,
        @KonfigVerdi(value = "ENABLE_BREV_ANSVARLIG", defaultVerdi = "true") boolean enableBrevAnsvarlig) {

        this.behandlingRepository = behandlingRepository;
        this.pdfGen = pdfGen;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
        this.informasjonsbrevInnholdByggere = informasjonsbrevInnholdByggere;
        this.microsoftGraphTjeneste = microsoftGraphTjeneste;
        this.enableBrevAnsvarlig = enableBrevAnsvarlig;
    }

    public InformasjonsbrevGenerererTjeneste() {
    }

    @WithSpan //WithSpan her for å kunne skille ventetid på semafor i opentelemetry
    public GenerertBrev genererInformasjonsbrev(InformasjonsbrevBestillingInput informasjonsbrevBestillingInput) {
        var behandling = behandlingRepository.hentBehandling(informasjonsbrevBestillingInput.behandlingId());

        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var bygger = bestemBygger(informasjonsbrevBestillingInput);
        var innhold = bygger.bygg(behandling, informasjonsbrevBestillingInput.getTypedInnhold());

        BrevAnsvarligDto brevAnsvarlig = lagBrevansvarlig(informasjonsbrevBestillingInput.bestillerIdent());
        var input = new TemplateInput(innhold.templateType(),
            new TemplateDto(
                FellesDto.lag(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr()), brevAnsvarlig),
                innhold.templateInnholdDto()
            ));

        PdfGenDokument dokument = pdfGen.lagDokument(input, informasjonsbrevBestillingInput.kunHtml());
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            DokumentMalType.GENERELT_FRITEKSTBREV,
            TemplateType.GENERELT_FRITEKSTBREV
        );
    }

    private InformasjonsbrevInnholdBygger<?> bestemBygger(InformasjonsbrevBestillingInput informasjonsbrevBestillingInput) {
        return informasjonsbrevInnholdByggere
            .select(new InformasjonsbrevInnholdByggerTypeRef.InformasjonsbrevInnholdByggerTypeRefLiteral(informasjonsbrevBestillingInput.dokumentMalType()))
            .get();
    }


    private BrevAnsvarligDto lagBrevansvarlig(String bestillerIdent) {
        if (!enableBrevAnsvarlig) {
            return new BrevAnsvarligDto(false, null, null);
        }
        return new BrevAnsvarligDto(
            false,
            microsoftGraphTjeneste.navnPåNavAnsatt(bestillerIdent).orElse(null),
            null
        );
    }

}

