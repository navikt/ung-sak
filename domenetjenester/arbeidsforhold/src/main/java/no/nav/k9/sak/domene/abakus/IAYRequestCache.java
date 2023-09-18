package no.nav.k9.sak.domene.abakus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import no.nav.abakus.iaygrunnlag.PersonIdent;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerMottattRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerRequest;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

@RequestScoped
class IAYRequestCache {
    private List<InntektArbeidYtelseGrunnlag> cacheGrunnlag = new ArrayList<>();
    private Map<InntektsmeldingCacheNøkkel, List<Inntektsmelding>> cacheInntektsmelding = new HashMap<>();

    void leggTil(InntektArbeidYtelseGrunnlag dto) {
        this.cacheGrunnlag.removeIf(g -> dto.getEksternReferanse().equals(g.getEksternReferanse()));
        this.cacheGrunnlag.add(dto);
    }

    void leggTilInntektsmeldinger(InntektsmeldingerRequest request, List<Inntektsmelding> ims) {
        var key = new InntektsmeldingCacheNøkkel(request.getSaksnummer(), request.getYtelseType(), request.getPerson());
        this.cacheInntektsmelding.put(key, ims);
    }

    InntektArbeidYtelseGrunnlag getGrunnlag(UUID grunnlagReferanse) {
        if (grunnlagReferanse == null) {
            return null;
        }
        return this.cacheGrunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), grunnlagReferanse)).findFirst().orElse(null);
    }

    List<Inntektsmelding> getInntektsmeldingerForSak(InntektsmeldingerRequest request) {
        if (request == null) {
            return null;
        }
        var key = new InntektsmeldingCacheNøkkel(request.getSaksnummer(), request.getYtelseType(), request.getPerson());
        return cacheInntektsmelding.get(key);
    }

    void invaliderInntektsmeldingerCacheForSak(InntektsmeldingerMottattRequest request) {
        if (request != null) {
            var key = new InntektsmeldingCacheNøkkel(request.getSaksnummer(), request.getYtelseType(), request.getAktør());
            cacheInntektsmelding.remove(key);
        }
    }


    UUID getSisteAktiveGrunnlagReferanse(UUID behandlingUuid) {
        return this.cacheGrunnlag.stream()
            .filter(it -> behandlingUuid.equals(it.getKoblingReferanse().orElse(null)))
            .filter(InntektArbeidYtelseGrunnlag::isAktiv)
            .sorted(Comparator.comparing(InntektArbeidYtelseGrunnlag::getOpprettetTidspunkt).reversed())
            .findFirst()
            .map(InntektArbeidYtelseGrunnlag::getEksternReferanse)
            .orElse(null);
    }

    private record InntektsmeldingCacheNøkkel(String saksnummer, YtelseType ytelseType,
                                              PersonIdent aktør) {
    }

}
