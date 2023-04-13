package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.INNTEKTSMELDING_KODE)
public class InntekstsmeldingDokumentValidator implements DokumentValidator {

    private final InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();
    private boolean zeroErKrav;

    @Inject
    public InntekstsmeldingDokumentValidator(@KonfigVerdi(value = "OMS_ZERO_REFUSJON_ER_KRAV", defaultVerdi = "false") boolean zeroErKrav) {
        this.zeroErKrav = zeroErKrav;
    }

    public InntekstsmeldingDokumentValidator() {
        // CDI Proxy
    }

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> inntektsmeldinger) {
        // TODO hvorfor er ikke validering lik i validerDokument og i validerDokumenter?
        var mottattBrevkoder = inntektsmeldinger.stream().map(MottattDokument::getType).toList();
        int i = 0;
        for (var m : inntektsmeldinger) {
            var brevkode = mottattBrevkoder.get(i++);
            Brevkode forventetBrevkode = Brevkode.INNTEKTSMELDING;
            if (!Objects.equals(brevkode, forventetBrevkode)) {
                throw new IllegalArgumentException("Forventet brevkode: " + forventetBrevkode + ", fikk: " + brevkode);
            }
            if (behandlingId == null && m.harPayload()) {
                inntektsmeldingParser.xmlTilWrapper(m); // gjør en tidlig validering
            }
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        // TODO hvorfor er ikke validering lik i validerDokument og i validerDokumenter?
        List<InntektsmeldingBuilder> builders = inntektsmeldingParser.parseInntektsmeldinger(mottattDokument);
        for (InntektsmeldingBuilder builder : builders) {
            //builder har validering
            boolean ignorerValideringInternArbeidsforhold = true;
            builder.build(ignorerValideringInternArbeidsforhold, zeroErKrav);
        }

    }

}
