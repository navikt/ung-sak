package no.nav.ung.sak.domene.abakus.mapping;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.ArbeidsforholdRefDto;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdInformasjon;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdOverstyringDto;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdReferanseDto;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.ung.sak.domene.iay.modell.BekreftetPermisjon;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.OrgNummer;
import no.nav.ung.sak.typer.Stillingsprosent;

final class MapArbeidsforholdInformasjon {

    private static final Comparator<ArbeidsforholdOverstyringDto> COMP_ARBEIDSFORHOLD_OVERSTYRING = Comparator
        .comparing((ArbeidsforholdOverstyringDto ov) -> ov.getArbeidsgiver().getIdent())
        .thenComparing(ov -> ov.getArbeidsforholdRef() == null ? null : ov.getArbeidsforholdRef().getAbakusReferanse(),
            Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<ArbeidsforholdReferanseDto> COMP_ARBEIDSFORHOLD_REFERANSE = Comparator
        .comparing((ArbeidsforholdReferanseDto ref) -> ref.getArbeidsgiver().getIdent())
        .thenComparing(ref -> ref.getArbeidsforholdReferanse() == null ? null : ref.getArbeidsforholdReferanse().getAbakusReferanse(),
            Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<Periode> COMP_PERIODE = Comparator
        .comparing((Periode per) -> per.getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(per -> per.getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private MapArbeidsforholdInformasjon() {
        // hidden
    }

    static class MapFraDto {
        private InntektArbeidYtelseGrunnlagBuilder grunnlagBuilder;

        public MapFraDto(InntektArbeidYtelseGrunnlagBuilder builder) {
            this.grunnlagBuilder = builder;
        }

        private ArbeidsforholdOverstyringBuilder mapArbeidsforholdOverstyring(ArbeidsforholdOverstyringDto ov, ArbeidsforholdInformasjonBuilder builder) {
            var arbeidsgiverRef = mapArbeidsforholdRef(ov.getArbeidsforholdRef());
            var nyArbeidsgiverRef = mapArbeidsforholdRef(ov.getNyArbeidsforholdRef());
            var arbeidsgiver = mapArbeidsgiver(ov.getArbeidsgiver());

            var overstyringBuilder = builder.getOverstyringBuilderFor(arbeidsgiver, arbeidsgiverRef);

            overstyringBuilder.medBeskrivelse(ov.getBegrunnelse())
                .medNyArbeidsforholdRef(nyArbeidsgiverRef)
                .medHandling(KodeverkMapper.mapArbeidsforholdHandlingTypeFraDto(ov.getHandling()))
                .medAngittArbeidsgiverNavn(ov.getAngittArbeidsgiverNavn())
                .medAngittStillingsprosent(ov.getStillingsprosent() == null ? null : new Stillingsprosent(ov.getStillingsprosent()));

            ov.getBekreftetPermisjon().ifPresent(bp -> {
                var bekreftetPermisjon = new BekreftetPermisjon(bp.getPeriode().getFom(), bp.getPeriode().getTom(),
                    KodeverkMapper.getBekreftetPermisjonStatus(bp.getBekreftetPermisjonStatus()));
                overstyringBuilder.medBekreftetPermisjon(bekreftetPermisjon);
            });

            // overstyrte perioder
            ov.getArbeidsforholdOverstyrtePerioder().stream().forEach(p -> overstyringBuilder.leggTilOverstyrtPeriode(p.getFom(), p.getTom()));

            return overstyringBuilder;
        }

        private InternArbeidsforholdRef mapArbeidsforholdRef(ArbeidsforholdRefDto arbeidsforholdId) {
            if (arbeidsforholdId == null) {
                return InternArbeidsforholdRef.nullRef();
            }
            // FIXME : Abakus må lagre intern<->ekstern referanse
            return InternArbeidsforholdRef.ref(arbeidsforholdId.getAbakusReferanse());
        }

        private ArbeidsforholdReferanse mapArbeidsforholdReferanse(ArbeidsforholdReferanseDto r) {
            var internRef = InternArbeidsforholdRef.ref(r.getArbeidsforholdReferanse().getAbakusReferanse());
            var eksternRef = EksternArbeidsforholdRef.ref(r.getArbeidsforholdReferanse().getEksternReferanse());
            var arbeidsgiver = mapArbeidsgiver(r.getArbeidsgiver());
            if (!internRef.gjelderForSpesifiktArbeidsforhold() && !eksternRef.gjelderForSpesifiktArbeidsforhold()) {
                return null;
            }
            var ref = new ArbeidsforholdReferanse(arbeidsgiver, internRef, eksternRef);
            return ref;
        }

        private Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
            if (arbeidsgiver.getErOrganisasjon()) {
                return Arbeidsgiver.virksomhet(new OrgNummer(arbeidsgiver.getIdent()));
            }
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }

        ArbeidsforholdInformasjonBuilder map(ArbeidsforholdInformasjon dto) {
            var eksisterende = grunnlagBuilder.getArbeidsforholdInformasjon();
            var builder = ArbeidsforholdInformasjonBuilder.builder(eksisterende);
            if (dto != null) {
                dto.getOverstyringer().stream().map(ov -> mapArbeidsforholdOverstyring(ov, builder)).forEach(builder::leggTil);
                dto.getReferanser().stream().map(this::mapArbeidsforholdReferanse).forEach(r -> builder.leggTilNyReferanse(r));
            }
            return builder;
        }
    }

    static class MapTilDto {

        private static final Logger log = LoggerFactory.getLogger(MapTilDto.class);

        private List<Periode> map(List<ArbeidsforholdOverstyrtePerioder> perioder) {
            return perioder == null ? null
                : perioder.stream()
                .map(ArbeidsforholdOverstyrtePerioder::getOverstyrtePeriode)
                .map(this::mapPeriode)
                .sorted(COMP_PERIODE)
                .collect(Collectors.toList());
        }

        private Aktør mapAktør(Arbeidsgiver arbeidsgiver) {
            return arbeidsgiver.erAktørId()
                ? new AktørIdPersonident(arbeidsgiver.getAktørId().getId())
                : new Organisasjon(arbeidsgiver.getOrgnr());
        }

        private ArbeidsforholdReferanseDto mapArbeidsforholdReferanse(ArbeidsforholdReferanse ref) {
            var arbeidsgiver = mapAktør(ref.getArbeidsgiver());
            var internReferanse = ref.getInternReferanse() != null ? ref.getInternReferanse().getReferanse() : null;
            var eksternReferanse = ref.getEksternReferanse() != null ? ref.getEksternReferanse().getReferanse() : null;

            if (internReferanse != null && eksternReferanse != null) {
                return new ArbeidsforholdReferanseDto(arbeidsgiver, new ArbeidsforholdRefDto(internReferanse, eksternReferanse));
            } else if (internReferanse == null && eksternReferanse != null) {
                log.warn("Mangler internReferanse for eksternReferanse:" + eksternReferanse + ", arbeidsgiver=" + arbeidsgiver);
                return null;
            } else if (internReferanse == null && eksternReferanse == null) {
                return null;
            } else {
                if (internReferanse != null) {
                    throw new IllegalStateException("Mangler eksternReferanse for internReferanse:" + internReferanse + ", arbeidsgiver=" + arbeidsgiver);
                } else {
                    throw new IllegalStateException("Mangler internReferanse for eksternReferanse:" + eksternReferanse + ", arbeidsgiver=" + arbeidsgiver);
                }
            }
        }

        private ArbeidsforholdRefDto mapArbeidsforholdsId(no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjon arbeidsforholdInformasjon,
                                                          Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, UUID grunnlagRef, boolean aktiv) {
            /*
             * TODO: Fjern denne kommentaren når migrering fra fpsak er overstått.
             * Merk motsatt mapping av fpsak under migrering. FPSAK holder kun AAREGISTER referanse (og således vil sende samme referanse for
             * intern/ekstern (abakus/aaregister)
             * I Abakus er disse skilt. ArbeidsforholdRef holder intern abakus referanse (ikke AAREGISTER referanse som i FPSAK)
             */

            if (ref != null && ref.getReferanse() != null) {
                EksternArbeidsforholdRef eksternReferanse;
                try {
                    eksternReferanse = arbeidsforholdInformasjon == null ? null : arbeidsforholdInformasjon.finnEkstern(arbeidsgiver, ref);
                } catch (IllegalStateException e) {
                    if (e.getMessage().startsWith("Mangler eksternReferanse for internReferanse:")) {
                        // Sukk, må håndtere at det ligger dritt her også ..
                        eksternReferanse = null;
                    } else {
                        throw e;
                    }
                }

                if (eksternReferanse == null || eksternReferanse.getReferanse() == null) {
                    log.warn("Grunnlag=[ref='{}',aktiv={}] Mangler eksternReferanse for internReferanse={}", grunnlagRef, aktiv, ref);
                    return null;
                }
                return new ArbeidsforholdRefDto(ref.getReferanse(), eksternReferanse.getReferanse());
            }
            return null;
        }

        private no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.BekreftetPermisjon mapBekreftetPermisjon(Optional<BekreftetPermisjon> entitet) {
            if (entitet.isEmpty()) {
                return null;
            }
            var bekreftetPermisjon = entitet.get();
            if (bekreftetPermisjon.getPeriode() == null) {
                return null;
            }
            var periode = mapPeriode(bekreftetPermisjon.getPeriode());
            var bekreftetPermisjonStatus = KodeverkMapper.mapBekreftetPermisjonStatus(bekreftetPermisjon.getStatus());
            return new no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.BekreftetPermisjon(periode, bekreftetPermisjonStatus);
        }

        private Periode mapPeriode(DatoIntervallEntitet periode) {
            return new Periode(periode.getFomDato(), periode.getTomDato());
        }

        ArbeidsforholdInformasjon map(no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjon ai, UUID eksternReferanse, boolean aktiv) {
            if (ai == null)
                return null;

            var arbeidsforholdInformasjon = new ArbeidsforholdInformasjon();
            var overstyringer = ai.getOverstyringer().stream()
                .map(ao -> {
                    var dto = new ArbeidsforholdOverstyringDto(mapAktør(ao.getArbeidsgiver()),
                        mapArbeidsforholdsId(ai, ao.getArbeidsgiver(), ao.getArbeidsforholdRef(), eksternReferanse, aktiv))
                        .medBegrunnelse(ao.getBegrunnelse())
                        .medBekreftetPermisjon(mapBekreftetPermisjon(ao.getBekreftetPermisjon()))
                        .medHandling(KodeverkMapper.mapArbeidsforholdHandlingTypeTilDto(ao.getHandling()))
                        .medNavn(ao.getArbeidsgiverNavn())
                        .medStillingsprosent(ao.getStillingsprosent() == null ? null : ao.getStillingsprosent().getVerdi())
                        .medNyArbeidsforholdRef(
                            ao.getNyArbeidsforholdRef() == null ? null : mapArbeidsforholdsId(ai, ao.getArbeidsgiver(), ao.getNyArbeidsforholdRef(), eksternReferanse, aktiv))
                        .medArbeidsforholdOverstyrtePerioder(map(ao.getArbeidsforholdOverstyrtePerioder()));
                    return dto;
                })
                .sorted(COMP_ARBEIDSFORHOLD_OVERSTYRING)
                .collect(Collectors.toList());

            var referanser = ai.getArbeidsforholdReferanser().stream()
                .map(this::mapArbeidsforholdReferanse)
                .filter(it -> it != null)
                .sorted(COMP_ARBEIDSFORHOLD_REFERANSE)
                .collect(Collectors.toList());

            return arbeidsforholdInformasjon
                .medOverstyringer(overstyringer)
                .medReferanser(referanser);
        }

    }
}
