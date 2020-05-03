package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultManglendePåkrevdeInntektsmeldingerTjeneste implements ManglendePåkrevdeInntektsmeldingerTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManglendePåkrevdeInntektsmeldingerTjeneste.class);

    private InntektsmeldingRegisterTjeneste inntektsmeldingTjeneste;
    private SøknadRepository søknadRepository;

    DefaultManglendePåkrevdeInntektsmeldingerTjeneste() {
        // CDI
    }

    @Inject
    public DefaultManglendePåkrevdeInntektsmeldingerTjeneste(InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste,
                                                             SøknadRepository søknadRepository) {
        this.inntektsmeldingTjeneste = inntektsmeldingArkivTjeneste;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse behandlingReferanse) {
        var result = new HashMap<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>>();
        boolean erEndringssøknad = erEndringssøknad(behandlingReferanse);
        boolean erIkkeEndringssøknad = !erEndringssøknad;

        final Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeInntektsmeldinger = inntektsmeldingTjeneste
            .utledManglendeInntektsmeldingerFraGrunnlagForVurdering(behandlingReferanse, erEndringssøknad);
        if (erIkkeEndringssøknad) {
            for (Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> entry : manglendeInntektsmeldinger.entrySet()) {
                LeggTilResultat.leggTil(result, AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, entry.getKey(), entry.getValue());
                LOGGER.info("Mangler inntektsmelding: arbeidsforholdRef={}", entry.getValue());
            }
        }
        return result;
    }

    private boolean erEndringssøknad(BehandlingReferanse referanse) {
        return søknadRepository.hentSøknadHvisEksisterer(referanse.getBehandlingId())
            .map(SøknadEntitet::erEndringssøknad)
            .orElse(false);
    }

}
