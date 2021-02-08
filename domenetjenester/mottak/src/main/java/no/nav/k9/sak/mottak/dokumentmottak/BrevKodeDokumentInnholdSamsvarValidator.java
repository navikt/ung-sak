package no.nav.k9.sak.mottak.dokumentmottak;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.mottak.dokumentmottak.søknad.SøknadParser;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;

@Dependent
public class BrevKodeDokumentInnholdSamsvarValidator {

    private SøknadParser søknadParser;
    private InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    @Inject
    public BrevKodeDokumentInnholdSamsvarValidator(SøknadParser søknadParser) {
        this.søknadParser = søknadParser;
    }

    public void validerSamsvarBrevkodeOgInnhold(MottattDokument mottattDokument) {
        Brevkode brevkode = mottattDokument.getType();
        DokumentInnholdType innholdType = analyserInnhold(mottattDokument);

        boolean erInntektsmelding = innholdType == DokumentInnholdType.INNTEKTSMELDING && brevkode == Brevkode.INNTEKTSMELDING;
        boolean erSøknadUtbetalingOMS = innholdType == DokumentInnholdType.SØKNAD_UTBETALING_OMS && brevkode == Brevkode.SØKNAD_UTBETALING_OMS;
        boolean kanVæreLegeerklæring = innholdType == DokumentInnholdType.ANNET && brevkode == Brevkode.LEGEERKLÆRING;
        boolean noeUdefinert = innholdType == DokumentInnholdType.ANNET && brevkode == Brevkode.UDEFINERT;

        boolean erOK = erInntektsmelding || erSøknadUtbetalingOMS || kanVæreLegeerklæring || noeUdefinert;
        if (!erOK) {
            throw new DokumentValideringException("Mottok dokument hvor kombinasjon av innhold og brevkode var ugyldig. Har innhold av type " + innholdType + " og brevkode " + brevkode + ". Gjelder journlpostId " + mottattDokument.getJournalpostId().getVerdi());
        }
    }

    private DokumentInnholdType analyserInnhold(MottattDokument mottattDokument) {
        try {
            inntektsmeldingParser.parseInntektsmeldinger(mottattDokument);
            return DokumentInnholdType.INNTEKTSMELDING;
        } catch (Exception e) {
            //fallthrough
        }
        try {
            Søknad søknad = søknadParser.parseSøknad(mottattDokument);
            if (søknad.getYtelse() instanceof OmsorgspengerUtbetaling) {
                return DokumentInnholdType.SØKNAD_UTBETALING_OMS;
            } else if (søknad.getYtelse() instanceof PleiepengerSyktBarn) {
                return DokumentInnholdType.SØKNAD_PLEIEPENGER_SYKT_BARN;
            }
        } catch (Exception e) {
            //fallthrough
        }

        return DokumentInnholdType.ANNET;
    }

    enum DokumentInnholdType {
        INNTEKTSMELDING,
        SØKNAD_UTBETALING_OMS,
        SØKNAD_PLEIEPENGER_SYKT_BARN,
        ANNET
    }

}

