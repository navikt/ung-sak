package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
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

    ArbeidsforholdAdministrasjonTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdAdministrasjonTjeneste(VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste,
                                                InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
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

        Collection<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = filter.getArbeidsforholdOverstyringer();
        var mapper = new ArbeidsforholdMapper(arbeidsforholdInformasjon.orElse(null));
        mapArbeidsforhold(mapper, yrkesaktiviteter, arbeidsforholdOverstyringer, inntektsmeldinger);

        if (param.getVurderArbeidsforhold() && mapper.harArbeidsforhold()) {
            var vurderinger = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref, iayGrunnlag);
            mapper.mapVurderinger(vurderinger);
        }

        return mapper.getArbeidsforhold();

    }

    void mapArbeidsforhold(ArbeidsforholdMapper mapper,
                           Collection<Yrkesaktivitet> yrkesaktiviteter,
                           Collection<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                           NavigableSet<Inntektsmelding> inntektsmeldinger) {

        mapper.utledArbeidsforholdFraYrkesaktiviteter(Objects.requireNonNull(yrkesaktiviteter));

        // ta inntektsmeldinger etter yrkesaktivitet (beriker med inntektsmeldinger som matcher angitt)
        mapper.utledArbeidsforholdFraInntektsmeldinger(Objects.requireNonNull(inntektsmeldinger));

        mapper.utledArbeidsforholdFraArbeidsforholdInformasjon(Objects.requireNonNull(arbeidsforholdOverstyringer));
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
