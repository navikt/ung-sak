package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;

import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.mottak.dokumentmottak.MottattDokumentException;
import no.nav.k9.sak.mottak.inntektsmelding.xml.MottattDokumentXmlParser;

@SuppressWarnings("rawtypes")
public class InntektsmeldingParser {

    public InntektsmeldingParser() {
    }

    @SuppressWarnings("unchecked")
    public List<InntektsmeldingBuilder> parseInntektsmeldinger(Collection<MottattDokument> mottatteDokumenter) {
        // wrap/dekod alle først
        var mottatt = new IdentityHashMap();
        var oversettere = new IdentityHashMap();
        for (var m : mottatteDokumenter) {
            var wrapper = xmlTilWrapper(m);
            mottatt.put(m, wrapper);
            oversettere.put(m, getDokumentOversetter(wrapper.getSkjemaType()));
        }

        // så lagre - dette gjør remote kall m.m.
        List<InntektsmeldingBuilder> inntektsmeldinger = new ArrayList<>();
        for (var m : mottatteDokumenter) {
            var wrapper = (MottattInntektsmeldingWrapper) mottatt.get(m);
            var oversetter = (MottattInntektsmeldingOversetter) oversettere.get(m);
            var im = oversetter.trekkUtData(wrapper, m);
            inntektsmeldinger.add(im);
        }
        Collections.sort(inntektsmeldinger, Comparator.comparing(InntektsmeldingBuilder::getKanalreferanse, Comparator.nullsLast(Comparator.naturalOrder())));
        return inntektsmeldinger;
    }

    public List<InntektsmeldingBuilder> parseInntektsmeldinger(MottattDokument... dokument) {
        return parseInntektsmeldinger(List.of(dokument));
    }

    public MottattInntektsmeldingWrapper xmlTilWrapper(MottattDokument dokument) {
        return MottattDokumentXmlParser.unmarshallXml(dokument.getJournalpostId(), dokument.getPayload());
    }

    private MottattInntektsmeldingOversetter<?> getDokumentOversetter(String namespace) {
        var annotationLiteral = new NamespaceRef.NamespaceRefLiteral(namespace);
        var instance = CDI.current().select(new TypeLiteralMottattDokumentOversetter(), annotationLiteral);

        if (instance.isAmbiguous()) {
            throw MottattDokumentException.FACTORY.flereImplementasjonerAvSkjemaType(namespace);
        } else if (instance.isUnsatisfied()) {
            throw MottattDokumentException.FACTORY.ukjentSkjemaType(namespace);
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
