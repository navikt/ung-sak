package no.nav.ung.sak.domene.abakus;

import java.lang.StackWalker.StackFrame;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.ung.sak.domene.arbeidsforhold.IAYDiffsjekker;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.iay.modell.VersjonType;

/**
 * In-memory - legger kun grunnlag i minne (lagrer ikke i noe lager)
 * NB: Skal kun brukes for tester.
 * <p>
 * Definer som alternative i beans.xml (i src/test/resources/META-INF) i modul som skal bruke
 * <p>
 * <p>
 * Legg inn i fil <code>src/test/resources/META-INF</code> for å aktivere for enhetstester:
 * <p>
 * <code>
 * &lt;alternatives&gt;<br>
 * &lt;class&gt;no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste&lt;/class&gt;<br>
 * &lt;/alternatives&gt;<br>
 * </code>
 */
@RequestScoped
@Alternative
public class AbakusInMemoryInntektArbeidYtelseTjeneste implements InntektArbeidYtelseTjeneste {

    private Map<Long, Deque<UUID>> indeksBehandlingTilGrunnlag = new LinkedHashMap<>();
    private List<InntektArbeidYtelseGrunnlag> grunnlag = new ArrayList<>();

    /**
     * CDI ctor for proxies.
     */
    public AbakusInMemoryInntektArbeidYtelseTjeneste() {
    }

    private static String getCallerMethod() {
        List<StackFrame> frames = StackWalker.getInstance().walk(s -> s.limit(2).collect(Collectors.toList()));
        return frames.get(1).getMethodName();
    }

    @Override
    public Optional<InntektArbeidYtelseGrunnlag> finnGrunnlag(Long behandlingId) {
        return getAktivtInntektArbeidGrunnlag(behandlingId);
    }


    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId) {
        return getAktivtInntektArbeidGrunnlag(behandlingId).orElseThrow();
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlagForGrunnlagId(Long behandlingId, UUID inntektArbeidYtelseGrunnlagId) {
        return grunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), inntektArbeidYtelseGrunnlagId))
            .findFirst().orElseThrow();
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId) {
        Optional<InntektArbeidYtelseGrunnlag> origAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(fraBehandlingId);
        origAggregat.ifPresent(orig -> {
            InntektArbeidYtelseGrunnlag entitet = new InntektArbeidYtelseGrunnlag(orig);
            lagreOgFlush(tilBehandlingId, entitet);
        });
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId, Set<Dataset> dataset) {
        Optional<InntektArbeidYtelseGrunnlag> origAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(fraBehandlingId);
        origAggregat.ifPresent(orig -> {
            var builder = InntektArbeidYtelseGrunnlagBuilder.nytt();
            for (Dataset data : dataset) {
                switch (data) {
                    case REGISTER:
                        builder.medRegister(InntektArbeidYtelseAggregatBuilder.oppdatere(orig.getRegisterVersjon(), VersjonType.REGISTER));
                        break;
                    case OPPGITT_OPPTJENING:
                        builder.medOppgittOpptjening(OppgittOpptjeningBuilder.nyFraEksisterende(orig.getOppgittOpptjening().orElse(null), UUID.randomUUID(), LocalDateTime.now()));
                        break;
                    case OPPGITT_OPPTJENING_V2:
                        builder.medOppgitteOpptjeninger(orig.getOppgittOpptjeningAggregat().map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger).orElse(List.of()));
                        break;
                    case OVERSTYRT_OPPGITT_OPPTJENING:
                        builder.medOverstyrtOppgittOpptjening(OppgittOpptjeningBuilder.nyFraEksisterende(orig.getOverstyrtOppgittOpptjening().orElse(null), UUID.randomUUID(), LocalDateTime.now()));
                        break;
                    default:
                        throw new UnsupportedOperationException("Har ikke implementert støtte for Dataset:" + data);
                }
            }
            lagreOgFlush(tilBehandlingId, builder.build());
        });
    }

    @Override
    public void lagreIayAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
        var grunnlagBuilder = getGrunnlagBuilder(behandlingId, builder);
        lagreOgFlush(behandlingId, grunnlagBuilder.build());
    }

    private InMemoryInntektArbeidYtelseGrunnlagBuilder getGrunnlagBuilder(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
        Objects.requireNonNull(builder, "inntektArbeidYtelserBuilder"); // NOSONAR
        var opptjeningAggregatBuilder = opprettGrunnlagBuilderFor(behandlingId);
        opptjeningAggregatBuilder.medData(builder);
        return opptjeningAggregatBuilder;
    }

    private InMemoryInntektArbeidYtelseGrunnlagBuilder opprettGrunnlagBuilderFor(Long behandlingId) {
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);
        return InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidAggregat);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(Long behandlingId) {
        var iayGrunnlag = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);
        return opprettBuilderFor(VersjonType.REGISTER, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    private InntektArbeidYtelseAggregatBuilder opprettBuilderFor(VersjonType versjonType, UUID angittReferanse, LocalDateTime opprettetTidspunkt,
                                                                 Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        var grunnlagBuilder = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(grunnlag);
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder");
        Optional<InntektArbeidYtelseGrunnlag> aggregat = Optional.ofNullable(grunnlagBuilder.getKladd()); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(aggregat, "aggregat"); // NOSONAR $NON-NLS-1$
        if (aggregat.isPresent()) {
            final InntektArbeidYtelseGrunnlag aggregat1 = aggregat.get();
            return InntektArbeidYtelseAggregatBuilder.builderFor(hentRiktigVersjon(versjonType, aggregat1), angittReferanse, opprettetTidspunkt, versjonType);
        }
        throw new IllegalArgumentException("aggregat kan ikke være null: " + angittReferanse);
    }

    private Optional<InntektArbeidYtelseAggregat> hentRiktigVersjon(VersjonType versjonType, InntektArbeidYtelseGrunnlag aggregat) {
        if (versjonType == VersjonType.REGISTER) {
            return aggregat.getRegisterVersjon();
        }
        throw new IllegalStateException("Kunne ikke finne riktig versjon av InntektArbeidYtelseGrunnlag");
    }


    @Override
    /** @deprecated (brukes kun i test) */
    public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjening) {
        if (oppgittOpptjening == null) {
            return;
        }
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);

        var iayGrunnlag = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidAggregat);
        iayGrunnlag.medOppgittOpptjeningAggregat(List.of(oppgittOpptjening));

        lagreOgFlush(behandlingId, iayGrunnlag.build());
    }

    @Override
    public void lagreOverstyrtOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjening) {
        if (oppgittOpptjening == null) {
            return;
        }
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);

        var iayGrunnlag = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidAggregat);
        iayGrunnlag.medOppgittOpptjening(oppgittOpptjening);

        lagreOgFlush(behandlingId, iayGrunnlag.build());
    }


    @Override
    public Optional<OppgittOpptjening> hentKunOverstyrtOppgittOpptjening(Long behandlingId) {
        return finnGrunnlag(behandlingId).flatMap(InntektArbeidYtelseGrunnlag::getOverstyrtOppgittOpptjening);
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(UUID behandlingUUid) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED (mangler kobling til behandlingUUid): #" + getCallerMethod());
    }

    private Optional<InntektArbeidYtelseGrunnlag> getAktivtInntektArbeidGrunnlag(@SuppressWarnings("unused") UUID behandlingId) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED (mangler kobling til behandlingUUid): #" + getCallerMethod());
    }

    private Optional<InntektArbeidYtelseGrunnlag> getAktivtInntektArbeidGrunnlag(Long behandlingId) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            return Optional.empty();
        }
        return behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(behandlingId, grId))
            .filter(gr -> gr.isAktiv())
            .findFirst();
    }


    private Optional<InntektArbeidYtelseGrunnlag> hentInntektArbeidYtelseGrunnlagForBehandling(Long behandlingId) {
        return getAktivtInntektArbeidGrunnlag(behandlingId);
    }

    private void lagreGrunnlag(InntektArbeidYtelseGrunnlag nyttGrunnlag, Long behandlingId) {
        var entitet = nyttGrunnlag;

        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());

        setField(entitet, "behandlingId", behandlingId);

        behGrunnlag.push(entitet.getEksternReferanse());
        grunnlag.add(entitet);
    }

    private void lagreOgFlush(Long behandlingId, InntektArbeidYtelseGrunnlag nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        if (nyttGrunnlag == null) {
            return;
        }
        Optional<InntektArbeidYtelseGrunnlag> tidligereAggregat = getAktivtInntektArbeidGrunnlag(behandlingId);
        if (tidligereAggregat.isPresent()) {
            InntektArbeidYtelseGrunnlag entitet = tidligereAggregat.get();
            if (new IAYDiffsjekker(false).getDiffEntity().diff(entitet, nyttGrunnlag).isEmpty()) {
                return;
            }
            setField(entitet, "aktiv", false);
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        } else {
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        }
    }

    private void setField(Object entitet, String field, Object val) {
        try {
            var fld = entitet.getClass().getDeclaredField(field);
            fld.setAccessible(true);
            fld.set(entitet, val);
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new UnsupportedOperationException("Kan ikke sette felt: " + field + " på entitet: " + entitet.getClass() + " til " + val, e);
        }
    }

    /**
     * lagd for å eksponere builder metoder lokalt til denne tjeneste implementasjonen.
     */
    private static class InMemoryInntektArbeidYtelseGrunnlagBuilder extends InntektArbeidYtelseGrunnlagBuilder {
        private InMemoryInntektArbeidYtelseGrunnlagBuilder(InntektArbeidYtelseGrunnlag kladd) {
            super(kladd);
        }

        public static InMemoryInntektArbeidYtelseGrunnlagBuilder nytt() {
            return ny(UUID.randomUUID(), LocalDateTime.now());
        }

        /**
         * Opprett ny versjon av grunnlag med angitt assignet grunnlagReferanse og opprettetTidspunkt.
         */
        public static InMemoryInntektArbeidYtelseGrunnlagBuilder ny(UUID grunnlagReferanse, LocalDateTime opprettetTidspunkt) {
            return new InMemoryInntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(grunnlagReferanse, opprettetTidspunkt));
        }

        public static InMemoryInntektArbeidYtelseGrunnlagBuilder oppdatere(InntektArbeidYtelseGrunnlag kladd) {
            return new InMemoryInntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(kladd));
        }

        public static InMemoryInntektArbeidYtelseGrunnlagBuilder oppdatere(Optional<InntektArbeidYtelseGrunnlag> kladd) {
            return kladd.map(InMemoryInntektArbeidYtelseGrunnlagBuilder::oppdatere).orElseGet(InMemoryInntektArbeidYtelseGrunnlagBuilder::nytt);
        }

        @Override
        public InntektArbeidYtelseGrunnlag getKladd() {
            return super.getKladd();
        }

    }
}
