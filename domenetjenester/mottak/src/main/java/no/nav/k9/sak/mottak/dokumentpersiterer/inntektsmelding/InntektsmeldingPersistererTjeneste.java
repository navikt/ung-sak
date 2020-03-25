package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingInnhold;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingMottaker;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.xml.MottattDokumentXmlParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@SuppressWarnings("rawtypes")
@ApplicationScoped
public class InntektsmeldingPersistererTjeneste {

    private Instance<InntektsmeldingMottaker> inntektsmeldingMottakere;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    InntektsmeldingPersistererTjeneste() {
    }

    @Inject
    public InntektsmeldingPersistererTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                              @Any Instance<InntektsmeldingMottaker> inntektsmeldingMottakere) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingMottakere = inntektsmeldingMottakere;
    }

    @SuppressWarnings("unchecked")
    public void leggInntektsmeldingerTilBehandling(Behandling behandling, List<MottattDokument> mottatteDokumenter) {
        var mottaker = BehandlingTypeRef.Lookup.find(InntektsmeldingMottaker.class, inntektsmeldingMottakere, behandling.getFagsakYtelseType(), behandling.getType());

        // wrap/dekod alle først
        var mottatt = new IdentityHashMap();
        var oversettere = new IdentityHashMap();
        for (var m : mottatteDokumenter) {
            var wrapper = xmlTilWrapper(m);
            mottatt.put(m, wrapper);
            oversettere.put(m, getDokumentOversetter(wrapper.getSkjemaType()));
        }

        // så lagre - dette gjør remote kall m.m.
        List<InntektsmeldingInnhold> inntektsmeldinger = new ArrayList<>();
        for (var m : mottatteDokumenter) {
            var wrapper = (MottattInntektsmeldingWrapper) mottatt.get(m);
            var oversetter = (MottattInntektsmeldingOversetter) oversettere.get(m);
            var im = oversetter.trekkUtData(wrapper, m, behandling);
            inntektsmeldinger.add(im);
        }

        var ref = BehandlingReferanse.fra(behandling);
        mottaker.ifPresent(m -> m.mottattInntektsmelding(ref, inntektsmeldinger));

        inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldinger);
    }

    public void leggInntektsmeldingTilBehandling(Behandling behandling, MottattDokument... dokument) {
        leggInntektsmeldingerTilBehandling(behandling, List.of(dokument));
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
