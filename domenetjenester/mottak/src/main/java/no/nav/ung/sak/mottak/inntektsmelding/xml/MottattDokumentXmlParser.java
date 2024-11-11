package no.nav.ung.sak.mottak.inntektsmelding.xml;

import java.util.HashMap;
import java.util.Map;

import no.nav.ung.sak.mottak.inntektsmelding.MottattInntektsmeldingException;
import no.nav.ung.sak.mottak.inntektsmelding.MottattInntektsmeldingWrapper;
import no.nav.ung.sak.typer.JournalpostId;

public final class MottattDokumentXmlParser {

    private static Map<String, DokumentParserKonfig> SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER = new HashMap<>();

    static {
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.NAMESPACE,
            new DokumentParserKonfig(no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.JAXB_CLASS,
                no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.XSD_LOCATION));
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants.NAMESPACE,
            new DokumentParserKonfig(no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants.JAXB_CLASS,
                no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants.XSD_LOCATION));
    }

    private MottattDokumentXmlParser() {
    }

    @SuppressWarnings("rawtypes")
    public static MottattInntektsmeldingWrapper unmarshallXml(JournalpostId journalpostId, String xml) {
        final Object mottattDokument;
        final String namespace = hentNamespace(xml);

        try {
            DokumentParserKonfig dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
            if (dokumentParserKonfig == null) {
                throw MottattInntektsmeldingException.ukjentNamespace(namespace);
            }
            mottattDokument = JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, xml, dokumentParserKonfig.xsdLocation);
            return MottattInntektsmeldingWrapper.tilXmlWrapper(journalpostId, mottattDokument);
        } catch (Exception e) {
            throw MottattInntektsmeldingException.uventetFeilVedParsingAvXml(namespace, e);
        }
    }

    private static String hentNamespace(String xml) {
        final String namespace;
        try {
            namespace = JaxbHelper.retrieveNameSpaceOfXML(xml);
        } catch (Exception e) {
            throw MottattInntektsmeldingException.uventetFeilVedParsingAvXml("ukjent", e); //$NON-NLS-1$
        }
        return namespace;
    }

    private static class DokumentParserKonfig {
        Class<?> jaxbClass;
        String xsdLocation;

        DokumentParserKonfig(Class<?> jaxbClass, String xsdLocation) {
            this.jaxbClass = jaxbClass;
            this.xsdLocation = xsdLocation;
        }
    }
}
