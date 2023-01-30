package no.nav.k9.sak.domene.abakus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.JournalpostId;

class SakInntektsmeldinger {

    private static Logger LOGGER = LoggerFactory.getLogger(SakInntektsmeldinger.class);

    private final Map<Key, Set<Inntektsmelding>> data = new LinkedHashMap<>();
    private final Map<Key, InntektArbeidYtelseGrunnlag> grunnlag = new LinkedHashMap<>();

    void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt, Inntektsmelding inntektsmelding) {
        data.computeIfAbsent(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), k -> new LinkedHashSet<>()).add(inntektsmelding);
    }

    void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt, InntektArbeidYtelseGrunnlag grunnlag) {
        this.grunnlag.put(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), grunnlag);
    }

    Set<Inntektsmelding> hentInntektsmeldingerSidenRef(Long behandlingId, UUID eksternReferanse) {
        if (eksternReferanse == null || data.isEmpty()) {
            return Set.of();
        }
        var key = data.keySet().stream().filter(it -> Objects.equals(it.behandlingId, behandlingId) && it.grunnlagEksternReferanse.equals(eksternReferanse)).findAny();
        var orignInntektsmeldinger = key.map(it -> data.get(it)).orElse(Set.of());
        var relevanteKeys = data.keySet()
            .stream()
            .filter(it -> Objects.equals(it.behandlingId, behandlingId) && !it.grunnlagEksternReferanse.equals(eksternReferanse))
            .toList();

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


    Set<Inntektsmelding> hentInntektsmeldingerKommetTom(Long behandlingId) {
        if (data.isEmpty()) {
            return Set.of();
        }

        var unikeJournalposter = data.values().stream()
            .flatMap(Collection::stream)
            .map(Inntektsmelding::getJournalpostId)
            .collect(Collectors.toSet());

        LOGGER.info("Journalposter før filtrering av relevante: " + unikeJournalposter);

        var keyOpt = data.keySet().stream().filter(it -> Objects.equals(it.behandlingId, behandlingId)).max(Comparator.comparing(Key::getOpprettetTidspunkt));
        if (keyOpt.isEmpty()) {
            return Set.of();
        }

        LOGGER.info("Alle keys" + data.keySet());
        LOGGER.info("Valgt key" + keyOpt.get());


        var key = keyOpt.get();
        var relevanteKeys = data.keySet()
            .stream()
            .filter(it -> Objects.equals(it.grunnlagEksternReferanse, key.grunnlagEksternReferanse) || it.opprettetTidspunkt.isBefore(key.opprettetTidspunkt))
            .toList();


        LOGGER.info("Relevante keys" + relevanteKeys);


        var nyeInntektsmeldinger = new LinkedHashSet<Inntektsmelding>();

        relevanteKeys.stream()
            .map(data::get)
            .forEach(nyeInntektsmeldinger::addAll);

        return nyeInntektsmeldinger;
    }

    private boolean erLiktEllerSenere(Inntektsmelding siste, Inntektsmelding denne) {
        return siste == null || siste.equals(denne) || denne.getKanalreferanse().compareTo(siste.getKanalreferanse()) >= 0;
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


        @Override
        public String toString() {
            return "Key{" +
                "behandlingId=" + behandlingId +
                ", grunnlagEksternReferanse=" + grunnlagEksternReferanse +
                ", opprettetTidspunkt=" + opprettetTidspunkt +
                '}';
        }
    }

}
