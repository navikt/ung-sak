package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;

@RequestScoped
class BeregningRequestCache {
    private List<BeregningsgrunnlagGrunnlag> cacheGrunnlag = new ArrayList<>();

    void leggTil(BeregningsgrunnlagGrunnlag dto) {
        this.cacheGrunnlag.removeIf(g -> dto.getEksternReferanse().equals(g.getEksternReferanse()));
        this.cacheGrunnlag.add(dto);
    }

    BeregningsgrunnlagGrunnlag getGrunnlag(UUID grunnlagReferanse) {
        if (grunnlagReferanse == null) {
            return null;
        }
        return this.cacheGrunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), grunnlagReferanse)).findFirst().orElse(null);
    }

    UUID getSisteAktiveGrunnlagReferanse(UUID behandlingUuid) {
        return this.cacheGrunnlag.stream()
            .filter(it -> behandlingUuid.equals(it.getKoblingReferanse().orElse(null)))
            .filter(BeregningsgrunnlagGrunnlag::getAktiv)
            .findFirst()
            .map(BeregningsgrunnlagGrunnlag::getEksternReferanse)
            .orElse(null);
    }
}
