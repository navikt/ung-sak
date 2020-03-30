package no.nav.k9.sak.mottak.inntektsmelding.xml;

import static no.nav.k9.sak.mottak.inntektsmelding.xml.MottattDokumentXmlParserFeil.FACTORY;
import static no.nav.vedtak.felles.xml.XmlUtils.retrieveNameSpaceOfXML;

import java.util.HashMap;
import java.util.Map;

import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingWrapper;
import no.nav.vedtak.felles.integrasjon.felles.ws.JaxbHelper;

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
    public static MottattInntektsmeldingWrapper unmarshallXml(String xml) {
        final Object mottattDokument;
        final String namespace = hentNamespace(xml);

        try {
            DokumentParserKonfig dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
            if (dokumentParserKonfig == null) {
                throw FACTORY.ukjentNamespace(namespace, new IllegalStateException()).toException();
            }
            mottattDokument = JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, xml, dokumentParserKonfig.xsdLocation);
            return MottattInntektsmeldingWrapper.tilXmlWrapper(mottattDokument);
        } catch (Exception e) {
            throw FACTORY.uventetFeilVedParsingAvSoeknadsXml(namespace, e).toException();
        }
    }

    private static String hentNamespace(String xml) {
        final String namespace;
        try {
            namespace = retrieveNameSpaceOfXML(xml);
        } catch (Exception e) {
            throw FACTORY.uventetFeilVedParsingAvSoeknadsXml("ukjent", e).toException(); //$NON-NLS-1$
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
