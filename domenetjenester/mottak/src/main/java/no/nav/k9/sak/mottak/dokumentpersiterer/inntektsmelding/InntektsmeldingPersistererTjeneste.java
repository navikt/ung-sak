package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.xml.MottattDokumentXmlParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@SuppressWarnings("rawtypes")
@ApplicationScoped
public class InntektsmeldingPersistererTjeneste {

    public InntektsmeldingPersistererTjeneste() {
    }

    public MottattInntektsmeldingWrapper xmlTilWrapper(MottattDokument dokument) {
        return MottattDokumentXmlParser.unmarshallXml(dokument.getPayload());
    }

    @SuppressWarnings("unchecked")
    public InntektsmeldingInnhold persisterDokumentinnhold(MottattDokument dokument, Behandling behandling, Optional<LocalDate> gjelderFra) {
        var dokumentWrapper = xmlTilWrapper(dokument);
        MottattInntektsmeldingOversetter dokumentOversetter = getDokumentOversetter(dokumentWrapper.getSkjemaType());
        return dokumentOversetter.trekkUtDataOgPersister(dokumentWrapper, dokument, behandling, gjelderFra);
    }

    public InntektsmeldingInnhold persisterDokumentinnhold(MottattDokument dokument, Behandling behandling) {
        return persisterDokumentinnhold(dokument, behandling, Optional.empty());
    }

    private MottattInntektsmeldingOversetter<?> getDokumentOversetter(String namespace) {
        var annotationLiteral = new NamespaceRef.NamespaceRefLiteral(namespace);
        var instance = CDI.current().select(new TypeLiteralMottattDokumentOversetter(), annotationLiteral);

        if (instance.isAmbiguous()) {
            throw MottattDokumentFeil.FACTORY.flereImplementasjonerAvSkjemaType(namespace).toException();
        } else if (instance.isUnsatisfied()) {
            throw MottattDokumentFeil.FACTORY.ukjentSkjemaType(namespace).toException();
        }
        var minInstans = instance.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    private static final class TypeLiteralMottattDokumentOversetter extends TypeLiteral<MottattInntektsmeldingOversetter<?>> {
    }

}
