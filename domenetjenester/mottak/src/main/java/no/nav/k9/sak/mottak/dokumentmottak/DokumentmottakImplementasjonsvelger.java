package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.mottak.dokumentmottak.søknad.SøknadParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.Ytelse;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;


public class DokumentmottakImplementasjonsvelger<T> {

    private static final Logger logger = LoggerFactory.getLogger(DokumentmottakImplementasjonsvelger.class);

    private Instance<T> implementasjoner;
    private SøknadParser søknadParser;

    public DokumentmottakImplementasjonsvelger(Instance<T> implementasjoner, SøknadParser søknadParser) {
        this.implementasjoner = implementasjoner;
        this.søknadParser = søknadParser;
    }

    public T velgImplementasjon(Collection<MottattDokument> mottattDokument) {
        int antallTotalt = mottattDokument.size();
        long antallXml = antallMedXml(mottattDokument);
        if (antallTotalt == antallXml) {
            return velgInntektsmeldingImplementasjon(mottattDokument);
        }
        long antallJson = antallMedJson(mottattDokument);
        if (antallTotalt == antallJson) {
            return velgSøknadImplementasjon(mottattDokument);
        }
        throw new IllegalArgumentException("Støtter kun å motta bare xml, eller bare json. Fikk " + antallTotalt + " antall dokumenter, hvorav " + antallXml + " antall XML-dokumenter, og " + antallJson + " antall JSON-dokumenter");
    }

    private T velgSøknadImplementasjon(Collection<MottattDokument> mottattDokument) {
        Class<? extends Ytelse> ytelseklasse = ytelseISøknader(mottattDokument);
        Instance<T> instans = implementasjoner.select(new SøknadDokumentType.SøknadDokumentTypeLiteral(ytelseklasse));
        if (instans.isUnsatisfied()) {
            throw new IllegalArgumentException("Mangler støtte for søknad med ytelse " + ytelseklasse);
        }
        if (instans.isAmbiguous()) {
            throw new IllegalArgumentException("Har flere implementasjoner for søknad med ytelse " + ytelseklasse + " : " + instans);
        }
        loggEventueltAvvikMedBrevkodeForSøknad(ytelseklasse, mottattDokument);
        return instans.get();
    }

    private T velgInntektsmeldingImplementasjon(Collection<MottattDokument> mottattDokument) {
        Instance<T> instans = implementasjoner.select(new InntektsmeldingDokumentType.InntektsmeldingDokumentTypeLiteral());
        if (instans.isUnsatisfied()) {
            throw new IllegalArgumentException("Mangler støtte for inntektsmelding");
        }
        if (instans.isAmbiguous()) {
            throw new IllegalArgumentException("Har flere implementasjoner for inntektsmelding: " + instans);
        }
        loggEventueltAvvikMedBrevkodeForInntektsmelding(mottattDokument);
        return instans.get();
    }

    public Class<? extends Ytelse> ytelseISøknader(Collection<MottattDokument> mottattDokument) {
        Collection<Søknad> søknader = søknadParser.parseSøknader(mottattDokument);
        var ytelseKlasser = søknader.stream().map(s -> s.getYtelse().getClass()).collect(Collectors.toSet());
        if (ytelseKlasser.size() == 1) {
            return ytelseKlasser.iterator().next();
        }
        throw new IllegalArgumentException("Støtter ikke å behandle flere ulike typer søknader samtidig. Fikk: " + ytelseKlasser);
    }

    public static long antallMedXml(Collection<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream().filter(DokumentmottakImplementasjonsvelger::inneholderXml).count();
    }

    public static boolean inneholderXml(MottattDokument mottattDokument) {
        return erXml(mottattDokument.getPayload());
    }

    public static boolean erXml(String payload) {
        return payload != null && payload.substring(0, Math.min(50, payload.length())).trim().startsWith("<");
    }

    public static long antallMedJson(Collection<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream().filter(DokumentmottakImplementasjonsvelger::inneholderJson).count();
    }

    public static boolean inneholderJson(MottattDokument mottattDokument) {
        return erJson(mottattDokument.getPayload());
    }

    public static boolean erJson(String payload) {
        return payload != null && payload.substring(0, Math.min(50, payload.length())).trim().startsWith("{");
    }

    private static void loggEventueltAvvikMedBrevkodeForSøknad(Class<? extends Ytelse> ytelseklasse, Collection<MottattDokument> mottattDokument) {
        if (OmsorgspengerUtbetaling.class.equals(ytelseklasse)) {
            mottattDokument.stream()
                .filter(md -> md.getType() != Brevkode.SØKNAD_UTBETALING_OMS)
                .forEach(md -> logger.info("Avvik mellom brevkode {} og innhold i dokument {}. Innhold i dokument viser søknad om ytelse {}. Ignorerer brevkode.", md.getType(), md.getJournalpostId(), ytelseklasse.getName()));
        }
    }

    private static void loggEventueltAvvikMedBrevkodeForInntektsmelding(Collection<MottattDokument> mottattDokument) {
        mottattDokument.stream()
            .filter(md -> md.getType() != Brevkode.INNTEKTSMELDING)
            .forEach(md -> logger.info("Avvik mellom brevkode {} og innhold i dokument {}. Innhold i dokument er XML, som indikerer inntektsmelding. Ignorerer brevkode.", md.getType(), md.getJournalpostId()));
    }
}
