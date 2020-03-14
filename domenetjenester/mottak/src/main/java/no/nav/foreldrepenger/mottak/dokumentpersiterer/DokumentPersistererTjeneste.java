package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.xml.MottattDokumentXmlParser;

@SuppressWarnings("rawtypes")
@ApplicationScoped
public class DokumentPersistererTjeneste {

    public DokumentPersistererTjeneste() {
    }

    public MottattDokumentWrapper xmlTilWrapper(MottattDokument dokument) {
        return MottattDokumentXmlParser.unmarshallXml(dokument.getPayload());
    }

    @SuppressWarnings("unchecked")
    public void persisterDokumentinnhold(MottattDokumentWrapper wrapper, MottattDokument dokument, Behandling behandling, Optional<LocalDate> gjelderFra) {
        MottattDokumentOversetter dokumentOversetter = getDokumentOversetter(wrapper.getSkjemaType());
        dokumentOversetter.trekkUtDataOgPersister(wrapper, dokument, behandling, gjelderFra);
    }

    public void persisterDokumentinnhold(MottattDokument dokument, Behandling behandling) {
        MottattDokumentWrapper dokumentWrapper = xmlTilWrapper(dokument);
        persisterDokumentinnhold(dokumentWrapper, dokument, behandling, Optional.empty());
    }

    private MottattDokumentOversetter<?> getDokumentOversetter(String namespace) {
        NamespaceRef.NamespaceRefLiteral annotationLiteral = new NamespaceRef.NamespaceRefLiteral(namespace);

        Instance<MottattDokumentOversetter<?>> instance = CDI.current().select(new TypeLiteralMottattDokumentOversetter(), annotationLiteral);

        if (instance.isAmbiguous()) {
            throw MottattDokumentFeil.FACTORY.flereImplementasjonerAvSkjemaType(namespace).toException();
        } else if (instance.isUnsatisfied()) {
            throw MottattDokumentFeil.FACTORY.ukjentSkjemaType(namespace).toException();
        }
        MottattDokumentOversetter<?> minInstans = instance.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    private static final class TypeLiteralMottattDokumentOversetter extends TypeLiteral<MottattDokumentOversetter<?>> {
    }

}
