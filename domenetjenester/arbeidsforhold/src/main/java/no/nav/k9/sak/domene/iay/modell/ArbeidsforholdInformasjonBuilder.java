package no.nav.k9.sak.domene.iay.modell;

import java.util.Optional;

import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class ArbeidsforholdInformasjonBuilder {

    private final ArbeidsforholdInformasjon kladd;

    private ArbeidsforholdInformasjonBuilder(ArbeidsforholdInformasjon kladd) {
        this.kladd = kladd;
    }

    public static ArbeidsforholdInformasjonBuilder oppdatere(ArbeidsforholdInformasjon oppdatere) {
        return new ArbeidsforholdInformasjonBuilder(new ArbeidsforholdInformasjon(oppdatere));
    }

    public ArbeidsforholdOverstyringBuilder getOverstyringBuilderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return kladd.getOverstyringBuilderFor(arbeidsgiver, ref);
    }

    public ArbeidsforholdInformasjonBuilder tilbakestillOverstyringer() {
        kladd.tilbakestillOverstyringer();
        return this;
    }

    public ArbeidsforholdInformasjonBuilder leggTil(ArbeidsforholdOverstyringBuilder overstyringBuilder) {
        if (!overstyringBuilder.isOppdatering()) {
            kladd.leggTilOverstyring(overstyringBuilder.build());
        }
        return this;
    }

    public ArbeidsforholdInformasjon build() {
        return kladd;
    }

    public void fjernOverstyringerSomGjelder(Arbeidsgiver arbeidsgiver) {
        kladd.fjernOverstyringerSomGjelder(arbeidsgiver);
    }

    public void leggTil(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse, EksternArbeidsforholdRef eksternReferanse) {
        kladd.leggTilNyReferanse(new ArbeidsforholdReferanse(arbeidsgiver, internReferanse, eksternReferanse));
    }

    public void leggTilNyReferanse(ArbeidsforholdReferanse arbeidsforholdReferanse) {
        kladd.leggTilNyReferanse(arbeidsforholdReferanse);
    }

    public static ArbeidsforholdInformasjonBuilder oppdatere(Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        return oppdatere(InntektArbeidYtelseGrunnlagBuilder.oppdatere(grunnlag).getInformasjon());
    }

    public static ArbeidsforholdInformasjonBuilder builder(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var arbeidInfo = arbeidsforholdInformasjon.map(ai -> new ArbeidsforholdInformasjon(ai)).orElseGet(() -> new ArbeidsforholdInformasjon());
        return new ArbeidsforholdInformasjonBuilder(arbeidInfo);
    }
}
