package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.typer.AktørId;

/**
 * Håndterer administrasjon(saksbehandlers input) vedrørende arbeidsforhold.
 */
@Dependent
public class ArbeidsforholdAdministrasjonTjeneste {

    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private boolean nyArbeidsforholdMapper;

    ArbeidsforholdAdministrasjonTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdAdministrasjonTjeneste(VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste,
                                                InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                @KonfigVerdi(value = "ARBEIDSFORHOLD_MAPPER_NY", defaultVerdi = "true") boolean nyArbeidsforholdMapper) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.nyArbeidsforholdMapper = nyArbeidsforholdMapper;
    }

    /**
     * Oppretter en builder for å lagre ned overstyringen av arbeidsforhold
     *
     * @param behandlingId behandlingen sin ID
     * @return buildern
     */
    public ArbeidsforholdInformasjonBuilder opprettBuilderFor(Long behandlingId) {
        return ArbeidsforholdInformasjonBuilder.oppdatere(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId));
    }

    /**
     * Rydder opp i inntektsmeldinger som blir erstattet
     *
     * @param behandlingId behandlingId
     * @param aktørId aktørId
     * @param builder ArbeidsforholdsOverstyringene som skal lagrers
     */
    public void lagre(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder builder) {
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandlingId, aktørId, builder);
    }

    public Set<InntektArbeidYtelseArbeidsforholdV2Dto> hentArbeidsforhold(BehandlingReferanse ref,
                                                                          InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                          UtledArbeidsforholdParametere param) {

        var inntektsmeldinger = new TreeSet<Inntektsmelding>(Inntektsmelding.COMP_REKKEFØLGE); // ta i rekkefølge mottatt
        inntektsmeldinger.addAll(inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer()));

        var arbeidsforholdInformasjon = iayGrunnlag.getArbeidsforholdInformasjon();
        var filter = new YrkesaktivitetFilter(arbeidsforholdInformasjon, iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId()));

        var yrkesaktiviteter = filter.getAlleYrkesaktiviteter();

        if (this.nyArbeidsforholdMapper) {
            var mapper = new ArbeidsforholdMapper(arbeidsforholdInformasjon.orElse(null));
            mapper.utledArbeidsforholdFraYrkesaktiviteter(yrkesaktiviteter);
            mapper.utledArbeidsforholdFraArbeidsforholdInformasjon(filter.getArbeidsforholdOverstyringer());

            // ta inntektsmeldinger etter yrkesaktivitet og arbeidsforhold informasjon (beriker med inntektsmeldinger som matcher angitt)
            mapper.utledArbeidsforholdFraInntektsmeldinger(inntektsmeldinger);

            if (param.getVurderArbeidsforhold() && mapper.harArbeidsforhold()) {
                var vurderinger = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref, iayGrunnlag);
                mapper.mapVurdering(vurderinger);
            }

            return mapper.getArbeidsforhold();
        } else {
            var mapper = new OldArbeidsforholdMapper(arbeidsforholdInformasjon);
            mapper.utledArbeidsforholdFraInntektsmeldinger(inntektsmeldinger);
            mapper.utledArbeidsforholdFraYrkesaktivteter(yrkesaktiviteter);
            mapper.utledArbeidsforholdFraArbeidsforholdInformasjon(filter.getArbeidsforholdOverstyringer());

            if (param.getVurderArbeidsforhold() && mapper.harArbeidsforhold()) {
                var vurderinger = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref, iayGrunnlag);
                mapper.mapVurdering(vurderinger);
            }

            return mapper.getArbeidsforhold();
        }
    }

    /**
     * Param klasse for å kunne ta inn parametere som styrer utleding av arbeidsforhold.
     */
    public static class UtledArbeidsforholdParametere {
        private final boolean vurderArbeidsforhold;

        public UtledArbeidsforholdParametere(boolean vurderArbeidsforhold) {
            this.vurderArbeidsforhold = vurderArbeidsforhold;
        }

        public boolean getVurderArbeidsforhold() {
            return vurderArbeidsforhold;
        }
    }

}
