package no.nav.k9.sak.domene.abakus;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

class SakInntektsmeldinger {

    private final Map<Key, Set<Inntektsmelding>> data = new LinkedHashMap<>();
    private final Map<Key, InntektArbeidYtelseGrunnlag> grunnlag = new LinkedHashMap<>();

    void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt, Inntektsmelding inntektsmelding) {
        data.computeIfAbsent(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), k -> new LinkedHashSet<>()).add(inntektsmelding);
    }

    void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt, InntektArbeidYtelseGrunnlag grunnlag) {
        this.grunnlag.put(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), grunnlag);
    }

    Set<Inntektsmelding> hentInntektsmeldingerSidenRef(Long behandlingId, UUID eksternReferanse) {
        if (eksternReferanse == null) {
            return Set.of();
        }
        var key = data.keySet().stream().filter(it -> Objects.equals(it.behandlingId, behandlingId) && it.grunnlagEksternReferanse.equals(eksternReferanse)).findAny().orElseThrow();
        var orignInntektsmeldinger = data.get(key);
        var relevanteKeys = data.keySet()
            .stream()
            .filter(it -> Objects.equals(it.behandlingId, behandlingId) && !it.grunnlagEksternReferanse.equals(eksternReferanse))
            .collect(Collectors.toList());

        var nyeInntektsmeldinger = new LinkedHashSet<Inntektsmelding>();
        var senesteInntektsmelding = orignInntektsmeldinger.stream().max(Inntektsmelding.COMP_REKKEFØLGE).orElse(null);

        relevanteKeys.stream()
            .map(data::get)
            .forEach(it -> it.stream()
                .filter(im -> senesteInntektsmelding == null || erLiktEllerSenere(senesteInntektsmelding, im))
                .filter(at -> !orignInntektsmeldinger.contains(at))
                .forEach(nyeInntektsmeldinger::add));

        return nyeInntektsmeldinger;
    }

    private boolean erLiktEllerSenere(Inntektsmelding siste, Inntektsmelding denne) {
        return siste==null || siste.equals(denne) || denne.getKanalreferanse().compareTo(siste.getKanalreferanse()) >= 0;
    }

    static class Key {
        final Long behandlingId;
        final UUID grunnlagEksternReferanse;
        final LocalDateTime opprettetTidspunkt;

        Key(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime opprettetTidspunkt) {
            this.behandlingId = behandlingId;
            this.grunnlagEksternReferanse = grunnlagEksternReferanse;
            this.opprettetTidspunkt = opprettetTidspunkt;
        }

        LocalDateTime getOpprettetTidspunkt() {
            return opprettetTidspunkt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(behandlingId, key.behandlingId) &&
                Objects.equals(grunnlagEksternReferanse, key.grunnlagEksternReferanse) &&
                Objects.equals(opprettetTidspunkt, key.opprettetTidspunkt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(behandlingId, grunnlagEksternReferanse, opprettetTidspunkt);
        }
    }

}
