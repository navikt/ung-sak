package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.mottak.inntektsmelding.xml.MottattDokumentXmlParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@SuppressWarnings("rawtypes")
@Dependent
public class InntektsmeldingParser {

    @Inject
    public InntektsmeldingParser() {
    }

    @SuppressWarnings("unchecked")
    private List<InntektsmeldingBuilder> parseInntektsmeldinger(Behandling behandling, List<MottattDokument> mottatteDokumenter) {
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
            var im = oversetter.trekkUtData(wrapper, m, behandling);
            inntektsmeldinger.add(im);
        }
        return inntektsmeldinger;
    }

    public List<InntektsmeldingBuilder> parseInntektsmeldinger(Behandling behandling, MottattDokument... dokument) {
        return parseInntektsmeldinger(behandling, List.of(dokument));
    }

    public MottattInntektsmeldingWrapper xmlTilWrapper(MottattDokument dokument) {
        return MottattDokumentXmlParser.unmarshallXml(dokument.getPayload());
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
