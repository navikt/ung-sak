package no.nav.k9.sak.domene.iay.modell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

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
        if (internReferanse.getReferanse() == null)
            return EksternArbeidsforholdRef.nullRef();

        return referanser.stream()
            .filter(r -> Objects.equals(r.getInternReferanse(), internReferanse) && Objects.equals(r.getArbeidsgiver(), arbeidsgiver))
            .findFirst()
            .map(ArbeidsforholdReferanse::getEksternReferanse)
            .orElseThrow(
                () -> new IllegalStateException("Mangler eksternReferanse for internReferanse: " + internReferanse + ", arbeidsgiver: " + arbeidsgiver));
    }

    /**
     * @deprecated Bruk {@link ArbeidsforholdInformasjonBuilder} i stedet.
     */
    @Deprecated(forRemoval = true)
    public ArbeidsforholdReferanse opprettNyReferanse(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse,
                                                      EksternArbeidsforholdRef eksternReferanse) {
        final ArbeidsforholdReferanse arbeidsforholdReferanse = new ArbeidsforholdReferanse(arbeidsgiver,
            internReferanse, eksternReferanse);
        referanser.add(arbeidsforholdReferanse);
        return arbeidsforholdReferanse;
    }

    void leggTilNyReferanse(ArbeidsforholdReferanse arbeidsforholdReferanse) {
        referanser.add(arbeidsforholdReferanse);
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

    void leggTilOverstyring(ArbeidsforholdOverstyring build) {
        this.overstyringer.add(build);
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
