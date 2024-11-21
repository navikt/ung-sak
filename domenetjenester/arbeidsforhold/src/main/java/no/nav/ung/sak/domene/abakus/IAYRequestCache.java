package no.nav.ung.sak.domene.abakus;

import jakarta.enterprise.context.RequestScoped;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;

import java.util.*;

@RequestScoped
class IAYRequestCache {
    private final List<InntektArbeidYtelseGrunnlag> cacheGrunnlag = new ArrayList<>();

    void leggTil(InntektArbeidYtelseGrunnlag dto) {
        this.cacheGrunnlag.removeIf(g -> dto.getEksternReferanse().equals(g.getEksternReferanse()));
        this.cacheGrunnlag.add(dto);
    }

    InntektArbeidYtelseGrunnlag getGrunnlag(UUID grunnlagReferanse) {
        if (grunnlagReferanse == null) {
            return null;
        }
        return this.cacheGrunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), grunnlagReferanse)).findFirst().orElse(null);
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
}
