package no.nav.ung.sak.web.server.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import org.antlr.v4.runtime.misc.OrderedHashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class KodeverdiEnumLogFilterTest {
    final Logger log = (Logger) LoggerFactory.getLogger(KodeverdiEnumLogFilterTest.class);
    final LoggerContext loggerContext = log.getLoggerContext();
    final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    public void setup() {
        loggerContext.reset();
        final KodeverdiEnumLogFilter filter = new KodeverdiEnumLogFilter();
        filter.setContext(loggerContext);

        appender.addFilter(filter);
        appender.start();
        log.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        log.detachAndStopAllAppenders();
    }

    @Test
    public void testLoggKodeverdiEnumMedUlikNameOgKode() {
        log.warn("{}", VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("%s(%s)".formatted(VilkårType.BEREGNINGSGRUNNLAGVILKÅR.name(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR.getKode()));
    }

    @Test
    public void testLoggFlereKodeverdiEnumsMedUlikNameOgKode() {
        log.warn("VilkårType: {}, {}", VilkårType.BEREGNINGSGRUNNLAGVILKÅR, VilkårType.OPPTJENINGSVILKÅRET);
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("VilkårType: %s(%s), %s(%s)".formatted(VilkårType.BEREGNINGSGRUNNLAGVILKÅR.name(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR.getKode(), VilkårType.OPPTJENINGSVILKÅRET.name(), VilkårType.OPPTJENINGSVILKÅRET.getKode()));
    }

    @Test
    public void testLoggKodeverdiEnumMedSammeNameOgKode() {
        log.warn("DokumentStatus: {}", DokumentStatus.MOTTATT);
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("DokumentStatus: %s".formatted(DokumentStatus.MOTTATT.name()));
    }

    @Test
    public void testLoggUlikeKodeverdiEnums() {
        log.warn("VilkårType: {}, Avslagsårsak: {}, int: {}", VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, Avslagsårsak.MANGLENDE_DOKUMENTASJON, 123);
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("VilkårType: %s(%s), Avslagsårsak: %s(%s), int: 123".formatted(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.name(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.getKode(), Avslagsårsak.MANGLENDE_DOKUMENTASJON.name(), Avslagsårsak.MANGLENDE_DOKUMENTASJON.getKode()));
    }

    @Test
    void testIngenArgumenter() {
        log.error("hei");
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("hei");
    }


    @Test
    void testStacktrace() {
        try {
            throw new IllegalArgumentException("Boo");
        } catch (IllegalArgumentException e) {
            log.warn("Feilet med exception", e);
        }
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage()).isEqualTo("Feilet med exception");
        assertThat(appender.list.getFirst().getThrowableProxy().getMessage()).isEqualTo("Boo");
        assertThat(appender.list.getFirst().getThrowableProxy().getClassName()).isEqualTo(IllegalArgumentException.class.getName());
    }

    @Test
    public void testLoggingAvKodeverdiSet() {
        final Set<Kodeverdi> set = new OrderedHashSet<>();
        final var enum1 = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
        final var enum2 = VilkårType.MEDISINSKEVILKÅR_18_ÅR;
        final var enum3 = DokumentStatus.MOTTATT;
        set.add(enum1);
        set.add(enum2);
        set.add(enum3);
        log.warn("{}", set);
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("[%s(%s), %s(%s), %s]".formatted(enum1.name(), enum1.getKode(), enum2.name(), enum2.getKode(), enum3.name()));
    }

    @Test
    public void testLoggingAvKodeverdiMap() {
        final Map<Kodeverdi, Kodeverdi> map = new HashMap<>();
        final var enum1 = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
        final var enum2 = VilkårType.MEDISINSKEVILKÅR_18_ÅR;
        final var enum3 = DokumentStatus.MOTTATT;
        final var landkode = Landkoder.SWE; // <- kodeverdi som ikkje er enum
        map.put(enum1, enum2);
        map.put(enum2, enum3);
        map.put(enum3, landkode);
        log.info("{}", map);
        assertThat(appender.list).size().isEqualTo(1);
        final var event = appender.list.getFirst();
        final var extractedString = assertThat(event)
            .extracting(ILoggingEvent::getFormattedMessage)
            .asString();
        extractedString.contains("%s(%s)=%s(%s)".formatted(enum1.name(), enum1.getKode(), enum2.name(), enum2.getKode()));
        extractedString.contains("%s(%s)=%s".formatted(enum2.name(), enum2.getKode(), enum3.getKode()));
        extractedString.contains("%s=%s".formatted(enum3.name(), landkode));
    }

    @Test
    public void testLoggingAvAndreTing() {
        final Set<Integer> nums = new OrderedHashSet<Integer>();
        nums.add(1);
        nums.add(2);
        nums.add(3);
        log.info("{}", nums);
        assertThat(appender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("[1, 2, 3]");
    }
}
