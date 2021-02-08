package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.InntektsmeldingDokumentType;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@ApplicationScoped
@InntektsmeldingDokumentType
public class InntekstsmeldingDokumentValidator implements DokumentValidator {

    private final InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    @Override
    public void validerDokumenter(String behandlingId, Collection<MottattDokument> inntektsmeldinger) {
        //TODO hvorfor er ikke validering lik i validerDokument og i validerDokumenter?
        inntektsmeldinger.forEach((m -> {
            if (behandlingId == null && m.harPayload()) {
                inntektsmeldingParser.xmlTilWrapper(m); // gjør en tidlig validering
            }
        }));
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        //TODO hvorfor er ikke validering lik i validerDokument og i validerDokumenter?
        inntektsmeldingParser.parseInntektsmeldinger(mottattDokument);
    }

}
