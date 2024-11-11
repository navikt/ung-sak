package no.nav.ung.sak.domene.iay.modell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;

public class ArbeidsforholdInformasjon {

    @ChangeTracked
    private Set<ArbeidsforholdReferanse> referanser = new LinkedHashSet<>();

    @ChangeTracked
    private List<ArbeidsforholdOverstyring> overstyringer = new ArrayList<>();

    ArbeidsforholdInformasjon() {
    }

    public ArbeidsforholdInformasjon(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        for (ArbeidsforholdReferanse arbeidsforholdReferanse : arbeidsforholdInformasjon.referanser) {
            final ArbeidsforholdReferanse referanseEntitet = new ArbeidsforholdReferanse(arbeidsforholdReferanse);
            this.referanser.add(referanseEntitet);
        }
        for (ArbeidsforholdOverstyring arbeidsforholdOverstyringEntitet : arbeidsforholdInformasjon.overstyringer) {
            final ArbeidsforholdOverstyring overstyringEntitet = new ArbeidsforholdOverstyring(arbeidsforholdOverstyringEntitet);
            this.overstyringer.add(overstyringEntitet);
        }
    }

    public Collection<ArbeidsforholdReferanse> getArbeidsforholdReferanser() {
        return Collections.unmodifiableSet(this.referanser);
    }

    public List<ArbeidsforholdOverstyring> getOverstyringer() {
        return Collections.unmodifiableList(this.overstyringer);
    }

    public EksternArbeidsforholdRef finnEkstern(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse) {
        return validerOgFinnEksternReferanse(arbeidsgiver, internReferanse);
    }

    private EksternArbeidsforholdRef validerOgFinnEksternReferanse(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse) {
        if (internReferanse.getReferanse() == null) {
            return EksternArbeidsforholdRef.nullRef();
        }

        List<EksternArbeidsforholdRef> eksternReferanser = referanser.stream()
            .filter(r0 -> Objects.equals(r0.getArbeidsgiver(), arbeidsgiver))
            .filter(r1 -> Objects.equals(r1.getInternReferanse(), internReferanse))
            .map(ArbeidsforholdReferanse::getEksternReferanse)
            .toList();

        if (eksternReferanser.isEmpty()) {
            throw new IllegalStateException("Mangler eksternReferanse for internReferanse: " + internReferanse + ", arbeidsgiver: " + arbeidsgiver);
        } else if (eksternReferanser.size() > 1) {
            throw new IllegalStateException("Har mer enn 1 eksternReferanse for internReferanse: " + internReferanse + ", arbeidsgiver: " + arbeidsgiver + ": " + eksternReferanser);
        } else {
            return eksternReferanser.get(0);
        }
    }

    /**
     * @deprecated Bruk {@link ArbeidsforholdInformasjonBuilder} i stedet.
     */
    @Deprecated(forRemoval = true)
    public ArbeidsforholdReferanse opprettNyReferanse(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse,
                                                      EksternArbeidsforholdRef eksternReferanse) {
        var arbeidsforholdReferanse = new ArbeidsforholdReferanse(arbeidsgiver, internReferanse, eksternReferanse);
        leggTilNyReferanse(arbeidsforholdReferanse);
        return arbeidsforholdReferanse;
    }

    void leggTilNyReferanse(ArbeidsforholdReferanse arbeidsforholdReferanse) {
        var arbeidsgiver = arbeidsforholdReferanse.getArbeidsgiver();
        var nyRef = arbeidsforholdReferanse.getInternReferanse();
        referanser.add(arbeidsforholdReferanse);

        // valider etter å ha lagt til ny
        validerOgFinnEksternReferanse(arbeidsgiver, nyRef);
    }

    ArbeidsforholdOverstyringBuilder getOverstyringBuilderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return ArbeidsforholdOverstyringBuilder.oppdatere(this.overstyringer
            .stream()
            .filter(ov -> ov.getArbeidsgiver().equals(arbeidsgiver)
                && ov.getArbeidsforholdRef().gjelderFor(ref))
            .findFirst())
            .medArbeidsforholdRef(ref)
            .medArbeidsgiver(arbeidsgiver);
    }

    void leggTilOverstyring(ArbeidsforholdOverstyring overstyring) {
        this.overstyringer.remove(overstyring);
        this.overstyringer.add(overstyring);
    }

    void tilbakestillOverstyringer() {
        this.overstyringer.clear();
    }

    void fjernOverstyringerSomGjelder(Arbeidsgiver arbeidsgiver) {
        this.overstyringer.removeIf(ov -> arbeidsgiver.equals(ov.getArbeidsgiver()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof ArbeidsforholdInformasjon))
            return false;
        ArbeidsforholdInformasjon that = (ArbeidsforholdInformasjon) o;
        return Objects.equals(referanser, that.referanser) &&
            Objects.equals(overstyringer, that.overstyringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referanser, overstyringer);
    }

    @Override
    public String toString() {
        return "ArbeidsforholdInformasjonEntitet{" +
            "referanser=" + referanser +
            ", overstyringer=" + overstyringer +
            '}';
    }

}
